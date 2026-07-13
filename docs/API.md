# API Reference

本文档列出 CampusCircle 的核心 REST API。接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

需要登录的接口通过请求头传入 Token：

```http
Authorization: Bearer <token>
```

## Auth

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| POST | `/api/auth/logout` | 用户退出 | 是 |

注册请求：

```json
{
  "username": "alice",
  "password": "123456",
  "nickname": "Alice"
}
```

登录请求：

```json
{
  "username": "alice",
  "password": "123456"
}
```

## User

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| GET | `/api/users/me` | 查询当前用户资料 | 是 |
| PUT | `/api/users/me` | 修改当前用户资料 | 是 |
| GET | `/api/users/{userId}` | 查询公开用户主页 | 否 |
| GET | `/api/users/{userId}/posts` | 查询用户发布的帖子 | 否 |
| GET | `/api/users/me/comments` | 查询我的评论 | 是 |

修改资料请求：

```json
{
  "nickname": "Alice",
  "avatarUrl": "https://example.com/avatar.png",
  "bio": "南京大学学生"
}
```

## Category

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| GET | `/api/categories` | 查询启用的帖子分类 | 否 |

## Post

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| POST | `/api/posts` | 发布帖子 | 是 |
| GET | `/api/posts` | 分页查询帖子 | 否 |
| GET | `/api/posts/{postId}` | 查询帖子详情 | 否 |
| PUT | `/api/posts/{postId}` | 编辑帖子 | 是 |
| DELETE | `/api/posts/{postId}` | 删除帖子 | 是 |
| GET | `/api/posts/hot` | 查询热门帖子 | 否 |

发帖请求：

```json
{
  "categoryId": 1,
  "title": "期末复习资料怎么整理？",
  "content": "想问问大家期末复习有什么方法。"
}
```

帖子列表常用查询参数：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码，默认 `1` |
| `size` | 每页数量，默认 `10` |
| `categoryId` | 分类 ID |
| `keyword` | 标题或内容关键词 |
| `sort` | 排序方式，`hot` 表示按热度排序 |

## Comment

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| POST | `/api/posts/{postId}/comments` | 发表评论 | 是 |
| GET | `/api/posts/{postId}/comments` | 分页查询帖子评论 | 否 |
| DELETE | `/api/comments/{commentId}` | 删除评论 | 是 |

评论请求：

```json
{
  "content": "我一般先整理错题，再刷历年卷。"
}
```

## Like

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| POST | `/api/posts/{postId}/like` | 点赞帖子 | 是 |
| DELETE | `/api/posts/{postId}/like` | 取消点赞 | 是 |
| GET | `/api/posts/{postId}/like` | 查询当前用户点赞状态 | 是 |

## Notice

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| GET | `/api/notices` | 分页查询站内通知 | 是 |
| GET | `/api/notices/unread-count` | 查询未读通知数 | 是 |
| PUT | `/api/notices/{noticeId}/read` | 标记单条通知已读 | 是 |
| PUT | `/api/notices/read-all` | 标记全部通知已读 | 是 |

通知列表查询参数：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码，默认 `1` |
| `size` | 每页数量，默认 `10` |
| `readStatus` | 读取状态，`0` 未读，`1` 已读 |

## Admin

后台管理接口仅管理员可访问。

| 方法 | 路径 | 说明 | 登录 |
| --- | --- | --- | --- |
| PUT | `/api/admin/posts/{postId}/hide` | 隐藏帖子 | 是 |
| PUT | `/api/admin/posts/{postId}/restore` | 恢复帖子 | 是 |
| PUT | `/api/admin/users/{userId}/disable` | 禁用用户 | 是 |
| PUT | `/api/admin/users/{userId}/enable` | 启用用户 | 是 |

## Error Codes

| code | 说明 |
| --- | --- |
| `0` | 请求成功 |
| `40000` | 请求参数错误 |
| `40001` | 用户名或密码错误 |
| `40100` | 未登录或 Token 无效 |
| `40300` | 无权限访问 |
| `40400` | 资源不存在 |
| `40900` | 资源状态冲突 |
| `40901` | 用户名已存在 |
| `50000` | 系统内部错误 |
