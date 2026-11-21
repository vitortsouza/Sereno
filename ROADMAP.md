# Sereno — Roadmap

## Fase 1 — Sons e misturas (concluída)
- Biblioteca de sons, mixer de até 3 camadas, misturas rápidas e salvas
- Timer com fade, fades configuráveis e aviso de fones
- Temas claro/escuro/sistema e modo som único
- Notificação de reprodução, MediaSession e controles por Bluetooth/fone
- Wakelock durante a reprodução

## Fase 2 — Respirar e diário (concluída)
- Respiração guiada com padrões e duração
- Sinais de vibração e som guia por fase
- Som de fundo opcional com volumes independentes
- Diário de gratidão com várias entradas por dia, humor e histórico

## Fase 3 — Meditação (concluída)
- Três meditações guiadas com TTS em português
- Voz do sistema com ritmo ajustável e pausas de pontuação
- Ambiente sonoro opcional com volume manual e níveis equilibrados
- Ducking automático do fundo durante voz e sinais
- Fallback em modo silencioso, pausa, retomada e encerramento

## Biblioteca de natureza (concluída)
- Gravações reais em loop no lugar dos sons sintetizados
- Duas intensidades de chuva: suave e forte
- Mar, ondas lentas, rio, riacho, cachoeira, lago, submerso
- Vento, floresta, pássaros, grilos, trovoada, neve
- Gaivotas, praia tropical, lareira e fogueira
- Normalização automática de volume e níveis perceptuais por som

## Fase 4 — Experiência completa (0.6.0, implementada)
- 12 novas gravações reais: piano suave, violão calmo, taças de cristal,
  hang drum, sino de templo, coral ao amanhecer, café com chuva, biblioteca,
  trem, quarto aconchegante, ventilador e chuva na barraca
- Sino de templo com repetição espaçada e cauda natural
- Variação lenta de volume nos loops para reduzir a sensação de repetição
- Escolha e teste de voz offline do Brasil nas práticas, com cache da narração
- Vozes online e de Portugal removidas da seleção
- Vozes duplicadas (PTE e PT-BR-LANGUAGE) ocultadas
- Narração opcional na respiração, volume próprio e contagem 3-2-1
- Favoritos e busca na biblioteca de sons
- Renomear misturas e desfazer exclusão
- Modo dormir: encerra o som ao fim do timer sem deixar sessão pausada
- Diagnóstico de áudio nas configurações
- Fluxo para permissão de notificação negada
- Meditação em segundo plano com notificação, MediaSession e tela bloqueada
- Exportação do diário e lembrete gentil diário às 20h

## Pendências para a versão final
- Retomada da meditação pausada validada no build 30
- Validar o lembrete das 20h
- Avaliar reagendamento do lembrete após reinicialização do aparelho
- Manter o modo dormir como está
- Exportação do diário validada
- Revisão de acessibilidade e telas pequenas fora do escopo
