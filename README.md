# Yukiss Blog

一个前后端分离的全栈博客项目。前端使用 Vue 3 + Vite + Element Plus，后端使用 Spring Boot + MyBatis + MySQL。

## 项目结构

```text
yukiss-blog/
├─ web/                        # 前端项目
│  ├─ src/
│  │  ├─ assets/               # 图片资源
│  │  ├─ composables/          # 组合式业务逻辑，如文章、用户、表单
│  │  ├─ router/               # Vue Router 路由
│  │  ├─ utils/                # 请求封装、日期工具
│  │  ├─ views/                # 页面：主页、社区、个人资料
│  │  ├─ App.vue               # 应用外壳和顶部导航
│  │  └─ main.js               # 前端入口
│  ├─ vite.config.js           # Vite 开发服务器配置
│  └─ package.json             # 前端依赖和脚本
│
├─ server/                     # 后端项目
│  ├─ src/main/java/moon/yukiss/
│  │  ├─ config/               # Web 配置、跨域、拦截器注册、静态资源映射
│  │  ├─ controller/           # HTTP 接口层
│  │  ├─ entity/               # 数据实体
│  │  ├─ interceptors/         # 登录 Token 拦截器
│  │  ├─ mapper/               # MyBatis SQL 映射
│  │  ├─ service/              # 业务逻辑层
│  │  └─ utils/                # JWT、ThreadLocal 工具
│  ├─ src/main/resources/
│  │  ├─ application.properties # 后端配置
│  │  └─ static/               # 已打包的前端静态文件
│  └─ pom.xml                  # Maven 依赖和构建配置
│
└─ uploads/                    # 上传文件目录，已被 Git 忽略
```

## 本地启动

### 后端

```bash
cd server
mvn spring-boot:run
```

默认地址：`http://localhost:4000`

后端默认连接本机 MySQL：`my_blog_db`。可以用环境变量覆盖：

```bash
BLOG_SERVER_PORT=4000
BLOG_DB_URL=jdbc:mysql://localhost:3306/my_blog_db?serverTimezone=GMT%2B8
BLOG_DB_USERNAME=root
BLOG_DB_PASSWORD=your_password
BLOG_UPLOAD_DIR=../uploads
BLOG_JWT_SECRET=change-me
BLOG_JWT_EXPIRATION_HOURS=12
```

### 前端

```bash
cd web
npm install
npm run dev
```

默认地址：`http://127.0.0.1:3000`

前端接口默认请求 `http://localhost:4000`。如需改后端地址，可以设置：

```bash
VITE_API_BASE_URL=http://localhost:4000
```

## 主要接口

- `POST /user/register`：注册
- `POST /user/login`：登录并返回 Token
- `POST /user/updateAvatar`：更新头像，需要 Token
- `DELETE /user/delete`：注销当前账号，需要 Token
- `GET /articles`：文章列表
- `GET /articles/{id}`：文章详情
- `POST /articles`：发布文章，需要 Token
- `POST /upload`：上传头像，需要 Token
- `POST /like`：点赞/取消点赞，需要 Token
- `POST /comment/add`：发布评论，需要 Token
- `GET /comment/list`：文章评论列表

## 本次整理后的约定

- 前端所有业务请求统一走 `src/utils/request.js`。
- 后端可变配置统一放在 `application.properties`，并支持环境变量覆盖。
- 上传文件目录由 `app.upload.dir` 控制，启动时会自动创建。
- 开发日志、构建产物、上传文件不会进入 Git。
