# Yukiss Blog API

本文档对应当前 `blog-backend` 实现。

## 基本约定

- 默认地址：`http://localhost:4000`
- JSON 请求使用 `Content-Type: application/json`
- 登录后把 JWT 原样放入 `Authorization` 请求头，不添加 `Bearer` 前缀
- 除注明“公开”的接口外，其余接口均需要登录

```http
Authorization: <token>
```

统一响应：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

错误状态：

| 状态码 | 含义 |
| --- | --- |
| `400` | 参数或业务校验失败 |
| `401` | 未登录或 JWT 无效 |
| `403` | 已登录但无权操作该资源 |
| `404` | 文章或评论不存在 |
| `500` | 未处理的服务端异常 |

公开读取接口会尝试解析可选 JWT。携带有效 JWT 时，文章与评论中的 `likedByMe` 会反映当前用户状态；不携带或携带无效 JWT 时按匿名访问处理。

## 数据结构

### Article

```json
{
  "id": 12,
  "authorId": 3,
  "title": "使用 Markdown 写一篇文章",
  "content": "# 标题\n\n正文",
  "createTime": "2026-07-26T16:00:00",
  "updateTime": "2026-07-26T16:10:00",
  "authorNickname": "Yukiss",
  "authorAvatar": "/uploads/avatar.png",
  "likeCount": 8,
  "commentCount": 3,
  "likedByMe": true
}
```

文章标题为 1–80 个字符，Markdown 正文为 1–50,000 个字符。

### ArticleComment

```json
{
  "id": 21,
  "articleId": 12,
  "parentId": null,
  "userId": 4,
  "content": "写得很好",
  "createTime": "2026-07-26 16:20:00",
  "nickname": "朋友",
  "avatar": null,
  "parentNickname": null,
  "likeCount": 2,
  "likedByMe": false
}
```

评论最长 500 个字符。当前只允许回复顶级评论。

### LikeResult

```json
{
  "message": "点赞成功",
  "liked": true,
  "likeCount": 9
}
```

## 用户接口

### 登录

`POST /user/login`，公开。

```json
{
  "username": "yukiss",
  "password": "password123"
}
```

响应 `data`：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "yukiss",
    "nickname": "Yukiss"
  }
}
```

### 注册

`POST /user/register`，公开。

```json
{
  "username": "friend_01",
  "password": "password123",
  "nickname": "朋友",
  "email": "friend@example.com"
}
```

- 用户名：3–20 位字母、数字或下划线
- 密码：8–72 位，同时包含字母和数字
- 昵称和邮箱可选

### 当前用户

`GET /user/me`

### 修改资料

`PUT /user/profile`

```json
{
  "nickname": "朋友",
  "bio": "个人简介",
  "gender": "private",
  "birthday": "2000-01-01",
  "location": "Shanghai",
  "website": "https://example.com"
}
```

### 修改密码

`POST /user/change-password`

```json
{
  "oldPassword": "password123",
  "newPassword": "newPassword456"
}
```

### 邮箱绑定

获取验证码：

`POST /user/email/code`

```json
{
  "email": "friend@example.com"
}
```

绑定邮箱：

`POST /user/email/bind`

```json
{
  "email": "friend@example.com",
  "code": "123456"
}
```

本地开发默认会在响应 `data.devCode` 中返回验证码。

### 找回用户名

获取验证码：

`POST /user/recover/code`，公开。

找回账号：

`POST /user/recover/account`，公开。

```json
{
  "email": "friend@example.com",
  "code": "123456"
}
```

成功时返回 `data.username`。

### 更新头像

`POST /user/updateAvatar?avatarUrl=/uploads/example.jpg`

通常先调用上传接口，再把返回 URL 传给本接口。

### 注销账号

`DELETE /user/delete`

```json
{
  "password": "password123"
}
```

## 文章接口

### 兼容文章列表

`GET /articles`，公开。

返回按发布时间倒序排列的 `Article[]`。此接口为原有调用保留；新列表页应使用分页接口。

### 分页搜索

`GET /articles/page`，公开。

查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `page` | `1` | 页码，从 1 开始 |
| `pageSize` | `10` | 每页数量，1–50 |
| `keyword` | 空 | 搜索标题、正文和作者昵称，最长 80 字 |
| `sort` | `latest` | `latest` 或 `popular` |

响应 `data`：

```json
{
  "items": [],
  "total": 28,
  "page": 1,
  "pageSize": 10,
  "totalPages": 3
}
```

热门排序依次比较点赞数、评论数和发布时间。

### 我的文章

`GET /articles/mine`

返回当前用户发布的 `Article[]`。

### 文章详情

`GET /articles/{id}`，公开。

### 发布文章

`POST /articles`

```json
{
  "title": "文章标题",
  "content": "# Markdown 正文"
}
```

成功时返回创建后的 `Article`。

### 编辑文章

`PUT /articles/{id}`

请求体与发布文章相同，仅作者本人可操作。成功时返回更新后的 `Article`。

### 删除文章

`DELETE /articles/{id}`

仅作者本人可操作。文章点赞、评论点赞和评论会在同一事务内删除。

## 点赞接口

### 文章点赞切换

`POST /like?articleId=12`

返回 `LikeResult`。重复调用会在点赞和取消点赞之间切换。

### 评论点赞切换

`POST /comment/{commentId}/like`

返回 `LikeResult`。

## 评论接口

### 评论列表

`GET /comment/list?articleId=12`，公开。

返回按发布时间升序排列的 `ArticleComment[]`。

### 发布评论

`POST /comment/add`

顶级评论：

```json
{
  "articleId": 12,
  "content": "这是一条评论"
}
```

回复顶级评论：

```json
{
  "articleId": 12,
  "parentId": 21,
  "content": "这是一条回复"
}
```

成功时返回创建后的 `ArticleComment`。父评论必须存在、属于同一篇文章且自身不是回复。

## 文件上传

`POST /upload`

- 请求类型：`multipart/form-data`
- 字段名：`file`
- 支持 JPG、PNG、GIF、WebP
- 最大 5 MB

响应 `data`：

```json
{
  "url": "/uploads/550e8400-e29b-41d4-a716-446655440000.png"
}
```

上传文件可通过 `GET /uploads/{文件名}` 访问。
