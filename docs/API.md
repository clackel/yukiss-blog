# Yukiss Blog API

本文档对应当前 `server` 实现。项目整体说明见 [README](../README.md)，开发约定见 [开发指南](DEVELOPMENT.md)。

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
| `404` | 文章、评论或通知不存在（他人的草稿也返回 404） |
| `429` | 请求过于频繁（登录失败次数过多、验证码发送过快） |
| `500` | 未处理的服务端异常 |

公开读取接口会尝试解析可选 JWT。携带有效 JWT 时，文章与评论中的 `likedByMe` 会反映当前用户状态；不携带或携带无效 JWT 时按匿名访问处理。

JWT 失效规则：除签名错误和过期（默认 12 小时）外，以下情况也会使已签发的 JWT 立即失效，受保护接口返回 `401`：

- 修改密码：修改前签发的所有 JWT 失效（当前设备会在响应中拿到新 token）
- 注销账号：该账号的所有 JWT 失效

已注销用户：文章、评论、通知中涉及已注销用户时，昵称统一显示为 `已注销用户`，头像为 `null`，并通过 `authorDeleted` / `userDeleted` / `actorDeleted` 标记；其公开主页返回 404。

## 数据结构

### Article

```json
{
  "id": 12,
  "authorId": 3,
  "title": "使用 Markdown 写一篇文章",
  "content": "# 标题\n\n正文",
  "status": "published",
  "tags": ["Vue", "前端"],
  "createTime": "2026-07-26T16:00:00",
  "updateTime": "2026-07-26T16:10:00",
  "lastCommentTime": "2026-07-26T17:30:00",
  "authorNickname": "Yukiss",
  "authorAvatar": "/uploads/avatar.png",
  "authorDeleted": false,
  "likeCount": 8,
  "commentCount": 3,
  "likedByMe": true
}
```

- 标题 1–80 个字符；正文最长 50,000 个字符，发布时不能为空，草稿可以为空
- `status`：`published`（已发布）或 `draft`（草稿，仅作者本人可见）
- `tags`：最多 5 个，每个 1–20 个字符，不能包含逗号；保存时会去掉开头的 `#` 并按不区分大小写去重
- `createTime` 为发布时间：草稿首次发布时会更新为发布那一刻
- `commentCount` 包含回复

### ArticleComment

```json
{
  "id": 21,
  "articleId": 12,
  "parentId": null,
  "userId": 4,
  "content": "写得很好",
  "createTime": "2026-07-26 16:20:00",
  "updateTime": null,
  "nickname": "朋友",
  "avatar": null,
  "userDeleted": false,
  "parentNickname": null,
  "likeCount": 2,
  "likedByMe": false,
  "replies": []
}
```

- 评论最长 500 个字符，当前只允许回复顶级评论
- `updateTime` 只有编辑过的评论才有值
- `replies`：评论列表接口中，顶级评论携带它的全部回复（按时间正序）；回复本身的 `replies` 为空

### Notification

```json
{
  "id": 31,
  "type": "reply",
  "actorId": 4,
  "actorNickname": "朋友",
  "actorAvatar": null,
  "actorDeleted": false,
  "articleId": 12,
  "articleTitle": "使用 Markdown 写一篇文章",
  "commentId": 22,
  "commentContent": "这是一条回复",
  "read": false,
  "createTime": "2026-07-26T17:00:00"
}
```

| `type` | 触发时机 | 接收人 |
| --- | --- | --- |
| `comment` | 有人评论文章（含回复） | 文章作者 |
| `reply` | 有人回复评论 | 被回复的评论作者 |
| `article_like` | 有人点赞文章 | 文章作者 |
| `comment_like` | 有人点赞评论 | 评论作者 |
| `follow` | 有人关注 | 被关注的用户 |

- 对自己的内容操作不产生通知；回复文章作者本人的评论时，作者只收到一条 `reply`
- 取消点赞、取消关注会撤回对应通知；评论或文章被删除时，相关通知一并删除
- `follow` 类型的 `articleId`、`commentId` 为 `null`

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

失败次数限制：同一用户名在同一 IP 下，15 分钟内连续失败 5 次后锁定 15 分钟。锁定期间即使密码正确也返回 `429`，登录成功后计数清零。

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

响应中的用户对象包含 `followerCount`（粉丝数）和 `followingCount`（关注数）。

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

响应 `data`：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

修改成功后旧 JWT 全部失效，客户端需用返回的新 token 替换本地保存的 token。

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

验证码规则（绑定邮箱与找回用户名相同）：

- 有效期 10 分钟，验证成功后立即作废
- 同一邮箱同一用途 60 秒内只能发送一次，过快返回 `429`
- 输错 5 次后验证码作废，需要重新获取

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

注销后该账号的关注关系和收到的通知会被删除；已发布的文章和评论保留，作者显示为“已注销用户”。

## 公开主页与关注接口

### 查看用户主页

`GET /users/{id}`，公开。

返回公开资料，不包含登录名、邮箱等私密字段：

```json
{
  "id": 3,
  "nickname": "朋友",
  "avatar": "/uploads/avatar.png",
  "bio": "个人简介",
  "location": "Shanghai",
  "website": "https://example.com",
  "createTime": "2026-07-26T16:00:00",
  "followerCount": 12,
  "followingCount": 8,
  "followedByMe": true,
  "ownProfile": false
}
```

### 关注用户

`POST /users/{id}/follow`

### 取消关注

`DELETE /users/{id}/follow`

两个接口都返回更新后的公开主页数据。不能关注自己；重复关注不会生成重复关系，也不会重复通知。

## 文章接口

### 兼容文章列表

`GET /articles`，公开。

返回按发布时间倒序排列的已发布 `Article[]`。此接口为原有调用保留；新列表页应使用分页接口。

### 分页搜索

`GET /articles/page`，公开。只返回已发布的文章。

查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `page` | `1` | 页码，从 1 开始 |
| `pageSize` | `10` | 每页数量，1–50 |
| `keyword` | 空 | 搜索标题、正文和作者昵称，最长 80 字 |
| `tag` | 空 | 只返回带有该标签的文章（精确匹配，开头的 `#` 会被忽略） |
| `sort` | `published` | `published`、`commented`、`likes` 或 `comments` |

`keyword` 按子串匹配，`%`、`_` 等字符按字面匹配。标题和正文借助 ngram 全文索引加速，结果与子串匹配一致；作者昵称匹配不包含已注销用户。

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

- `published`：按文章发布时间倒序
- `commented`：按最新评论时间倒序；无评论的文章排在后面
- `likes`：按点赞数倒序，数量相同时按发布时间倒序
- `comments`：按评论数倒序，数量相同时按发布时间倒序

旧参数 `latest` 和 `popular` 仍兼容，分别等同于 `published` 和 `likes`。

### 热门标签

`GET /articles/tags`，公开。

返回已发布文章中使用最多的 20 个标签：

```json
[
  { "name": "Vue", "articleCount": 6 },
  { "name": "前端", "articleCount": 3 }
]
```

### 我的文章

`GET /articles/mine`

返回当前用户的全部 `Article[]`，包括草稿；草稿排在最前，其余按发布时间倒序。

### 指定用户的文章

`GET /articles/user/{userId}`，公开。

返回该用户按发布时间倒序排列的已发布 `Article[]`（即使是本人访问也不含草稿）。

### 文章详情

`GET /articles/{id}`，公开。

草稿只有作者本人能获取，其他人（包括未登录访客）得到 `404`。

### 发布文章

`POST /articles`

```json
{
  "title": "文章标题",
  "content": "# Markdown 正文",
  "status": "published",
  "tags": ["Vue", "前端"]
}
```

`status` 默认为 `published`；传 `draft` 保存为草稿。`tags` 可省略。成功时返回创建后的 `Article`。

### 编辑文章

`PUT /articles/{id}`

请求体与发布文章相同，仅作者本人可操作，`tags` 会整体替换。把草稿改为 `published` 即为发布。成功时返回更新后的 `Article`。

### 删除文章

`DELETE /articles/{id}`

仅作者本人可操作。文章的评论、点赞、评论点赞、标签和相关通知会在同一事务内删除。

## 点赞接口

### 文章点赞切换

`POST /like?articleId=12`

返回 `LikeResult`。重复调用会在点赞和取消点赞之间切换。草稿不能点赞（`400`）。

### 评论点赞切换

`POST /comment/{commentId}/like`

返回 `LikeResult`。

## 评论接口

### 评论列表

`GET /comment/list?articleId=12&page=1&pageSize=10`，公开。

按发布时间正序分页返回顶级评论，每条顶级评论的 `replies` 中带有它的全部回复。`page` 默认 1，`pageSize` 默认 10、最大 50。

```json
{
  "items": [{ "id": 21, "content": "写得很好", "replies": [] }],
  "total": 12,
  "page": 1,
  "pageSize": 10,
  "totalPages": 2
}
```

`total` 为顶级评论数；文章的评论总数（含回复）见 `Article.commentCount`。草稿文章对作者以外的人返回 `404`。

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

成功时返回创建后的 `ArticleComment`。父评论必须存在、属于同一篇文章且自身不是回复。草稿不能评论（作者本人 `400`，其他人 `404`）。

### 编辑评论

`PUT /comment/{commentId}`

```json
{
  "content": "修改后的内容"
}
```

仅评论作者本人可操作（否则 `403`）。成功时返回更新后的 `ArticleComment`，`updateTime` 为编辑时间。

### 删除评论

`DELETE /comment/{commentId}`

评论作者本人或文章作者可操作（否则 `403`）。删除顶级评论时，它的回复、相关点赞和通知会一起删除。

响应 `data`：

```json
{
  "deletedCount": 3
}
```

## 通知接口

均需要登录，只能读取和操作自己的通知。

### 通知列表

`GET /notifications?page=1&pageSize=20`

按时间倒序返回 `Notification` 分页数据（结构同文章分页），`pageSize` 默认 20、最大 50。

### 未读数量

`GET /notifications/unread-count`

```json
{
  "count": 5
}
```

### 标记已读

`POST /notifications/{id}/read`：标记单条为已读，不是自己的通知返回 `404`。

`POST /notifications/read-all`：全部标记为已读。

## 文件上传

`POST /upload`

- 请求类型：`multipart/form-data`
- 字段名：`file`
- 支持 JPG、PNG、GIF、WebP（按文件内容的文件头识别，不信任客户端上报的类型和文件名；扩展名由服务端决定）
- 最大 5 MB

响应 `data`：

```json
{
  "url": "/uploads/550e8400-e29b-41d4-a716-446655440000.png"
}
```

上传文件可通过 `GET /uploads/{文件名}` 访问。头像和文章插图共用此接口，文章中以 `![说明](/uploads/文件名)` 引用。

上传后超过 24 小时、且未被任何头像或文章引用的文件会被每日清理任务删除。
