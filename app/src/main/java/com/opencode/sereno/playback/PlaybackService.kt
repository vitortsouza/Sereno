package com.opencode.sereno.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.opencode.sereno.MainActivity
import com.opencode.sereno.R
import com.opencode.sereno.audio.SoundCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var manager: NotificationManager
    private lateinit var mediaSession: MediaSession
    private var lastKey: String? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        createChannel()
        createMediaSession()
        val notification = buildNotification(Player.state.value)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        scope.launch {
            Player.state.collect { state ->
                if (state.layers.isEmpty()) {
                    stopSelf()
                } else {
                    updateMediaSession(state)
                    val key = notificationKey(state)
                    if (key != lastKey) {
                        lastKey = key
                        runCatching {
                            NotificationManagerCompat.from(this@PlaybackService)
                                .notify(NOTIFICATION_ID, buildNotification(state))
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> Player.togglePlayback()
            ACTION_STOP -> {
                Player.stopPlayback()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createMediaSession() {
        mediaSession = MediaSession(this, "Sereno")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                Player.resumePlayback()
            }

            override fun onPause() {
                Player.pause()
            }

            override fun onStop() {
                Player.stopPlayback()
                stopSelf()
            }
        })
        mediaSession.setSessionActivity(contentIntent())
        mediaSession.isActive = true
        updateMediaSession(Player.state.value)
    }

    private fun updateMediaSession(state: PlayerState) {
        val names = state.layers.map { SoundCatalog.name(it.soundId) }
        val title = when {
            names.isEmpty() -> "Sereno"
            names.size == 1 -> names[0]
            names.size == 2 -> "${names[0]} + ${names[1]}"
            else -> "${names[0]} + ${names[1]} +1"
        }
        val position = if (state.totalMs > 0) {
            (state.totalMs - state.remainingMs).coerceIn(0L, state.totalMs)
        } else {
            0L
        }
        val playbackState = when {
            state.isPlaying -> PlaybackState.STATE_PLAYING
            state.isPaused -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_NONE
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP
                )
                .setState(playbackState, position, 1f)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Sereno")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, state.totalMs)
                .build()
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reprodução",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Sons e misturas em andamento"
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(state: PlayerState): Notification {
        val names = state.layers.map { SoundCatalog.name(it.soundId) }
        val title = when {
            names.isEmpty() -> "Sereno"
            names.size == 1 -> names[0]
            names.size == 2 -> "${names[0]} + ${names[1]}"
            else -> "${names[0]} + ${names[1]} +1"
        }
        val timer = when {
            state.totalMs <= 0 -> if (state.isPlaying) "sem limite" else "pausado · sem limite"
            state.isPlaying -> "em reprodução"
            else -> "pausado · ${formatTime(state.remainingMs)}"
        }
        val text = if (state.layers.isEmpty()) {
            "Pronto para começar"
        } else {
            "$timer · ${state.layers.size} camada${if (state.layers.size > 1) "s" else ""}"
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sereno)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("Sereno")
            .setContentIntent(contentIntent())
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setShowWhen(state.isPlaying && state.totalMs > 0)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(
                Notification.Action.Builder(
                    if (state.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                    if (state.isPlaying) "Pausar" else "Retomar",
                    serviceIntent(ACTION_TOGGLE, 1)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Parar",
                    serviceIntent(ACTION_STOP, 2)
                ).build()
            )

        if (state.isPlaying && state.totalMs > 0 && state.remainingMs > 0) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.remainingMs)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun notificationKey(state: PlayerState): String {
        val seconds = if (state.isPlaying) -1L else state.remainingMs / 1000
        val layers = state.layers.joinToString(",") { it.soundId }
        return "${state.isPlaying}|${state.totalMs}|$seconds|$layers"
    }

    private fun contentIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    companion object {
        const val CHANNEL_ID = "sereno_playback"
        const val NOTIFICATION_ID = 1401
        const val ACTION_START = "com.opencode.sereno.action.START"
        const val ACTION_TOGGLE = "com.opencode.sereno.action.TOGGLE"
        const val ACTION_STOP = "com.opencode.sereno.action.STOP"
    }
}
