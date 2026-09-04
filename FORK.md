# mishaqp/Line — fork notes

Форк LineCode Pro для тестового rooted Android + agent runtime.

## Уже в `master`

- Root (`su`) execution target: `RootSupport`, `RootCommandRunner`, `RootFileExecutor`, `RootShellExecutor`
- Skills как инструменты агента: `skill_list` / `create` / `install` / `delete` / `set_enabled`
- Material 3 tokens, state layers, Dynamic Color / Material You

## Открытые PR

- #1 `feat/agent-runtime` — Codex OAuth, Agent Runtime, Full Access, локальный `shell_execute`
- #5 `arena/01a06dc9-line` — рестайл чата в духе RikkaHub

## План слияния

1. Rebase `#1` на актуальный `master` и squash-merge.
2. Squash-merge `#5` после зелёного CI.
3. Удалить слитые `arena/*`.
4. Не коммитить `.idea/` и `ci-diag/`.

## Чем форк отличается от апстрима

| Тема | Апстрим LineCode Pro | Этот форк |
| --- | --- | --- |
| Execution | Local SAF / SSH / IPC | + Root (`su -c`) |
| Shell | Termux / SSH / IPC | + локальный шелл и root target |
| Skills | каталог на диске | агент сам ставит/выключает skills |
| Auth | API keys | + Codex OAuth (ветка runtime) |
| UI | Java views | + M3 tokens / Material You / chat restyle |
