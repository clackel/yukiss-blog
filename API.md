# Yukiss Blog 接口文档

本文档根据当前后端实现整理，适用于 `blog-backend`。

## 1. 基本约定

- 默认服务地址：`http://localhost:4000`
- 默认前端地址：`http://localhost:3000`
- 除文件上传外，请求体均使用 `application/json`
- 字符编码：UTF-8
- 认证方式：登录成功后，将返回的 JWT 原样放入请求头 `Authorization`

```http
Authorization: <token>
```

> 注意：当前实现不使用 `Bearer ` 前缀。

### 1.1 统一响应结构

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | boolean | 请求是否成功 |
| `message` | string | 结果或错误提示 |
| `data` | any/null | 返回数据；无返回数据时为 `null` |

### 1.2 HTTP 状态码

| 状态码 | 说明 |
| --- | --- |
| `200` | 请求成功 |
| `400` | 业务校验失败，错误原因见 `message` |
| `401` | 未提供有效 JWT，响应体可能为空 |
| `500` | 服务端异常 |

### 1.3 接口概览

| 模块 | 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- | --- |
| 用户 | POST | `/user/login` | 否 | 登录 |
| 用户 | POST | `/user/register` | 否 | 注册 |
| 用户 | GET | `/user/me` | 是 | 当前用户信息 |
| 用户 | PUT | `/user/profile` | 是 | 修改个人资料 |
| 用户 | POST | `/user/change-password` | 是 | 修改密码 |
| 用户 | POST | `/user/email/code` | 是 | 获取邮箱绑定验证码 |
| 用户 | POST | `/user/email/bind` | 是 | 绑定邮箱 |
| 用户 | POST | `/user/recover/code` | 否 | 获取账号找回验证码 |
| 用户 | POST | `/user/recover/account` | 否 | 找回用户名 |
| 用户 | POST | `/user/updateAvatar` | 是 | 更新头像地址 |
| 用户 | DELETE | `/user/delete` | 是 | 注销账号 |
| 文件 | POST | `/upload` | 是 | 上传头像图片 |
| 文章 | GET | `/articles` | 否 | 文章列表 |
| 文章 | GET | `/articles/{id}` | 否 | 文章详情 |
| 文章 | GET | `/articles/mine` | 是 | 当前用户的文章 |
| 文章 | POST | `/articles` | 是 | 发布文章 |
| 点赞 | POST | `/like` | 是 | 点赞/取消点赞文章 |
| 评论 | GET | `/comment/list` | 是 | 评论列表 |
| 评论 | POST | `/comment/add` | 是 | 发布评论或回复 |
| 评论 | POST | `/comment/{commentId}/like` | 是 | 点赞/取消点赞评论 |

> `/articles/mine` 虽然会被当前拦截器作为 GET 文章接口放行，但业务逻辑依赖登录用户。调用时仍必须传入有效 JWT，否则无法正常获得“我的文章”。

## 2. 数据模型

### 2.1 User

用户响应不会返回密码。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | integer | 用户 ID |
| `username` | string | 用户名 |
| `nickname` | string/null | 昵称 |
| `email` | string/null | 邮箱 |
| `emailVerified` | boolean | 邮箱是否已验证 |
| `avatar` | string/null | 头像 URL |
| `bio` | string/null | 个人简介 |
| `gender` | string/null | 性别 |
| `birthday` | string/null | 生日，建议传 ISO 8601 日期或日期时间 |
| `location` | string/null | 所在地 |
| `website` | string/null | 个人网站 |
| `role` | string/null | 用户角色 |
| `createTime` | string/null | 创建时间 |
| `lastLoginTime` | string/null | 最近登录时间 |
| `passwordUpdatedTime` | string/null | 密码更新时间 |
| `deletedTime` | string/null | 注销时间 |

### 2.2 Article

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | integer | 文章 ID |
| `authorId` | integer | 作者 ID |
| `title` | string | 标题 |
| `content` | string | 正文 |
| `createTime` | string/null | 创建时间 |
| `updateTime` | string/null | 更新时间 |
| `authorNickname` | string/null | 作者昵称 |
| `authorAvatar` | string/null | 作者头像 |
| `likeCount` | integer | 点赞数 |
| `commentCount` | integer | 评论数 |
| `likedByMe` | boolean/null | 当前用户是否点赞；匿名访问时通常为 `false` 或 `null` |

### 2.3 ArticleComment

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | integer | 评论 ID |
| `articleId` | integer | 文章 ID |
| `parentId` | integer/null | 父评论 ID；顶级评论为空 |
| `userId` | integer | 评论用户 ID，由后端根据 JWT 写入 |
| `content` | string | 评论内容 |
| `createTime` | string | 创建时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `nickname` | string/null | 评论者昵称 |
| `avatar` | string/null | 评论者头像 |
| `parentNickname` | string/null | 被回复用户昵称 |
| `likeCount` | integer | 点赞数 |
| `likedByMe` | boolean/null | 当前用户是否点赞 |

## 3. 用户接口

### 3.1 登录

`POST /user/login`

请求体：

```json
{
  "username": "yukiss",
  "password": "your-password"
}
```

成功响应中的 `data`：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "yukiss",
    "nickname": "Yukiss",
    "email": null,
    "emailVerified": false
  }
}
```

### 3.2 注册

`POST /user/register`

请求体：

```json
{
  "username": "yukiss",
  "password": "your-password",
  "nickname": "Yukiss",
  "email": "user@example.com"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `username` | 是 | 3～20 位，只能包含字母、数字和下划线 |
| `password` | 是 | 密码 |
| `nickname` | 否 | 最多 30 个字符；为空时使用用户名 |
| `email` | 否 | 邮箱；注册时仅保存，仍为未验证状态 |

成功时 `data` 为 `User`。

### 3.3 获取当前用户

`GET /user/me`

需要鉴权。成功时 `data` 为 `User`。

### 3.4 修改个人资料

`PUT /user/profile`

需要鉴权。请求体：

```json
{
  "nickname": "Yukiss",
  "bio": "个人简介",
  "gender": "保密",
  "birthday": "2000-01-01",
  "location": "Shanghai",
  "website": "https://example.com"
}
```

| 字段 | 限制 |
| --- | --- |
| `nickname` | 最多 30 个字符；为空时回退为用户名 |
| `bio` | 最多 300 个字符 |
| `gender` | 最多 20 个字符 |
| `birthday` | 日期或日期时间 |
| `location` | 最多 80 个字符 |
| `website` | 最多 180 个字符 |

成功时 `data` 为更新后的 `User`。

### 3.5 修改密码

`POST /user/change-password`

需要鉴权。请求体：

```json
{
  "oldPassword": "old-password",
  "newPassword": "new-password"
}
```

成功时 `data` 为 `null`。

### 3.6 获取邮箱绑定验证码

`POST /user/email/code`

需要鉴权。请求体：

```json
{
  "email": "user@example.com"
}
```

成功响应中的 `data`：

```json
{
  "message": "验证码已发送，请在 10 分钟内完成绑定",
  "devCode": "123456"
}
```

`devCode` 仅在当前验证码服务返回开发验证码时存在，客户端不应依赖该字段。

### 3.7 绑定邮箱

`POST /user/email/bind`

需要鉴权。请求体：

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

验证码有效期为 10 分钟。成功时 `data` 为更新后的 `User`。

### 3.8 获取账号找回验证码

`POST /user/recover/code`

请求体：

```json
{
  "email": "user@example.com"
}
```

成功时 `data` 包含提示信息，并可能包含开发环境验证码 `devCode`。

### 3.9 找回用户名

`POST /user/recover/account`

请求体：

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

成功响应中的 `data`：

```json
{
  "username": "yukiss"
}
```

### 3.10 更新头像地址

`POST /user/updateAvatar?avatarUrl=/uploads/example.jpg`

需要鉴权。`avatarUrl` 是查询参数，不是 JSON 请求体。

成功时 `data` 为更新后的 `User`。通常应先调用上传接口，再把其返回的 `url` 传给本接口。

### 3.11 注销账号

`DELETE /user/delete`

需要鉴权。请求体：

```json
{
  "password": "your-password"
}
```

成功后当前账号被注销，原登录状态应视为失效。

## 4. 文件接口

### 4.1 上传头像

`POST /upload`

需要鉴权，请求类型为 `multipart/form-data`。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | JPG、PNG、GIF 或 WebP，最大 5 MB |

示例：

```bash
curl -X POST "http://localhost:4000/upload" \
  -H "Authorization: <token>" \
  -F "file=@avatar.png"
```

成功响应中的 `data`：

```json
{
  "url": "/uploads/550e8400-e29b-41d4-a716-446655440000.png"
}
```

文件可通过 `GET /uploads/{文件名}` 公开访问。

## 5. 文章接口

### 5.1 获取文章列表

`GET /articles`

无需鉴权。若携带有效 JWT，返回的 `likedByMe` 可体现当前用户的点赞状态。成功时 `data` 为 `Article[]`。

### 5.2 获取文章详情

`GET /articles/{id}`

无需鉴权。

| 路径参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | integer | 文章 ID |

成功时 `data` 为 `Article`。

### 5.3 获取我的文章

`GET /articles/mine`

需要鉴权。成功时 `data` 为当前用户发布的 `Article[]`。

### 5.4 发布文章

`POST /articles`

需要鉴权。请求体：

```json
{
  "title": "文章标题",
  "content": "文章正文"
}
```

`authorId` 由后端根据 JWT 设置，不应由客户端传入。成功时 `data` 为 `null`。

## 6. 文章点赞接口

### 6.1 点赞或取消点赞文章

`POST /like?articleId=1`

需要鉴权。`articleId` 是查询参数。

成功响应中的 `data`：

```json
{
  "message": "点赞成功",
  "liked": true,
  "likeCount": 6
}
```

重复调用会切换点赞状态；取消点赞时 `liked` 为 `false`。

## 7. 评论接口

### 7.1 获取文章评论

`GET /comment/list?articleId=1`

当前实现需要鉴权。`articleId` 是查询参数。成功时 `data` 为 `ArticleComment[]`。

### 7.2 发布评论或回复

`POST /comment/add`

需要鉴权。发布顶级评论：

```json
{
  "articleId": 1,
  "content": "这是一条评论"
}
```

回复某条评论：

```json
{
  "articleId": 1,
  "parentId": 10,
  "content": "这是一条回复"
}
```

`userId` 由后端根据 JWT 设置。成功时 `data` 为 `null`。

### 7.3 点赞或取消点赞评论

`POST /comment/{commentId}/like`

需要鉴权。

| 路径参数 | 类型 | 说明 |
| --- | --- | --- |
| `commentId` | integer | 评论 ID |

成功响应中的 `data`：

```json
{
  "message": "点赞成功",
  "liked": true
}
```

重复调用会切换点赞状态。

## 8. 完整调用示例

登录后获取文章并点赞：

```bash
# 登录
curl -X POST "http://localhost:4000/user/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"yukiss","password":"your-password"}'

# 获取文章列表（公开接口）
curl "http://localhost:4000/articles"

# 点赞文章（将 TOKEN 替换为登录接口返回值）
curl -X POST "http://localhost:4000/like?articleId=1" \
  -H "Authorization: TOKEN"
```

## 9. 实现注意事项

- JWT 默认有效期为 12 小时，可通过环境变量 `BLOG_JWT_EXPIRATION_HOURS` 调整。
- 跨域默认允许 `http://localhost:3000` 和 `http://127.0.0.1:3000`。
- 后端上传请求总大小上限配置为 10 MB，但头像接口自身限制为 5 MB。
- 当前代码未使用 Bean Validation 注解；除文档明确列出的业务校验外，部分必填项、文章长度和评论长度由调用方保证。
- 点赞相关接口是“切换”操作，不具备 PUT 的幂等性；每调用一次都会在点赞与未点赞之间切换。
