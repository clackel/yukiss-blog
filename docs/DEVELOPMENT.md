# 开发指南

本文档面向参与开发的人，说明配置项、整体架构、编码约定、测试与构建方式。接口细节见 [API.md](API.md)。

## 配置项

后端配置位于 `server/src/main/resources/application.yml`，所有值都可以通过环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BLOG_SERVER_PORT` | `4000` | 后端端口 |
| `BLOG_DB_URL` | `jdbc:mysql://localhost:3306/my_blog_db?createDatabaseIfNotExist=true&serverTimezone=GMT%2B8` | 数据库连接，库不存在时自动创建 |
| `BLOG_DB_USERNAME` | `root` | 数据库用户名 |
| `BLOG_DB_PASSWORD` | 空 | 数据库密码 |
| `BLOG_UPLOAD_DIR` | `${user.dir}/../uploads` | 上传文件目录，对外映射为 `/uploads/**` |
| `BLOG_JWT_SECRET` | `yukiss-dev-secret-change-me` | JWT 签名密钥，非本地环境必须修改 |
| `BLOG_JWT_EXPIRATION_HOURS` | `12` | JWT 有效期（小时） |
| `BLOG_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` | 允许跨域的前端地址，逗号分隔 |
| `BLOG_EMAIL_DEV_RETURN_CODE` | `true` | 为 `true` 时邮箱验证码直接在接口响应中返回（项目未接入真实邮件发送） |

前端只有一个变量：`VITE_API_BASE_URL`，默认 `http://localhost:4000`。

## 后端架构

包根为 `moon.yukiss`，按层组织：

```text
controller → service（接口）→ service/impl → mapper → MySQL
```

| 包 | 职责 |
| --- | --- |
| `controller` | 路由与参数校验，返回 `ApiResponse` |
| `service` / `service/impl` | 业务逻辑与权限判断 |
| `mapper` | MyBatis 接口；复杂 SQL 放在 `resources/mapper/*.xml` |
| `dto` / `entity` | 请求对象 / 数据库实体 |
| `common` | 统一响应、分页结果、业务异常、全局异常处理 |
| `config` | Web 配置（CORS、拦截器、静态资源）与表结构初始化 |
| `interceptors` | 登录拦截器 |
| `utils` | JWT、密码哈希、`ThreadLocalUtil` |

### 表结构管理

项目不使用迁移文件。`BlogSchemaInitializer` 与 `UserSchemaInitializer` 在启动时执行 `CREATE TABLE IF NOT EXISTS`，并通过幂等的“缺失才添加”方法补充新字段和索引。**修改表结构时请在这两个类中追加升级步骤**，不要修改已有建表语句来代替升级。可通过 `app.schema-initializer.enabled=false` 关闭。

### 认证

- 登录成功后返回 JWT，前端将其**原样**放入 `Authorization` 请求头，不加 `Bearer` 前缀。
- `LoginInterceptor` 拦截除 `WebConfig` 中排除路径以外的所有请求，解析出的 claims 存入 `ThreadLocalUtil`，请求结束时清理。业务代码从 `ThreadLocalUtil.get()` 获取当前用户。
- 部分 GET 接口（文章列表、文章详情、评论列表、用户主页等）为公开读取：不要求登录，但若携带有效 JWT 会解析当前用户，用于计算 `likedByMe` 等字段。

### 响应与错误

- 所有接口返回 `ApiResponse`：`{ success, message, data }`，分页数据使用 `PageResult`。
- 可预期的失败抛出 `BusinessException` 并指定 HTTP 状态码（400 / 401 / 403 / 404），由 `GlobalExceptionHandler` 统一转换；参数校验失败返回 400，未处理异常返回 500。

## 前端架构

| 位置 | 职责 |
| --- | --- |
| `src/utils/request.js` | 统一 Axios 实例：自动附带 token、全局错误提示；收到 401 时清除登录态并派发 `yukiss:auth-expired` 事件。使用 `apiData(response)` 解包 `ApiResponse` |
| `src/composables/useUser.js` | 模块级单例状态：token、当前用户、登录弹窗、登录后跳转地址 |
| `src/router/index.js` | 路由表；`meta.requiresAuth` 的页面在未登录时跳转到 `/?login=1&redirect=...` |
| `src/utils/markdown.js` + `components/MarkdownContent.vue` | Markdown 渲染，输出经 DOMPurify 过滤 |
| `src/utils/media.js` | 将上传文件、头像等相对路径补全为后端地址 |
| `src/views/` | 页面组件 |

## 约定

- 新增或修改接口时，同步更新 [API.md](API.md)。
- 界面文案与接口错误信息使用中文。
- 前端请求统一走 `utils/request.js`，不要直接创建新的 Axios 实例。
- 前端测试与源文件放在同一目录，命名为 `*.test.js`。

## 测试

后端测试以单元测试为主：直接构造 `*ServiceImpl`，用 Mockito 模拟 mapper，并通过 `ThreadLocalUtil.set(...)` 设置当前用户（在 `@AfterEach` 中清理）。`BlogBackendApplicationTests` 会加载完整 Spring 上下文。

```bash
cd server
mvn test                                     # 全部
mvn test -Dtest=ArticleServiceImplTest       # 单个测试类
mvn test -Dtest=ArticleServiceImplTest#方法名  # 单个测试方法
```

```bash
cd web
npm test                                     # 全部
npx vitest run src/utils/markdown.test.js    # 单个文件
npm run test:watch                           # 监听模式
```

## 构建与发布

前端：

```bash
cd web
npm run build      # 产物输出到 web/dist
```

后端 `server/src/main/resources/static/` 中保存了一份已构建的前端产物，使后端可以直接托管页面。该目录**不会**随构建自动更新，如需更新，请将 `web/dist` 的内容手动复制过去。

注意：后端目前没有 SPA 路由回退，直接访问或刷新 `/community` 等前端路由会被登录拦截器处理，而不是返回 `index.html`。生产部署建议由 Nginx 等反向代理托管 `web/dist` 并配置 `try_files` 回退。

后端：

```bash
cd server
mvn clean package  # 产物为 target/blog-backend-0.0.1-SNAPSHOT.jar
java -jar target/blog-backend-0.0.1-SNAPSHOT.jar
```
