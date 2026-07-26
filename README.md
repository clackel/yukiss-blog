# Yukiss Blog

Yukiss Blog 是一个面向朋友的小型博客社区。访客无需登录即可阅读公开文章；注册用户可以使用 Markdown 写作、管理自己的文章、点赞、评论和回复。

## 功能

- 公开落地页、社区文章流和文章详情
- 开放注册、登录、账号找回与个人资料管理
- Markdown 长文编辑、实时预览和安全渲染
- 文章关键词搜索、最新/热门排序和分页
- 作者文章新建、编辑与删除
- 文章点赞、评论点赞和一级评论回复
- 浅色、深色、跟随系统三种主题
- 响应式桌面与移动端界面
- 本地头像上传与环境变量配置

## 技术栈

- 前端：Vue 3、Vite、Vue Router、Element Plus、Axios
- Markdown：markdown-it、DOMPurify
- 后端：Spring Boot、MyBatis、MySQL、JWT
- 测试：JUnit 5、Mockito、Vitest、Vue Test Utils

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+
- MySQL 8+

### Windows 一键启动

先在当前终端设置数据库密码，再运行：

```bat
set BLOG_DB_PASSWORD=你的数据库密码
start.bat
```

首次启动会自动安装前端依赖。默认地址：

- 前端：`http://127.0.0.1:3000`
- 后端：`http://localhost:4000`

默认数据库为 `my_blog_db`。数据库账号具备创建数据库权限时，后端会自动创建数据库和所需表；已有表和数据不会被覆盖。

### 手动启动

后端：

```bash
cd blog-backend
mvn spring-boot:run
```

前端：

```bash
cd blog-frontend
npm install
npm run dev
```

## 配置

后端通过环境变量覆盖默认配置。可参考 `blog-backend/.env.example`，但 Spring Boot 不会自动读取该文件，需要在终端或启动环境中设置变量。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BLOG_SERVER_PORT` | `4000` | 后端端口 |
| `BLOG_DB_URL` | 本机 `my_blog_db` | MySQL JDBC 地址 |
| `BLOG_DB_USERNAME` | `root` | 数据库用户名 |
| `BLOG_DB_PASSWORD` | 空 | 数据库密码 |
| `BLOG_UPLOAD_DIR` | `../uploads` | 头像文件目录 |
| `BLOG_JWT_SECRET` | 本地开发密钥 | JWT 签名密钥 |
| `BLOG_JWT_EXPIRATION_HOURS` | `12` | 登录有效小时数 |
| `BLOG_CORS_ALLOWED_ORIGINS` | 本机前端地址 | 允许的前端来源，多个值用逗号分隔 |
| `BLOG_EMAIL_DEV_RETURN_CODE` | `true` | 是否在响应中返回本地验证码 |

前端可以在 `blog-frontend/.env.local` 中覆盖后端地址：

```bash
VITE_API_BASE_URL=http://localhost:4000
```

## 临时分享给朋友

同一局域网或临时隧道分享时：

1. 把 `VITE_API_BASE_URL` 设置为朋友设备可以访问的后端地址。
2. 把 `BLOG_CORS_ALLOWED_ORIGINS` 设置为实际前端地址；多个来源使用逗号分隔。
3. 启动后端，然后运行：

```bash
cd blog-frontend
npm run dev:share
```

前端会监听所有网络接口。仓库不内置隧道服务，也不会自动公网发布；使用临时隧道时应分别配置可访问的前端和后端地址。

## 常用命令

```bash
# 后端测试
cd blog-backend
mvn clean test

# 前端测试
cd blog-frontend
npm test

# 前端生产构建
npm run build
```

## 主要页面

| 路径 | 访问权限 | 用途 |
| --- | --- | --- |
| `/` | 公开 | 落地页和最新文章 |
| `/community` | 公开 | 搜索、排序和浏览文章 |
| `/articles/{id}` | 公开 | Markdown 阅读与评论 |
| `/home` | 登录 | 我的文章与创作统计 |
| `/editor` | 登录 | 发布文章 |
| `/editor/{id}` | 作者本人 | 编辑文章 |
| `/profile` | 登录 | 头像、资料、邮箱与安全设置 |

## 项目结构

```text
yukiss-blog/
├─ blog-frontend/       # Vue 前端
│  ├─ src/components/   # 通用文章卡片、登录弹窗、Markdown 渲染
│  ├─ src/views/        # 页面
│  └─ src/utils/        # 请求、Markdown、查询和校验工具
├─ blog-backend/        # Spring Boot 后端
│  ├─ src/main/java/    # 控制器、服务、Mapper 和配置
│  └─ src/test/java/    # 后端测试
├─ uploads/             # 本地上传目录，不进入 Git
└─ API.md               # 完整接口说明
```

详细请求与响应格式见 [API.md](./API.md)。
