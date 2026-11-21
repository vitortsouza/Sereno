# Sereno

Aplicativo Android pessoal para desacelerar com sons reais, respiração guiada,
meditação, diário de gratidão e rotinas de sono. Funciona offline e não exige
conta, cadastro ou servidor.

## Estado atual

- Versão: `0.6.0` (`versionCode 7`)
- Pacote: `com.opencode.sereno`
- Android mínimo: 8.0 (API 26)
- Alvo: Android 15 (API 35)
- Build validado no dispositivo: build 30

## Recursos

- Biblioteca com 31 gravações reais de natureza, música e ambientes.
- Mixer de até três camadas, misturas salvas, favoritos e busca.
- Timer com fade, modo dormir, notificação e controles por Bluetooth/fone.
- Respiração guiada com padrões, vibração, som guia e narração opcional.
- Meditação com voz offline do Brasil, ritmo ajustável, ambiente e tela bloqueada.
- Diário de gratidão com humor, histórico, exportação e lembrete diário.
- Tema claro, escuro ou do sistema.

## Requisitos para compilar

- Android Studio recente ou JDK 17.
- Android SDK 35.
- Conexão com a internet na primeira compilação para baixar dependências.

O projeto inclui Gradle Wrapper 8.7. No Linux/macOS, torne o script executável:

```sh
chmod +x gradlew
```

## Como compilar

Linux/macOS:

```sh
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:assembleDebug
```

O APK de debug será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Estrutura

```text
app/src/main/java/com/opencode/sereno/
  audio/       síntese, catálogo e reprodução dos sons
  data/        preferências, diário e misturas salvas
  playback/    player, meditação, serviço e foco de áudio
  reminder/    agendamento e notificação do lembrete
  ui/          telas, componentes e tema Compose
app/src/main/assets/sounds/    gravações PCM
```

## Privacidade

Os dados ficam apenas no aparelho. Não há analytics, anúncios, login nem
requisições de rede em tempo de execução.

## Áudio e licenciamento

As gravações em `app/src/main/assets/sounds/` foram incluídas para uso pessoal
neste projeto. Antes de publicar o repositório como público, confirme os direitos
de redistribuição ou substitua/remova esses arquivos. Para manter o repositório
leve e evitar problemas de licença, uma opção é publicá-lo como privado.

## Pendências conhecidas

- Validar o lembrete diário após as 20h.
- Opcional: reagendar o lembrete após reiniciar o aparelho.
- Acessibilidade e ajustes para telas pequenas estão fora do escopo atual.

## Roadmap

Consulte `ROADMAP.md` para o histórico das fases e pendências.
