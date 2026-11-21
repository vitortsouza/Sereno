package com.opencode.sereno.audio

data class MeditationStep(
    val title: String,
    val text: String,
    val seconds: Int
) {
    val durationMs: Long get() = seconds * 1000L
}

data class MeditationSession(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val steps: List<MeditationStep>
) {
    val totalMs: Long get() = steps.sumOf { it.durationMs }
}

val meditations = listOf(
    MeditationSession(
        id = "pause",
        name = "Pausa consciente",
        subtitle = "3 min · no meio do dia",
        description = "Uma pausa curta para sair do automático, sentir o corpo e voltar ao que importa.",
        steps = listOf(
            MeditationStep(
                "Acomodar",
                "Encontre uma posição confortável. Se quiser, feche os olhos. Solte os ombros e deixe as mãos descansarem.",
                25
            ),
            MeditationStep(
                "Respirar",
                "Perceba o ar entrando e saindo. Não tente mudar o ritmo. Apenas acompanhe o caminho do ar.",
                25
            ),
            MeditationStep(
                "Sentir o corpo",
                "Leve a atenção aos pés e às pernas. Note o contato com o apoio. Depois, sinta o peso do corpo sobre a cadeira.",
                30
            ),
            MeditationStep(
                "Escutar",
                "Amplie a escuta para os sons ao redor. Não precise nomear nem julgar. Apenas escute.",
                25
            ),
            MeditationStep(
                "Pensamentos",
                "Quando um pensamento aparecer, reconheça e volte ao ar. Não é preciso afastar nada. A respiração está sempre aqui.",
                35
            ),
            MeditationStep(
                "Retornar",
                "Respire um pouco mais fundo. Perceba o corpo inteiro. Abra os olhos quando estiver pronto e leve esta pausa com você.",
                40
            )
        )
    ),
    MeditationSession(
        id = "body",
        name = "Corpo e respiração",
        subtitle = "5 min · relaxamento",
        description = "Uma prática guiada para soltar as tensões do corpo e respirar com mais espaço.",
        steps = listOf(
            MeditationStep(
                "Chegar",
                "Sente-se com as costas apoiadas. Deixe o queixo relaxado e os pés no chão. Feche os olhos, se for confortável.",
                30
            ),
            MeditationStep(
                "Respiração 4-6",
                "Inspire por quatro tempos e expire por seis. Sem forçar. Se o ritmo incomodar, volte à respiração natural.",
                45
            ),
            MeditationStep(
                "Pernas",
                "Leve a atenção aos pés, tornozelos e pernas. Sinta o que está aqui, sem precisar mudar.",
                40
            ),
            MeditationStep(
                "Tronco e mãos",
                "Perceba o abdômen, o peito e as mãos. Note o movimento suave da respiração.",
                45
            ),
            MeditationStep(
                "Rosto",
                "Relaxe a mandíbula, os olhos e a testa. Deixe o rosto ficar mais suave.",
                40
            ),
            MeditationStep(
                "Espaço",
                "Sinta o corpo inteiro, como um só espaço. Onde está a tensão? Não precisa resolvê-la agora.",
                45
            ),
            MeditationStep(
                "Retornar",
                "Respire fundo. Mova os dedos e os pés. Abra os olhos e escolha um gesto gentil para começar o resto do dia.",
                55
            )
        )
    ),
    MeditationSession(
        id = "slow",
        name = "Desacelerar",
        subtitle = "8 min · fim do dia",
        description = "Uma sessão mais longa para diminuir o ritmo, escutar o corpo e encerrar o dia com calma.",
        steps = listOf(
            MeditationStep(
                "Chegada",
                "Encontre uma posição estável. Sinta o apoio dos pés e da cadeira. Deixe os olhos se fecharem e a respiração encontrar seu próprio ritmo.",
                45
            ),
            MeditationStep(
                "Expirar",
                "A cada expiração, deixe o corpo soltar um pouco mais. Não há pressa. O ar sai, e o corpo pode descansar.",
                60
            ),
            MeditationStep(
                "Varredura",
                "Percorra o corpo com a atenção: pés, pernas, quadril, abdômen, peito, mãos, braços, pescoço e rosto. Onde houver tensão, apenas observe. Onde houver calma, permita que fique.",
                75
            ),
            MeditationStep(
                "Sons e espaço",
                "Escute os sons que chegam e desaparecem. Perceba também o silêncio entre eles. Você não precisa fazer nada.",
                60
            ),
            MeditationStep(
                "Emoções",
                "Se uma emoção aparecer, note onde ela toca o corpo. Respire com ela, sem tentar afastá-la. Nenhuma sensação precisa durar para sempre.",
                70
            ),
            MeditationStep(
                "Descanso",
                "Fique com a respiração. Não há nada a alcançar. Este momento já é suficiente. Deixe o corpo descansar por alguns instantes.",
                80
            ),
            MeditationStep(
                "Intenção",
                "Pense em uma qualidade que você gostaria de levar para o dia: paciência, presença ou gentileza. Escolha uma e mantenha-a perto.",
                50
            ),
            MeditationStep(
                "Retornar",
                "Volte a sentir o contato com o chão. Mova o corpo devagar. Abra os olhos quando estiver pronto. Leve esta calma com você.",
                40
            )
        )
    )
)
