#!/usr/bin/env bash
# 本机一键部署：构建前后端 -> 上传到服务器 -> 安装并重启（Git Bash / Linux / macOS 均可）。
#
# 用法（在仓库任意目录）：
#   bash deploy/deploy.sh
# 环境变量：
#   DEPLOY_HOST  SSH 目标，默认 yukiss-blog（~/.ssh/config 中的别名）
#
# 部署前请确认测试已通过（CI 会在推送后运行），这里构建后端时跳过了测试。
set -euo pipefail

HOST="${DEPLOY_HOST:-yukiss-blog}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGE=/tmp/yukiss-blog-deploy

echo "==> 构建前端（VITE_API_BASE_URL 取自 web/.env.production）"
cd "$ROOT/web"
[[ -d node_modules ]] || npm ci --no-audit --no-fund
npm run build --silent

echo "==> 构建后端"
cd "$ROOT/server"
mvn -q -B -DskipTests clean package
JAR=$(ls target/blog-backend-*.jar | grep -v -- '-plain' | head -n 1)

echo "==> 上传到 $HOST"
ssh "$HOST" "rm -rf $STAGE && mkdir -p $STAGE/web"
scp -q "$JAR" "$HOST:$STAGE/app.jar"
scp -q "$ROOT/deploy/nginx/yukiss-blog.conf" \
       "$ROOT/deploy/systemd/yukiss-blog.service" \
       "$ROOT/deploy/remote-install.sh" "$HOST:$STAGE/"
tar -C "$ROOT/web/dist" -czf - . | ssh "$HOST" "tar -C $STAGE/web -xzf -"

echo "==> 在服务器上安装"
ssh "$HOST" "sudo bash $STAGE/remote-install.sh $STAGE"

echo "==> 部署完成"
