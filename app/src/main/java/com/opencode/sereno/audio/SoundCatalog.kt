package com.opencode.sereno.audio

enum class SoundCategory(val key: String, val label: String) {
    NATURE("nature", "Natureza"),
    AMBIENCE("ambience", "Ambientes"),
    NOISE("noise", "Ruído"),
    BINAURAL("binaural", "Binaural"),
    FREQUENCY("frequency", "Frequências"),
    MUSIC("music", "Música"),
    FAVORITES("favorites", "Favoritos"),
    MIXES("mixes", "Meus mixes")
}

data class SoundDef(
    val id: String,
    val name: String,
    val category: SoundCategory,
    val subtitle: String,
    val headphone: Boolean = false,
    val make: () -> Voice
)

data class QuickMix(
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val soundIds: List<String>
)

object SoundCatalog {

    val sounds: List<SoundDef> = listOf(
        SoundDef("rain_soft", "Chuva suave", SoundCategory.NATURE, "garoa fina e contínua") {
            SampleBank.voice("rain_soft") ?: RainVoice(0.42f, 6800f, 4f, roof = false, leaves = true, seed = 101)
        },
        SoundDef("rain_heavy", "Chuva forte", SoundCategory.NATURE, "peso e tambores no teto") {
            SampleBank.voice("rain_heavy") ?: RainVoice(1.0f, 3000f, 38f, roof = true, leaves = false, seed = 102)
        },
        SoundDef("ocean", "Ondas do oceano", SoundCategory.NATURE, "mar em movimento") {
            SampleBank.voice("ocean") ?: OceanVoice(0.07f, 0.13f, slow = false, seed = 110)
        },
        SoundDef("ocean_slow", "Ondas lentas", SoundCategory.NATURE, "a respiração do mar") {
            SampleBank.voice("ocean_slow") ?: OceanVoice(0.045f, 0.085f, slow = true, seed = 111)
        },
        SoundDef("river", "Correnteza do rio", SoundCategory.NATURE, "água em curso") {
            SampleBank.voice("river") ?: RiverVoice(0.65f, seed = 120)
        },
        SoundDef("brook", "Riacho", SoundCategory.NATURE, "água entre pedras") {
            SampleBank.voice("brook") ?: RiverVoice(0.5f, seed = 121)
        },
        SoundDef("waterfall", "Cachoeira", SoundCategory.NATURE, "queda contínua") {
            SampleBank.voice("waterfall") ?: WaterfallVoice(seed = 130)
        },
        SoundDef("lake", "Lago", SoundCategory.NATURE, "margem calma") {
            SampleBank.voice("lake") ?: RiverVoice(0.3f, seed = 131)
        },
        SoundDef("underwater", "Submerso", SoundCategory.NATURE, "sons do fundo") {
            SampleBank.voice("underwater") ?: NoiseVoice(NoiseColor.BROWN, seed = 132)
        },
        SoundDef("wind", "Vento", SoundCategory.NATURE, "rajadas e assobio") {
            SampleBank.voice("wind") ?: WindVoice(seed = 140)
        },
        SoundDef("forest", "Floresta", SoundCategory.NATURE, "folhas e vida ao redor") {
            SampleBank.voice("forest") ?: WindVoice(seed = 141)
        },
        SoundDef("birdsong", "Pássaros", SoundCategory.NATURE, "canto ao amanhecer") {
            SampleBank.voice("birdsong") ?: NoiseVoice(NoiseColor.GREEN, seed = 142)
        },
        SoundDef("crickets", "Grilos", SoundCategory.NATURE, "noite tranquila") {
            SampleBank.voice("crickets") ?: NoiseVoice(NoiseColor.GREEN, seed = 143)
        },
        SoundDef("thunder", "Trovoada distante", SoundCategory.NATURE, "trovões ao longe") {
            SampleBank.voice("thunder") ?: NoiseVoice(NoiseColor.BROWN, seed = 144)
        },
        SoundDef("snowfall", "Neve", SoundCategory.NATURE, "silêncio abafado") {
            SampleBank.voice("snowfall") ?: NoiseVoice(NoiseColor.WHITE, seed = 145)
        },
        SoundDef("seagulls", "Gaivotas", SoundCategory.NATURE, "costa aberta") {
            SampleBank.voice("seagulls") ?: WindVoice(seed = 146)
        },
        SoundDef("beach", "Praia tropical", SoundCategory.NATURE, "mar e brisa") {
            SampleBank.voice("beach") ?: OceanVoice(0.06f, 0.11f, slow = false, seed = 147)
        },
        SoundDef("fireplace", "Lareira", SoundCategory.NATURE, "estalos e calor") {
            SampleBank.voice("fireplace") ?: FireVoice(7f, seed = 150)
        },
        SoundDef("campfire", "Fogueira", SoundCategory.NATURE, "fogo aberto") {
            SampleBank.voice("campfire") ?: FireVoice(13f, seed = 151)
        },
        SoundDef("rain_tent", "Chuva na barraca", SoundCategory.NATURE, "gotas macias no tecido") {
            SampleBank.voice("rain_tent") ?: RainVoice(0.55f, 4800f, 12f, roof = true, leaves = false, seed = 152)
        },

        SoundDef("rainy_cafe", "Café chuvoso", SoundCategory.AMBIENCE, "conversas e xícaras ao fundo") {
            SampleBank.voice("rainy_cafe") ?: NoiseVoice(NoiseColor.BROWN, seed = 231)
        },
        SoundDef("library", "Biblioteca", SoundCategory.AMBIENCE, "silêncio de páginas e passos") {
            SampleBank.voice("library") ?: NoiseVoice(NoiseColor.PINK, seed = 232)
        },
        SoundDef("train_ride", "Viagem de trem", SoundCategory.AMBIENCE, "trilhos constantes") {
            SampleBank.voice("train_ride") ?: NoiseVoice(NoiseColor.BROWN, seed = 233)
        },
        SoundDef("cozy_room", "Quarto aconchegante", SoundCategory.AMBIENCE, "calor e presença baixa") {
            SampleBank.voice("cozy_room") ?: NoiseVoice(NoiseColor.PINK, seed = 234)
        },
        SoundDef("fan_hum", "Ventilador", SoundCategory.AMBIENCE, "zumbido uniforme") {
            SampleBank.voice("fan_hum") ?: NoiseVoice(NoiseColor.BROWN, seed = 235)
        },
        SoundDef("dawn_chorus", "Amanhecer", SoundCategory.AMBIENCE, "pássaros distantes") {
            SampleBank.voice("dawn_chorus") ?: WindVoice(seed = 236)
        },

        SoundDef("white", "Ruído branco", SoundCategory.NOISE, "espectro plano") {
            NoiseVoice(NoiseColor.WHITE, seed = 201)
        },
        SoundDef("pink", "Ruído rosa", SoundCategory.NOISE, "suave e equilibrado") {
            NoiseVoice(NoiseColor.PINK, seed = 202)
        },
        SoundDef("brown", "Ruído marrom", SoundCategory.NOISE, "grave e profundo") {
            NoiseVoice(NoiseColor.BROWN, seed = 203)
        },
        SoundDef("green", "Ruído verde", SoundCategory.NOISE, "centrado no meio") {
            NoiseVoice(NoiseColor.GREEN, seed = 204)
        },
        SoundDef("blue", "Ruído azul", SoundCategory.NOISE, "brilhante e nítido") {
            NoiseVoice(NoiseColor.BLUE, seed = 205)
        },
        SoundDef("gray", "Ruído cinza", SoundCategory.NOISE, "loudness equilibrado") {
            NoiseVoice(NoiseColor.GRAY, seed = 206)
        },

        SoundDef("delta", "Ondas delta", SoundCategory.BINAURAL, "2,5 Hz · sono profundo", true) {
            BinauralVoice(200f, 2.5f, seed = 301)
        },
        SoundDef("theta", "Ondas teta", SoundCategory.BINAURAL, "6 Hz · relaxamento", true) {
            BinauralVoice(200f, 6f, seed = 302)
        },
        SoundDef("alpha", "Ondas alfa", SoundCategory.BINAURAL, "10 Hz · calma alerta", true) {
            BinauralVoice(200f, 10f, seed = 303)
        },
        SoundDef("beta", "Ondas beta", SoundCategory.BINAURAL, "18 Hz · foco", true) {
            BinauralVoice(200f, 18f, seed = 304)
        },

        SoundDef("f174", "174 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(174f) },
        SoundDef("f285", "285 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(285f) },
        SoundDef("f396", "396 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(396f) },
        SoundDef("f417", "417 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(417f) },
        SoundDef("f432", "432 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(432f) },
        SoundDef("f528", "528 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(528f) },
        SoundDef("f639", "639 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(639f) },
        SoundDef("f741", "741 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(741f) },
        SoundDef("f852", "852 Hz", SoundCategory.FREQUENCY, "tom puro") { SolfeggioVoice(852f) },

        SoundDef("bowls", "Taças tibetanas", SoundCategory.MUSIC, "parciais ressonantes") {
            BowlVoice(
                baseFreq = 130.81f,
                ratios = floatArrayOf(1f, 2.76f, 5.4f, 8.9f),
                amps = floatArrayOf(1f, 0.5f, 0.26f, 0.12f),
                taus = floatArrayOf(10f, 6f, 3.4f, 2.2f),
                minInterval = 9f,
                maxInterval = 16f,
                strikeGain = 0.2f,
                seed = 401
            )
        },
        SoundDef("gong", "Gongo", SoundCategory.MUSIC, "ressonância longa") {
            BowlVoice(
                baseFreq = 65.41f,
                ratios = floatArrayOf(1f, 1.48f, 2.1f, 3.4f, 4.7f),
                amps = floatArrayOf(1f, 0.55f, 0.35f, 0.2f, 0.12f),
                taus = floatArrayOf(18f, 14f, 10f, 7f, 5f),
                minInterval = 22f,
                maxInterval = 36f,
                strikeGain = 0.22f,
                seed = 402
            )
        },
        SoundDef("bell", "Sino de templo", SoundCategory.MUSIC, "toques espaçados") {
            SampleBank.bellVoice("temple_bell", 8f) ?: BellVoice(
                baseFreq = 261.63f,
                minInterval = 10f,
                maxInterval = 18f,
                strikeGain = 0.16f,
                seed = 403
            )
        },
        SoundDef("piano_soft", "Piano suave", SoundCategory.MUSIC, "notas lentas e limpas") {
            SampleBank.voice("soft_piano") ?: PluckVoice(
                notes = floatArrayOf(261.63f, 293.66f, 329.63f, 392f, 440f, 523.25f),
                minInterval = 1.1f,
                maxInterval = 2.4f,
                damping = 0.9975f,
                amplitude = 0.15f,
                tauSeconds = 4.5f,
                seed = 408
            )
        },
        SoundDef("guitar_calm", "Violão calmo", SoundCategory.MUSIC, "dedilhado macio") {
            SampleBank.voice("calm_guitar") ?: PluckVoice(
                notes = floatArrayOf(196f, 220f, 246.94f, 293.66f, 329.63f, 392f),
                minInterval = 1.3f,
                maxInterval = 2.7f,
                damping = 0.9965f,
                amplitude = 0.16f,
                tauSeconds = 5f,
                seed = 409
            )
        },
        SoundDef("crystal_bowls", "Taças de cristal", SoundCategory.MUSIC, "brilho suspenso") {
            SampleBank.voice("crystal_bowls") ?: BowlVoice(
                baseFreq = 261.63f,
                ratios = floatArrayOf(1f, 2.1f, 3.4f, 5.2f),
                amps = floatArrayOf(1f, 0.5f, 0.25f, 0.12f),
                taus = floatArrayOf(9f, 6f, 4f, 2.6f),
                minInterval = 11f,
                maxInterval = 19f,
                strikeGain = 0.18f,
                seed = 410
            )
        },
        SoundDef("hang_drum", "Hang drum", SoundCategory.MUSIC, "metal suave e circular") {
            SampleBank.voice("hang_drum") ?: BowlVoice(
                baseFreq = 98f,
                ratios = floatArrayOf(1f, 2.4f, 3.1f, 4.6f),
                amps = floatArrayOf(1f, 0.5f, 0.28f, 0.14f),
                taus = floatArrayOf(8f, 5f, 3.5f, 2.2f),
                minInterval = 6f,
                maxInterval = 11f,
                strikeGain = 0.2f,
                seed = 411
            )
        },
        SoundDef("kalimba", "Kalimba", SoundCategory.MUSIC, "cordas dedilhadas") {
            PluckVoice(
                notes = floatArrayOf(523.25f, 587.33f, 659.25f, 783.99f, 880f, 1046.5f),
                minInterval = 0.9f,
                maxInterval = 2.1f,
                damping = 0.9955f,
                amplitude = 0.2f,
                tauSeconds = 3f,
                seed = 404
            )
        },
        SoundDef("harp", "Harpa", SoundCategory.MUSIC, "arpejos lentos") {
            PluckVoice(
                notes = floatArrayOf(196f, 220f, 261.63f, 293.66f, 329.63f, 392f, 440f),
                minInterval = 1.4f,
                maxInterval = 2.8f,
                damping = 0.998f,
                amplitude = 0.16f,
                tauSeconds = 6f,
                seed = 405
            )
        },
        SoundDef("pad_serene", "Serenidade", SoundCategory.MUSIC, "pad em lá menor") {
            PadVoice(floatArrayOf(110f, 164.81f, 220f, 261.63f, 329.63f), seed = 406)
        },
        SoundDef("pad_night", "Noturno", SoundCategory.MUSIC, "pad em ré menor") {
            PadVoice(floatArrayOf(73.42f, 146.83f, 174.61f, 220f, 293.66f), seed = 407)
        }
    )

    private val byId: Map<String, SoundDef> = sounds.associateBy { it.id }

    fun find(id: String): SoundDef? = byId[id]

    fun name(id: String): String = byId[id]?.name ?: id

    fun voiceFor(id: String): Voice? = byId[id]?.let {
        runCatching { it.make() }.getOrNull()
    }

    fun soundsOf(category: SoundCategory): List<SoundDef> = sounds.filter { it.category == category }

    val quickMixes = listOf(
        QuickMix(
            title = "Dormir",
            subtitle = "chuva + delta + noturno",
            minutes = 30,
            soundIds = listOf("rain_soft", "delta", "pad_night")
        ),
        QuickMix(
            title = "Foco",
            subtitle = "marrom + alfa + serenidade",
            minutes = 15,
            soundIds = listOf("brown", "alpha", "pad_serene")
        ),
        QuickMix(
            title = "Relaxar",
            subtitle = "ondas + 528 Hz + taças",
            minutes = 15,
            soundIds = listOf("ocean", "f528", "bowls")
        )
    )
}
