#!/usr/bin/env bash
# 在服务器上以 root 执行，由 deploy/deploy.sh 调用，不需要手动运行。
# 把上传到暂存目录的构建产物安装到位，更新 Nginx / systemd 配置，重启后端并做健康检查。
# 上一个版本保留为 app.jar.prev 和 /var/www/yukiss-blog.prev，用于回滚。
set -euo pipefail

STAGE="${1:?用法: remote-install.sh <暂存目录>}"
APP_DIR=/opt/yukiss-blog
WEB_DIR=/var/www/yukiss-blog
SERVICE=yukiss-blog

if [[ ! -f /etc/yukiss-blog/blog.env ]]; then
    echo "缺少 /etc/yukiss-blog/blog.env，请先运行 deploy/setup-server.sh" >&2
    exit 1
fi

echo "==> 安装前端"
rm -rf "${WEB_DIR}.new"
cp -r "$STAGE/web" "${WEB_DIR}.new"
chown -R root:root "${WEB_DIR}.new"
chmod -R u=rwX,go=rX "${WEB_DIR}.new"
rm -rf "${WEB_DIR}.prev"
[[ -d $WEB_DIR ]] && mv "$WEB_DIR" "${WEB_DIR}.prev"
mv "${WEB_DIR}.new" "$WEB_DIR"

echo "==> 安装后端"
[[ -f $APP_DIR/app.jar ]] && cp -p "$APP_DIR/app.jar" "$APP_DIR/app.jar.prev"
install -o yukiss -g yukiss -m 640 "$STAGE/app.jar" "$APP_DIR/app.jar"

echo "==> 更新 systemd 和 Nginx 配置"
install -m 644 "$STAGE/yukiss-blog.service" /etc/systemd/system/$SERVICE.service
systemctl daemon-reload
systemctl enable $SERVICE >/dev/null 2>&1
install -m 644 "$STAGE/yukiss-blog.conf" /etc/nginx/sites-available/yukiss-blog
ln -sf /etc/nginx/sites-available/yukiss-blog /etc/nginx/sites-enabled/yukiss-blog
rm -f /etc/nginx/sites-enabled/default
nginx -t -q
systemctl reload nginx

echo "==> 重启后端"
systemctl restart $SERVICE

# 公开接口 /articles/tags 能返回数据即视为启动成功（启动时会先执行表结构升级）
echo -n "==> 等待后端就绪"
for _ in $(seq 1 60); do
    if curl -fs -o /dev/null http://127.0.0.1:4000/articles/tags; then
        echo " OK"
        curl -fsS -o /dev/null http://127.0.0.1/api/articles/tags && echo "==> Nginx /api 转发正常"
        rm -rf "$STAGE"
        exit 0
    fi
    if ! systemctl is-active --quiet $SERVICE && [[ $(systemctl show -p NRestarts --value $SERVICE) -ge 3 ]]; then
        break
    fi
    echo -n "."
    sleep 2
done

echo
echo "!! 后端没有在预期时间内就绪，最近日志：" >&2
journalctl -u $SERVICE -n 40 --no-pager >&2 || true
echo "!! 回滚：sudo cp $APP_DIR/app.jar.prev $APP_DIR/app.jar && sudo systemctl restart $SERVICE" >&2
exit 1
