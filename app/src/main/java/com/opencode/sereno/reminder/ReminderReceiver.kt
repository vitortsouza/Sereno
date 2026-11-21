package com.opencode.sereno.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.opencode.sereno.MainActivity
import com.opencode.sereno.R
import com.opencode.sereno.data.Prefs

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.journalReminder.value) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Lembretes gentis",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Um convite para escrever no diário"
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            context,
            7302,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sereno)
            .setContentTitle("Um instante para você")
            .setContentText("Que tal registrar um pequeno momento do seu dia?")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        ReminderScheduler.schedule(context)
    }

    companion object {
        private const val CHANNEL_ID = "sereno_reminders"
        private const val NOTIFICATION_ID = 7401
    }
}
