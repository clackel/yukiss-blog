# Yukiss Blog

一个前后端分离的个人博客 / 社区练习项目：用户可以注册登录、用 Markdown 写文章、评论与回复、点赞、关注其他用户，并维护个人主页。

## 功能

- 账号：注册、登录（JWT）、修改资料与密码、上传头像、绑定邮箱、通过邮箱找回用户名、注销账号
- 文章：Markdown 编辑与渲染（XSS 过滤）、插入图片、草稿、标签、发布 / 编辑 / 删除、全文搜索、按标签筛选
- 互动：评论与一级回复（分页、编辑、删除）、文章点赞、评论点赞
- 社交：公开用户主页、关注 / 取消关注、站内通知（评论、回复、点赞、关注）
- 安全：改密码 / 注销后旧登录态立即失效、登录失败锁定、验证码防暴力破解、按文件内容校验上传图片

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 前端 `web/` | Vue 3、Vite、Vue Router、Element Plus、Axios、markdown-it、DOMPurify |
| 后端 `server/` | Java 21、Spring Boot 4、MyBatis、MySQL、java-jwt、Lombok |
| 测试 | JUnit 5、Mockito、Vitest、Vue Test Utils |

## 目录结构

```text
yukiss-blog/
├── server/        Spring Boot 后端（端口 4000）
├── web/           Vue 3 前端（端口 3000）
├── docs/          项目文档
│   ├── API.md         接口文档
│   ├── DEVELOPMENT.md 开发指南（配置、架构、约定、测试）
│   └── DEPLOYMENT.md  部署指南（服务器初始化、一键部署、运维）
├── deploy/        Nginx / systemd 配置与部署脚本
├── uploads/       上传文件目录（运行时生成，已忽略）
└── start.bat      Windows 一键启动脚本
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+ 与 npm
- MySQL 8（数据库 `my_blog_db` 与表结构会在后端启动时自动创建）

### 配置

后端配置通过 `BLOG_*` 变量覆盖。复制 `server/.env.example` 为 `server/.env`，至少填写数据库密码 `BLOG_DB_PASSWORD`：

```bash
cp server/.env.example server/.env
```

后端启动时会自动读取 `server/.env`（需在 `server/` 目录下启动）。同名的系统环境变量优先级更高。`.env` 已被 git 忽略，不要提交。

前端如需修改后端地址，复制 `web/.env.example` 为 `web/.env` 并修改 `VITE_API_BASE_URL`（默认 `http://localhost:4000`）。

### 启动

Windows 下可直接运行根目录的 `start.bat`，它会检查依赖、首次安装前端依赖，并在两个窗口中分别启动前后端。

也可以手动启动：

```bash
cd server
mvn spring-boot:run
```

```bash
cd web
npm install
npm run dev
```

启动后访问 <http://127.0.0.1:3000>。

## 测试

```bash
cd server
mvn test
```

```bash
cd web
npm test
npm run lint
```

推送到 GitHub 后，GitHub Actions 会自动运行前后端测试、代码检查和前端构建。

## 文档

- [接口文档](docs/API.md)：请求约定、数据结构与全部接口
- [开发指南](docs/DEVELOPMENT.md)：配置项、架构说明、编码约定与构建发布
- [部署指南](docs/DEPLOYMENT.md)：服务器初始化、`bash deploy/deploy.sh` 一键部署、日志、回滚与备份
