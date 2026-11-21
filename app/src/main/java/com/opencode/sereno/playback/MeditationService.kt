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
import com.opencode.sereno.audio.meditations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MeditationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var manager: NotificationManager
    private lateinit var mediaSession: MediaSession
    private var lastKey: String? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        createChannel()
        createMediaSession()
        val notification = buildNotification(MeditationController.state.value)
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
            MeditationController.state.collect { state ->
                if (state.sessionId.isBlank() ||
                    (!state.running && state.elapsedMs <= 0L && !state.finished)
                ) {
                    stopSelf()
                    return@collect
                }
                updateMediaSession(state)
                val key = "${state.running}|${state.finished}|${state.stepIndex}|${state.elapsedMs / 1000}"
                if (key != lastKey) {
                    lastKey = key
                    runCatching {
                        NotificationManagerCompat.from(this@MeditationService)
                            .notify(NOTIFICATION_ID, buildNotification(state))
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> MeditationController.toggle()
            ACTION_STOP -> {
                MeditationController.stop()
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
        mediaSession = MediaSession(this, "Sereno Meditação")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = MeditationController.toggle()
            override fun onPause() = MeditationController.pause()
            override fun onStop() {
                MeditationController.stop()
                stopSelf()
            }
        })
        mediaSession.setSessionActivity(contentIntent())
        mediaSession.isActive = true
    }

    private fun updateMediaSession(state: MeditationState) {
        val stepTitle = meditations.firstOrNull { it.id == state.sessionId }
            ?.steps?.getOrNull(state.stepIndex)?.title
            .orEmpty()
        val playbackState = when {
            state.running -> PlaybackState.STATE_PLAYING
            state.finished -> PlaybackState.STATE_NONE
            else -> PlaybackState.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP
                )
                .setState(playbackState, state.elapsedMs, 1f)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, state.sessionName)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, stepTitle.ifBlank { "Sereno" })
                .putLong(MediaMetadata.METADATA_KEY_DURATION, state.totalMs)
                .build()
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Práticas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Meditações em andamento"
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(state: MeditationState): Notification {
        val stepTitle = meditations.firstOrNull { it.id == state.sessionId }
            ?.steps?.getOrNull(state.stepIndex)?.title
            .orEmpty()
        val text = when {
            state.finished -> "Sessão concluída"
            state.running -> stepTitle.ifBlank { "Meditação em andamento" }
            else -> "Pausada · ${formatTime(state.elapsedMs)}"
        }
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sereno)
            .setContentTitle(state.sessionName.ifBlank { "Sereno" })
            .setContentText(text)
            .setSubText("Sereno")
            .setContentIntent(contentIntent())
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(state.running)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(
                Notification.Action.Builder(
                    if (state.running) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                    if (state.running) "Pausar" else "Retomar",
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
        if (state.running && state.totalMs > 0L) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + (state.totalMs - state.elapsedMs))
        }
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        10,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MeditationService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "sereno_practice"
        const val NOTIFICATION_ID = 1402
        const val ACTION_START = "com.opencode.sereno.action.PRACTICE_START"
        const val ACTION_TOGGLE = "com.opencode.sereno.action.PRACTICE_TOGGLE"
        const val ACTION_STOP = "com.opencode.sereno.action.PRACTICE_STOP"
    }
}
