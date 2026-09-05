#!/system/bin/sh
# Tiny GitHub API wrapper for Line root shell.
# Usage: api.sh GET /repos/mishaqp/Line/pulls
#        api.sh POST /repos/mishaqp/Line/git/refs '{"ref":"refs/heads/feat/x","sha":"..."}'
set -e
METHOD=${1:-GET}
PATH_Q=${2:-/}
BODY=${3:-}
TOKEN=""
if [ -n "$GITHUB_TOKEN" ]; then TOKEN=$GITHUB_TOKEN; fi
if [ -z "$TOKEN" ] && [ -n "$GH_TOKEN" ]; then TOKEN=$GH_TOKEN; fi
for f in /data/media/0/.linecode/github_token /data/data/cn.lineai/files/.linecode/github_token; do
  if [ -z "$TOKEN" ] && [ -f "$f" ]; then TOKEN=$(tr -d '\n\r ' < "$f"); fi
done
if [ -z "$TOKEN" ]; then
  echo "no github token. put PAT in /data/media/0/.linecode/github_token" >&2
  exit 2
fi
URL="https://api.github.com${PATH_Q}"
if [ -n "$BODY" ]; then
  curl -sS -X "$METHOD" -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    -H "Content-Type: application/json" \
    -d "$BODY" "$URL"
else
  curl -sS -X "$METHOD" -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "$URL"
fi
