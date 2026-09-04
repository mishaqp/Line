# mishaqp/Line — fork notes

Форк LineCode Pro для тестового rooted Android + agent runtime.
Не переписывать на Kotlin/Compose. Java 11, один Activity, без XML inflate — см. `CLAUDE.md`.

## Уже в `master`

- Root (`su`) execution target: `RootSupport`, `RootCommandRunner`, `RootFileExecutor`, `RootShellExecutor`
- Skills как инструменты агента: `skill_list` / `create` / `install` / `delete` / `set_enabled`
- Material 3 tokens, state layers, Dynamic Color / Material You
- Chat restyle (баблы, composer, chat scale, transcript mode)
- Agent Runtime + Codex OAuth + Full Access
- Локальный `shell_execute` (`sh -c`, PATH Termux/system, stdin → `/dev/null`)
- Root-mode system prompt
- `README_RU.md`

## Чем форк отличается от апстрима

| Тема | Апстрим LineCode Pro | Этот форк |
| --- | --- | --- |
| Execution | Local SAF / SSH / IPC | + Root (`su -c`) |
| Shell | Termux / SSH / IPC | + локальный шелл и root target |
| Skills | каталог на диске | агент сам ставит/выключает skills |
| Auth | API keys | + Codex OAuth |
| Policy | per-tool confirm | + Full Access (без confirm) |
| UI | Java views | + M3 tokens / Material You / chat restyle |

## Что ещё сделать

1. Индикатор Execution target в шапке чата (Local / SSH / IPC / Root + cwd).
2. Проба `su` до первого tool-call: binary / `uid=0` / interactive-useless.
3. Skills: hot-reload в `ToolRegistry`, preview манифеста при install, дедуп по имени.
4. Субагентам не отдавать root по умолчанию (`subagents inherit root` — отдельный флаг).
5. `shell_execute`: явный `target` user|root; не фолбечить на su только из-за 127.
6. Экран последних 50 tool calls (target / cwd / exit / duration).
7. Удалить слитые ветки `arena/*`, `feat/agent-runtime*`. Не коммитить `.idea/` и ci-diag.
