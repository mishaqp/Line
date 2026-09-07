# Line (форк mishaqp)

Карманный AI-воркспейс для Android 8+: чат с tool-loop, файлы, шелл, SSH, IPC и root.

Полное описание архитектуры — в [README.md](README.md) и [CLAUDE.md](CLAUDE.md).
Отличия форка и остаток работы — в [FORK.md](FORK.md).

## Что умеет этот форк (всё уже в `master`)

- Чаты: OpenAI-compatible, Anthropic Messages, Codex Responses / OAuth, локальный GGUF.
- Инструменты: файлы, glob, шелл, web, картинки, субагенты, todo, memory, skills.
- Режимы исполнения: Local / SSH / Terminal Provider (IPC) / **Root**.
- Локальный шелл без обязательного Termux/SSH (`sh -c`).
- Skills: агент ставит, включает и удаляет скиллы.
- Full Access: глобальный тумблер, `needsConfirmation = false`.
- Agent Runtime и Codex-аккаунт.
- Тема: M3-токены без AppCompat/MDC, Material You, чат в духе RikkaHub.

## Быстрый старт

1. Собрать `debugUserCert` APK или взять артефакт CI.
2. Выдать доступ к хранилищу, добавить модель.
3. Открыть папку проекта.
4. Root: Settings → MCP execution mode → Root.
5. Full Access: Settings → тумблер Full Access (только для тестового форка).
6. Skills: `GLOBAL_SKILLS_ROOT` и `WORKSPACE_SKILLS_ROOT`.

## Сборка

```bash
./gradlew :app:assembleDebug
./gradlew :app:exportDebugUserCertApk
```

Java 11 only. Не коммитить `.idea/` и ci-diag.
