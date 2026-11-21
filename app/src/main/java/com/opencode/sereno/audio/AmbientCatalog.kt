package com.opencode.sereno.audio

data class AmbientOption(
    val id: String,
    val label: String,
    val level: Float
)

val AmbientOptions = listOf(
    AmbientOption("none", "Sem som", 0f),
    AmbientOption("rain_soft", "Chuva suave", 0.62f),
    AmbientOption("rain_heavy", "Chuva forte", 0.50f),
    AmbientOption("ocean_slow", "Ondas", 0.60f),
    AmbientOption("pad_serene", "Serenidade", 0.92f),
    AmbientOption("fireplace", "Lareira", 0.95f)
)
