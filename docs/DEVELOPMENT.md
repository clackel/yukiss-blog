# 开发指南

本文档面向参与开发的人，说明配置项、整体架构、编码约定、测试与构建方式。接口细节见 [API.md](API.md)。

## 配置项

后端配置位于 `server/src/main/resources/application.yml`，所有值都可以通过下列变量覆盖。变量来源按优先级从高到低：

1. 系统 / 终端环境变量
2. `server/.env`（通过 `spring.config.import: optional:file:.env[.properties]` 读取，路径相对于启动目录，文件不存在时忽略；格式为 properties，即 `KEY=VALUE`，值不需要加引号）
3. `application.yml` 中的默认值

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
| `utils` | JWT、密码哈希、`ThreadLocalUtil`、`CurrentUser`（读取当前登录用户）、`ImageTypes`（按文件头识别图片） |

### 表结构管理

项目不使用迁移文件。`BlogSchemaInitializer` 与 `UserSchemaInitializer` 在启动时执行 `CREATE TABLE IF NOT EXISTS`，并通过幂等的“缺失才添加”方法（`addColumnIfMissing`、`addIndexIfMissing`）补充新字段和索引，新库和旧库走同一条路径。**修改表结构时请在这两个类中追加升级步骤**，不要修改已有建表语句来代替升级。可通过 `app.schema-initializer.enabled=false` 关闭。

- `addColumnIfMissing` 返回是否真的新增了字段，可据此做一次性数据回填（例如首次添加计数字段时回填点赞数和评论数）。
- 全文索引 `ft_article_title_content` 使用 ngram 分词。默认停用词表中有 `a`、`i` 等单字母，而 ngram 会丢弃“包含”停用词的词元，所以建索引时在同一连接上临时关闭了 `innodb_ft_enable_stopword`。

### 文章、草稿与标签

- `article.status` 为 `published` 或 `draft`。可见性统一在 service 中判断：草稿对作者以外的人一律表现为 404（`ArticleServiceImpl.getById`、评论与点赞服务中的 `requireVisibleArticle`），公开列表的 SQL 只查 `published`。
- 草稿首次发布时，`ArticleMapper.update` 把 `create_time` 改为发布时间（`create_time` 的赋值必须写在 `status` 之前）。
- 标签存放在 `article_tag`，查询时用 `GROUP_CONCAT` 拼成逗号分隔的 `tagNames`，再由 `Article.getTags()` 拆分，因此标签不允许包含逗号。保存时整体替换。

### 冗余计数

`article.like_count`、`comment_count`、`last_comment_time` 是冗余字段，供列表展示和排序使用，避免每行都做子查询。点赞、评论、删除评论之后，在同一事务内调用 `ArticleMapper.refreshLikeCount` / `refreshCommentStats` **从源表重新统计**，而不是加一减一，避免并发下出错。评论的点赞数仍为实时统计。

### 搜索

关键词按子串匹配标题、正文和作者昵称。`ArticleServiceImpl` 生成两个参数：

- `likeKeyword`：转义了 `%`、`_`、`\` 的关键词，保证按字面匹配；
- `fulltextKeyword`：把关键词按非字母数字字符拆成片段，生成 `+"片段1" +"片段2"`，先用全文索引缩小候选范围，再用 `LIKE` 精确判断。ngram 会忽略标点，单靠 `MATCH` 会把 “100%” 当成 “100”。任一片段不足 2 个字符（ngram 的最小单位）时为 `null`，只用 `LIKE`。

### 通知

`NotificationService.notify` / `retract` 由评论、点赞、关注的业务代码调用，自动忽略对自己内容的操作。取消点赞或关注时撤回对应通知；删除评论、文章、账号时同步删除相关通知（见 `ArticleCommentServiceImpl.deleteComment`、`ArticleMapper.deleteNotificationsByArticleId`、`UserServiceImpl.deleteAccount`）。前端每 60 秒轮询一次未读数。

### 上传与清理

- `FileUploadController` 根据文件头识别图片类型，忽略客户端上报的 Content-Type 和文件名，不接受 SVG。
- `UploadCleanupService` 每天 3:30（`app.upload.cleanup-cron`）删除不再被头像或文章（含草稿）引用、且上传超过 24 小时的文件。只处理上传接口生成的 UUID 文件名。

### 已注销用户

账号是软删除（`user.is_deleted`）。文章、评论、通知的查询在 SQL 中把已注销用户的昵称替换为“已注销用户”、头像置空，并返回 `authorDeleted` / `userDeleted` / `actorDeleted` 供前端去掉主页链接。

### 认证

- 登录成功后返回 JWT，前端将其**原样**放入 `Authorization` 请求头，不加 `Bearer` 前缀。
- `LoginInterceptor` 拦截除 `WebConfig` 中排除路径以外的所有请求，解析出的 claims 存入 `ThreadLocalUtil`，请求结束时清理。业务代码从 `ThreadLocalUtil.get()` 获取当前用户。
- JWT 中带有签发时间 `issuedAt`（秒）。拦截器每次都会通过 `UserMapper.isTokenActive` 查库确认：账号未注销，且 `password_updated_time` 不晚于签发时间。所以改密码和注销会立即让旧 token 失效。签发 token 统一走 `UserController.issueToken`。
- 登录失败限制（`LoginAttemptService`）和验证码限制（`EmailCodeService`）的计数都保存在内存中：重启后清空，多实例部署时需要换成 Redis。两者通过注入 `Clock` 来测试时间相关逻辑（见测试中的 `MutableClock`）。
- 登录限制按 `request.getRemoteAddr()` 统计 IP。`server.forward-headers-strategy: native` 让部署在本机反向代理之后时也能拿到真实 IP。
- 部分 GET 接口（文章列表、文章详情、评论列表、用户主页等）为公开读取：不要求登录，但若携带有效 JWT 会解析当前用户，用于计算 `likedByMe` 等字段。

### 响应与错误

- 所有接口返回 `ApiResponse`：`{ success, message, data }`，分页数据使用 `PageResult`。
- 可预期的失败抛出 `BusinessException` 并指定 HTTP 状态码（400 / 401 / 403 / 404 / 429），由 `GlobalExceptionHandler` 统一转换；参数校验失败返回 400，未处理异常返回 500。

## 前端架构

| 位置 | 职责 |
| --- | --- |
| `src/utils/request.js` | 统一 Axios 实例：自动附带 token、全局错误提示；收到 401 时清除登录态并派发 `yukiss:auth-expired` 事件。使用 `apiData(response)` 解包 `ApiResponse` |
| `src/composables/useUser.js` | 模块级单例状态：token、当前用户、登录弹窗、登录后跳转地址 |
| `src/composables/useNotifications.js` | 模块级单例的未读通知数；登录后由 `App.vue` 开始轮询，导航栏角标和通知页共享 |
| `src/router/index.js` | 路由表；`meta.requiresAuth` 的页面在未登录时跳转到 `/?login=1&redirect=...` |
| `src/utils/markdown.js` + `components/MarkdownContent.vue` | Markdown 渲染，输出经 DOMPurify 过滤；图片只允许外部 http(s) 地址和本站 `/uploads/` |
| `src/utils/media.js` | 将上传文件、头像等相对路径补全为后端地址 |
| `src/components/CommentSection.vue` + `CommentItem.vue` | 评论区：分页加载、发表、回复、编辑、删除、点赞；从通知跳转时定位到 `#comment-id` |
| `src/views/` | 页面组件 |

Element Plus 组件由 `unplugin-vue-components` 按需引入（见 `vite.config.js`），模板中直接使用 `<el-xxx>` 即可，无需手动注册；`ElMessage` 等函数式 API 仍从 `element-plus` 显式导入。样式在 `main.js` 中整体引入。

## 约定

- 新增或修改接口时，同步更新 [API.md](API.md)。
- 界面文案与接口错误信息使用中文。
- 后端读取当前用户统一用 `CurrentUser.idOrNull()` / `CurrentUser.requireId()`。
- 前端请求统一走 `utils/request.js`，不要直接创建新的 Axios 实例。
- 前端代码风格由 ESLint（`eslint.config.js`）和 Prettier（`.prettierrc.json`）约束，提交前运行 `npm run lint` 和 `npm run format`。
- 前端测试与源文件放在同一目录，命名为 `*.test.js`。

## 测试

后端测试以单元测试为主：直接构造 `*ServiceImpl`，用 Mockito 模拟 mapper，并通过 `ThreadLocalUtil.set(...)` 设置当前用户（在 `@AfterEach` 中清理）。`ApiWebLayerTest` 用 MockMvc 加上真实的拦截器、参数校验和全局异常处理，覆盖鉴权和 HTTP 状态码映射。`BlogBackendApplicationTests` 会加载完整 Spring 上下文。

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
npm run lint                                 # ESLint
npm run format                               # Prettier 格式化（format:check 只检查）
```

### 持续集成

`.github/workflows/ci.yml` 在推送到 `main` 和每个 Pull Request 时运行：

- 后端：启动一个 MySQL 8.4 服务，执行 `mvn -B test`
- 前端：`npm ci` 后依次执行 lint、格式检查、测试和构建

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
