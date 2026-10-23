#!/usr/bin/env bash
# 新服务器一次性初始化（Ubuntu 24.04），可重复执行。
# 安装 JDK 21 / MySQL 8 / Nginx，创建运行账号、目录、数据库、专用数据库账号，
# 并生成 /etc/yukiss-blog/blog.env（随机数据库密码和 JWT 密钥只保存在服务器上）。
#
# 用法（在本机仓库根目录）：
#   ssh yukiss-blog 'sudo bash -s http://服务器IP或域名' < deploy/setup-server.sh
set -euo pipefail

PUBLIC_ORIGIN="${1:-}"
ENV_FILE=/etc/yukiss-blog/blog.env

if [[ $EUID -ne 0 ]]; then
    echo "请用 sudo 运行" >&2
    exit 1
fi

rand() { openssl rand -base64 96 | tr -dc 'A-Za-z0-9' | head -c "$1"; }

echo "==> 安装软件包"
export DEBIAN_FRONTEND=noninteractive
timedatectl set-timezone Asia/Shanghai
apt-get update -qq
apt-get install -y -qq openjdk-21-jdk-headless mysql-server nginx >/dev/null
systemctl enable --now mysql nginx >/dev/null

echo "==> 创建运行账号和目录"
id yukiss >/dev/null 2>&1 || useradd --system --home-dir /opt/yukiss-blog --shell /usr/sbin/nologin yukiss
install -d -o yukiss -g yukiss -m 750 /opt/yukiss-blog /var/lib/yukiss-blog /var/lib/yukiss-blog/uploads
install -d -o root -g yukiss -m 750 /etc/yukiss-blog
install -d -o root -g root -m 755 /var/www/yukiss-blog

if [[ -f $ENV_FILE ]]; then
    echo "==> $ENV_FILE 已存在，保留现有密钥"
else
    echo "==> 创建数据库、专用账号和 $ENV_FILE"
    if [[ -z $PUBLIC_ORIGIN ]]; then
        echo "首次运行需要传入站点地址，例如 http://1.2.3.4" >&2
        exit 1
    fi
    DB_PASSWORD=$(rand 32)
    JWT_SECRET=$(rand 64)
    mysql <<SQL
CREATE DATABASE IF NOT EXISTS my_blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'yukiss_blog'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER 'yukiss_blog'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON my_blog_db.* TO 'yukiss_blog'@'localhost';
FLUSH PRIVILEGES;
SQL
    umask 027
    cat >"$ENV_FILE" <<EOF
# Yukiss Blog 生产环境配置（由 deploy/setup-server.sh 生成，含密钥，勿外传）
BLOG_SERVER_PORT=4000
BLOG_DB_URL=jdbc:mysql://localhost:3306/my_blog_db?serverTimezone=GMT%2B8
BLOG_DB_USERNAME=yukiss_blog
BLOG_DB_PASSWORD=${DB_PASSWORD}
BLOG_JWT_SECRET=${JWT_SECRET}
BLOG_JWT_EXPIRATION_HOURS=12
BLOG_UPLOAD_DIR=/var/lib/yukiss-blog/uploads
BLOG_CORS_ALLOWED_ORIGINS=${PUBLIC_ORIGIN}
BLOG_EMAIL_DEV_RETURN_CODE=false
BLOG_LOG_LEVEL=info
EOF
    unset DB_PASSWORD JWT_SECRET
fi

# 旧版本生成的配置文件可能缺少后来新增的变量，这里只补缺、不覆盖
grep -q '^BLOG_LOG_LEVEL=' "$ENV_FILE" || echo 'BLOG_LOG_LEVEL=info' >>"$ENV_FILE"
chown root:yukiss "$ENV_FILE"
chmod 640 "$ENV_FILE"

echo "==> 完成。MySQL 只监听本机："
ss -ltn | awk 'NR==1 || /:3306 /'
