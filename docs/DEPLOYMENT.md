# 部署指南

本文档说明如何把 Yukiss Blog 部署到一台 Ubuntu 24.04 服务器（当前为腾讯云轻量应用服务器）。部署相关文件都在 `deploy/` 目录。

## 架构

```text
浏览器 ──HTTP──▶ Nginx :80
                  ├── /          → /var/www/yukiss-blog（前端静态文件，SPA 路由回退到 index.html）
                  └── /api/      → 127.0.0.1:4000（Spring Boot，去掉 /api 前缀后转发）
                                     └── MySQL 8（只监听 127.0.0.1:3306）
```

- 前端和后端同域，生产构建时 `VITE_API_BASE_URL=/api`（见 `web/.env.production`），不存在跨域问题。
- 后端路径与前端路由（如 `/articles/12`）冲突的问题由 `/api` 前缀解决；刷新任意前端页面都会返回 `index.html`。
- 上传文件保存在 `/var/lib/yukiss-blog/uploads`，通过 `/api/uploads/文件名` 访问。

| 文件 | 服务器上的位置 | 作用 |
| --- | --- | --- |
| `deploy/nginx/yukiss-blog.conf` | `/etc/nginx/sites-available/yukiss-blog` | 静态文件、`/api` 反向代理、上传大小上限、缓存与 gzip |
| `deploy/systemd/yukiss-blog.service` | `/etc/systemd/system/yukiss-blog.service` | 以 `yukiss` 用户运行后端，开机自启、崩溃自动重启 |
| `deploy/setup-server.sh` | — | 新服务器一次性初始化 |
| `deploy/deploy.sh` | — | 本机一键构建并部署 |
| `deploy/remote-install.sh` | — | 由 `deploy.sh` 上传到服务器执行的安装步骤 |

## 服务器上的目录

| 路径 | 说明 |
| --- | --- |
| `/opt/yukiss-blog/app.jar` | 后端 jar；上一个版本保留为 `app.jar.prev` |
| `/var/www/yukiss-blog` | 前端构建产物；上一个版本保留为 `/var/www/yukiss-blog.prev` |
| `/var/lib/yukiss-blog/uploads` | 上传的头像和文章图片 |
| `/etc/yukiss-blog/blog.env` | 生产配置（数据库密码、JWT 密钥等），权限 `root:yukiss 640` |

## 首次部署

### 1. 准备服务器

- 防火墙（腾讯云控制台 → 服务器 → 防火墙）：放通 22、80、443，**不要**放通 3306、4000。
- SSH 密钥登录：在控制台“SSH 密钥”中导入本机公钥并绑定实例（绑定后密码登录自动关闭），然后在本机 `~/.ssh/config` 中添加别名：

```text
Host yukiss-blog
    HostName 服务器公网IP
    User ubuntu
    IdentityFile ~/.ssh/yukiss_blog_ed25519
    IdentitiesOnly yes
```

### 2. 初始化服务器

在仓库根目录执行（参数为站点访问地址，用于 CORS 配置）：

```bash
ssh yukiss-blog 'sudo bash -s http://服务器公网IP' < deploy/setup-server.sh
```

脚本会安装 JDK 21、MySQL 8、Nginx，创建 `yukiss` 运行账号、目录、数据库 `my_blog_db` 和只能访问该库的专用账号 `yukiss_blog`，并生成 `/etc/yukiss-blog/blog.env`。数据库密码和 JWT 密钥在服务器上随机生成，不会经过本机。脚本可以重复执行：配置文件已存在时保留现有密钥，只补充缺少的变量。

### 3. 部署

```bash
bash deploy/deploy.sh
```

脚本依次：构建前端 → 构建后端 jar（跳过测试）→ 上传到服务器 `/tmp` → 安装前后端、更新 Nginx 与 systemd 配置 → 重启后端 → 健康检查（请求 `/articles/tags`，最多等待 2 分钟）。默认部署到 SSH 别名 `yukiss-blog`，可用 `DEPLOY_HOST=其他主机 bash deploy/deploy.sh` 覆盖。

首次启动时，后端会自动建表和建全文索引。

## 日常更新

代码推送并确认 CI 通过后，在本机执行 `bash deploy/deploy.sh` 即可。表结构变化由后端启动时的 `*SchemaInitializer` 自动升级，无需手动改库。

## 运维

```bash
ssh yukiss-blog

sudo systemctl status yukiss-blog          # 后端状态
sudo journalctl -u yukiss-blog -f          # 实时日志
sudo journalctl -u yukiss-blog --since "1 hour ago"
sudo systemctl restart yukiss-blog         # 重启后端
sudo nginx -t && sudo systemctl reload nginx
sudoedit /etc/yukiss-blog/blog.env         # 修改配置后需要 restart 后端
```

### 回滚到上一个版本

```bash
sudo cp /opt/yukiss-blog/app.jar.prev /opt/yukiss-blog/app.jar && sudo systemctl restart yukiss-blog
sudo rm -rf /var/www/yukiss-blog && sudo mv /var/www/yukiss-blog.prev /var/www/yukiss-blog
```

### 备份

数据都在 MySQL 和上传目录里，建议定期备份并下载到本机或对象存储：

```bash
sudo mysqldump --single-transaction my_blog_db | gzip > ~/my_blog_db-$(date +%F).sql.gz
sudo tar -czf ~/uploads-$(date +%F).tar.gz -C /var/lib/yukiss-blog uploads
```

腾讯云控制台的“快照”功能也可以对整块系统盘做备份。

## 生产配置

`/etc/yukiss-blog/blog.env` 中与开发环境不同的设置：

| 变量 | 生产值 | 说明 |
| --- | --- | --- |
| `BLOG_DB_USERNAME` / `BLOG_DB_PASSWORD` | `yukiss_blog` / 随机 | 专用账号，只有 `my_blog_db` 的权限 |
| `BLOG_JWT_SECRET` | 随机 64 位 | 修改后所有用户需要重新登录 |
| `BLOG_UPLOAD_DIR` | `/var/lib/yukiss-blog/uploads` | systemd 只允许后端写这个目录 |
| `BLOG_CORS_ALLOWED_ORIGINS` | 站点地址 | 同域部署时其实用不到，绑定域名后改成域名 |
| `BLOG_EMAIL_DEV_RETURN_CODE` | `false` | 验证码不再出现在接口响应中（目前也没有接入发信，绑定邮箱和找回账号暂不可用） |
| `BLOG_LOG_LEVEL` | `info` | 项目包的日志级别 |

## 域名与 HTTPS（尚未配置）

服务器位于中国大陆，使用域名访问 80/443 端口前必须完成 ICP 备案（腾讯云控制台 → 备案）。备案通过后：

1. 在域名解析中添加 A 记录指向服务器公网 IP。
2. 把 `deploy/nginx/yukiss-blog.conf` 中的 `server_name _;` 改为域名，并修改 `blog.env` 中的 `BLOG_CORS_ALLOWED_ORIGINS`。
3. 申请免费证书（`sudo apt install certbot python3-certbot-nginx && sudo certbot --nginx -d 域名`），或在腾讯云 SSL 证书控制台申请后手动配置。

## 已知限制

- 后端启动的最初几秒（建表 / 升级表结构期间）可能返回 500，健康检查会自动等待。
- 登录失败计数、验证码保存在后端内存中，重启后清空；当前为单实例部署，不受影响。
- `server/src/main/resources/static/` 中的旧版前端不再使用，生产页面由 Nginx 提供。
