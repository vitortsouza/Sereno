package com.opencode.sereno

import android.app.Application
import com.opencode.sereno.audio.SampleBank
import com.opencode.sereno.data.JournalStore
import com.opencode.sereno.data.Prefs
import com.opencode.sereno.playback.MeditationController
import com.opencode.sereno.playback.Player
import com.opencode.sereno.reminder.ReminderScheduler

class SerenoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SampleBank.init(this)
        Player.init(this)
        MeditationController.init(this)
        JournalStore.init(this)
        if (Prefs.journalReminder.value) ReminderScheduler.schedule(this)
    }
}
