package com.opencode.sereno.audio

import android.speech.tts.Voice

fun Voice.isOfflinePtBr(): Boolean =
    locale.language.equals("pt", ignoreCase = true) &&
        locale.country.equals("BR", ignoreCase = true) &&
        !isNetworkConnectionRequired

fun Collection<Voice>.offlinePtBr(): List<Voice> = filter { it.isOfflinePtBr() }
