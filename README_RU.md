# Line (форк mishaqp)

Карманный AI-воркспейс для Android 8+: чат с tool-loop, файлы, шелл, SSH, IPC и root.

Полное описание архитектуры — в [README.md](README.md) и [CLAUDE.md](CLAUDE.md).
Отличия форка — в [FORK.md](FORK.md).

## Что умеет этот форк

- Чаты: OpenAI-compatible, Anthropic Messages, Codex Responses, локальный GGUF.
- Инструменты: файлы, glob, шелл, web, картинки, субагенты, todo, memory, skills.
- Режимы исполнения: Local / SSH / Terminal Provider (IPC) / **Root**.
- Skills: агент может ставить, включать и удалять скиллы.
- Тема: M3-токены без AppCompat/MDC, Material You.
- На ветке `feat/agent-runtime`: Full Access и Codex OAuth.

## Быстрый старт

1. Собрать `debugUserCert` APK или взять артефакт CI.
2. Выдать доступ к хранилищу, добавить модель.
3. Открыть папку проекта.
4. Root: Settings → MCP execution mode → Root.
5. Skills: `GLOBAL_SKILLS_ROOT` и `WORKSPACE_SKILLS_ROOT`.

## Сборка

```bash
./gradlew :app:assembleDebug
./gradlew :app:exportDebugUserCertApk
```

Java 11 only.
