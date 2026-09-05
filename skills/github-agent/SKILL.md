---
name: github-agent
description: GitHub-only coding agent for mishaqp/Line. Use when the user wants PRs, files, Actions, releases, or merge without a local checkout. Triggers include гитхаб, PR, workflow, APK, влей, push, skill install.
---

# GitHub Agent

Работай только через GitHub API. Не клонируй репо на телефон, не пиши код «на устройство и потом залей».

Дефолты если пользователь не сказал иначе
- owner/repo `mishaqp/Line`
- base `master`
- API `https://api.github.com`

## Токен

Не читай токен вслух, не клади его в чат и не пиши в SKILL.md.

Первый найденный
1. `$GITHUB_TOKEN` или `$GH_TOKEN`
2. `/data/media/0/.linecode/github_token`
3. `/data/data/cn.lineai/files/.linecode/github_token`

Права PAT `repo` + `workflow` (или fine-grained Contents, Pull requests, Actions, Workflows на `mishaqp/Line`).

Вызовы через `shell_execute`, cwd `/data`.

```
TOKEN_FILE=/data/media/0/.linecode/github_token
TOKEN=$(tr -d '\n\r ' < "$TOKEN_FILE")
AUTH="Authorization: Bearer $TOKEN"
API=https://api.github.com
REPO=mishaqp/Line
```

Если файла нет — остановись и скажи пользователю создать PAT и положить в этот путь одной строкой. Не выдумывай токен.

Скрипт рядом с skill (esли установлен каталогом)
`scripts/api.sh GET /repos/mishaqp/Line/pulls`

## Правила

- Новая работа — ветка `feat/...` от `master`, потом PR, не коммит прямо в master без просьбы.
- Файлы пиши Contents API (`PUT /repos/{repo}/contents/{path}`), не git commit на устройстве.
- Перед перезаписью файла возьми текущий `sha`.
- APK — `workflow_dispatch` у `test-build-apk.yml`, артефакт `LineCode-user-cert-*.apk` в Releases.
- Не трогай `MainCoordinator.java` целиком, если можно избежать.
- Root cwd `/data`. Пути пользователя `/data/media/0/...`, не `/sdcard` как cwd.

## Рецепты

Шапка каждого curl
`-H "$AUTH" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28"`

Создать ветку от master
```
SHA=$(curl -sS -H "$AUTH" $API/repos/$REPO/git/ref/heads/master | sed -n 's/.*"sha": "\([^"]*\)".*/\1/p' | head -1)
curl -sS -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"ref\":\"refs/heads/feat/NAME\",\"sha\":\"$SHA\"}" \
  $API/repos/$REPO/git/refs
```

Прочитать файл
```
curl -sS -H "$AUTH" $API/repos/$REPO/contents/PATH?ref=BRANCH
```
Ответ `content` — base64. Декод `base64 -d`.

Записать файл
```
OLD_SHA=$(curl -sS -H "$AUTH" "$API/repos/$REPO/contents/PATH?ref=BRANCH" | sed -n 's/.*"sha": "\([^"]*\)".*/\1/p' | head -1)
B64=$(base64 -w0 FILE || base64 FILE)
curl -sS -X PUT -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"message\":\"MSG\",\"content\":\"$B64\",\"branch\":\"BRANCH\",\"sha\":\"$OLD_SHA\"}" \
  $API/repos/$REPO/contents/PATH
```
Для нового файла поле `sha` не шли.

Открыть PR
```
curl -sS -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"title":"TITLE","head":"feat/NAME","base":"master","body":"BODY"}' \
  $API/repos/$REPO/pulls
```

Слить PR squash
```
curl -sS -X PUT -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"merge_method":"squash"}' \
  $API/repos/$REPO/pulls/NUMBER/merge
```

Запустить тестовый APK
```
curl -sS -X POST -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"ref":"BRANCH","inputs":{"note":"from phone agent"}}' \
  $API/repos/$REPO/actions/workflows/test-build-apk.yml/dispatches
```

Последний релиз
```
curl -sS -H "$AUTH" $API/repos/$REPO/releases?per_page=3
```

## Ответ пользователю

Коротко ссылки на commit / PR / run / APK. Не пасть сырой JSON. Не печатай токен.
