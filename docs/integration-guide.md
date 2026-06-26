# 前端接入指南

面向前端/客户端的后端 API 对接文档。建议配合 Swagger UI 使用 — 本文档补充 Swagger 无法自动生成的 WebSocket 协议、文件上传流程、错误码等。

## 1. 基本信息

| 项目 | 值 |
|------|-----|
| Base URL（本地） | `http://localhost:8080` |
| Base URL（Docker） | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| WebSocket | `ws://localhost:8091` |
| Content-Type | `application/json`（文件上传除外） |
| 字符编码 | UTF-8 |

**端口汇总（本地开发直接访问微服务）：**

| 服务 | 端口 | 职责 |
|------|------|------|
| Gateway | 8080 | 统一入口（前端只访问这个） |
| User Service | 8081 | 登录鉴权、用户管理 |
| Server Service | 8082 | 服务器/频道/角色/成员/表情/邀请 |
| Message Service | 8083 | 消息/Reaction/搜索/话题 |
| File Service | 8084 | 文件上传/下载 |
| WebSocket | 8091 | 实时推送 |

> **推荐 Apifox 导入**：Apifox → 新建项目 → 导入 → OpenAPI URL → `http://localhost:8080/v3/api-docs`，即可获得所有 REST 端点的交互式文档。

## 2. 认证鉴权

### 2.1 登录

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

成功响应：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "token": "eyJhbG..."
  }
}
```

### 2.2 请求携带 Token

所有需要鉴权的请求在 Header 中携带：

```
Authorization: Bearer eyJhbG...
```

Gateway 自动校验 Token 并将用户 ID 注入 `X-Uid` Header 传递给下游微服务。

### 2.3 Token 刷新

Token 过期时间 5 天。过期前应调用刷新接口获取新 Token：

```
POST /api/v1/auth/refresh
Content-Type: application/json

{ "token": "eyJhbG..." }
```

### 2.4 公开端点

以下端点不需要携带 Token：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/register` | 用户注册 |

### 2.5 401/403 响应格式

鉴权失败由 Gateway 直接返回（不进入 Controller，因此 Swagger 不显示），格式为：

```json
{"code": 401, "message": "Missing Authorization header"}
{"code": 401, "message": "Invalid or expired token"}
{"code": 403, "message": "Internal endpoints not accessible externally"}
```

## 3. 标准响应格式

所有 REST 接口统一使用 `ApiResult<T>` 包装：

```json
{
  "success": true,
  "errCode": null,
  "errMsg": null,
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | Boolean | `true` 成功，`false` 失败 |
| `errCode` | Integer | 错误码，成功时为 `null` |
| `errMsg` | String | 错误描述，成功时为 `null` |
| `data` | Object | 业务数据，可能为 `null` |

### 3.1 成功示例

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "https://..."
  }
}
```

### 3.2 错误示例

```json
{
  "success": false,
  "errCode": 1001,
  "errMsg": "用户不存在"
}
```

### 3.3 HTTP 状态码约定

| HTTP Status | 场景 |
|-------------|------|
| 200 | 请求成功（正常返回） |
| 204 | 请求成功（无响应体，如删除消息、移除 Reaction） |
| 400 | 参数校验失败 或 业务逻辑错误（权限不足、数据不存在等） |
| 401 | 未携带 Token 或 Token 无效/过期（Gateway 返回） |
| 403 | 禁止访问（如外部请求访问内部端点） |
| 405 | 不支持的 HTTP 方法 |
| 429 | 请求频率过高被限流 |
| 500 | 服务器内部错误 |

### 3.4 游标分页

列表接口使用游标分页（`CursorPageBaseResp<T>`），不使用传统页码：

```json
{
  "success": true,
  "data": {
    "cursor": "12345",
    "isLast": false,
    "list": [ ... ]
  }
}
```

- `cursor`：下一页游标，传回给下次请求的 `?cursor=xxx` 参数
- `isLast`：`true` 表示最后一页
- 请求时不传 `cursor` 表示获取第一页

## 4. 错误码对照表

### 通用错误码（CommonErrorEnum）

| 错误码 | 含义 |
|--------|------|
| -1 | 系统内部错误，请稍后再试 |
| -2 | 参数校验失败 |
| -3 | 请求太频繁，请稍后再试 |
| -4 | 操作太频繁，请稍后再试 |

### 业务错误码（BusinessErrorEnum）

#### 用户模块 (1xxx)

| 错误码 | 含义 |
|--------|------|
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户名已存在 |
| 1004 | Token 无效或已过期 |
| 1005 | Token 已过期，请重新登录 |
| 1006 | 微信登录暂不可用 |
| 1007 | 该微信已被其他用户绑定 |

#### 服务器模块 (2xxx)

| 错误码 | 含义 |
|--------|------|
| 2001 | 服务器不存在 |
| 2002 | 无权限执行此操作 |
| 2003 | 不是该服务器成员 |
| 2004 | 角色不存在 |
| 2005 | 频道不存在 |
| 2006 | 分类不存在 |
| 2007 | 表情不存在 |

#### 消息模块 (3xxx)

| 错误码 | 含义 |
|--------|------|
| 3001 | 消息不存在 |
| 3002 | 不是消息作者 |
| 3003 | 话题不存在 |
| 3004 | 话题已归档，不可回复 |

#### 文件模块 (4xxx)

| 错误码 | 含义 |
|--------|------|
| 4001 | 文件不存在 |
| 4002 | 文件上传失败 |

#### 系统 (5xxx)

| 错误码 | 含义 |
|--------|------|
| 5001 | 操作过于频繁，请稍后再试 |
| 5002 | 请求过于频繁，请稍后再试 |

> 前端应根据 `errCode` 做中文提示。不做字符串匹配判断（错误消息文案可能变更）。

## 5. 文件上传流程

文件上传采用 **3 步流程**：客户端先获取预签名 URL，再直传到 MinIO，最后确认上传。

### 步骤 1：获取预签名上传 URL

```
POST /api/v1/upload/presigned
Content-Type: application/json

{
  "fileName": "photo.jpg",
  "fileSize": 2048000,
  "mimeType": "image/jpeg"
}
```

响应：
```json
{
  "success": true,
  "data": {
    "uploadUrl": "http://localhost:9004/community/1/uuid_photo.jpg?X-Amz-...",
    "fileId": 123
  }
}
```

### 步骤 2：PUT 直传到 MinIO

使用步骤 1 返回的 `uploadUrl`，直接 **PUT** 请求上传文件二进制。此请求不经过 Gateway，直接打到 MinIO。

```
PUT <uploadUrl>
Content-Type: image/jpeg

<binary file data>
```

> 预签名 URL 有效期 **10 分钟**，超时需重新获取。

### 步骤 3：确认上传完成

```
POST /api/v1/upload/confirm
Content-Type: application/json

{ "fileId": 123 }
```

响应返回完整 `FileVO`，状态变为 `UPLOADED`。

### 获取下载 URL

```
GET /api/v1/files/{fileId}/download
```

响应：
```json
{
  "success": true,
  "data": {
    "downloadUrl": "http://localhost:9004/community/1/uuid_photo.jpg?X-Amz-..."
  }
}
```

> 下载 URL 有效期 **1 小时**。

### 发送带文件的消息

上传完成后，在发送消息时将 `fileId` 传入 `fileIds` 数组：

```
POST /api/v1/channels/{channelId}/messages

{
  "content": "看看这张照片",
  "msgType": 1,
  "fileIds": [123]
}
```

## 6. WebSocket 协议

WebSocket 用于 **实时接收推送**（消息、成员变更、频道更新等）。消息发送通过 REST API，不通过 WebSocket。

### 6.1 连接

```
ws://localhost:8091/?token=<JWT_TOKEN>
```

Token 通过 URL 查询参数传递。连接建立后服务端推送 `LOGIN_SUCCESS`（type=3）表示鉴权成功。

### 6.2 消息格式

客户端与服务端通信统一使用 JSON 文本帧：

```json
{
  "type": 4,
  "data": "{ ... }"
}
```

- `type`：整数，对应下表中的类型码
- `data`：JSON 字符串，由具体类型的 handler 解析

### 6.3 客户端 → 服务端

| Type | 枚举名 | 说明 | data 格式 |
|------|--------|------|-----------|
| 2 | HEARTBEAT | 心跳 | 不需要 |
| 4 | SEND_MESSAGE | 发送消息 | `{"channelId":1,"content":"hello","msgType":1}` |
| 5 | SUBSCRIBE_CHANNEL | 订阅频道 | `{"channelId":1}` |
| 6 | UNSUBSCRIBE_CHANNEL | 取消订阅频道 | `{"channelId":1}` |
| 7 | SUBSCRIBE_THREAD | 订阅话题 | `{"threadId":5}` |
| 8 | UNSUBSCRIBE_THREAD | 取消订阅话题 | `{"threadId":5}` |
| 9 | TYPING_START | 输入中 | `{"channelId":1,"threadId":null}` |
| 10 | TYPING_STOP | 停止输入 | `{"channelId":1,"threadId":null}` |

### 6.4 服务端 → 客户端（推送）

| Type | 枚举名 | 说明 |
|------|--------|------|
| 3 | LOGIN_SUCCESS | 连接鉴权成功，返回用户信息 |
| 6 | INVALIDATE_TOKEN | Token 失效，前端应跳转登录 |
| 20 | MESSAGE_CREATE | 新消息推送（含消息全文） |
| 21 | MESSAGE_UPDATE | 消息编辑推送 |
| 22 | MESSAGE_DELETE | 消息删除推送 |
| 23 | REACTION_ADD | Reaction 添加推送 |
| 24 | REACTION_REMOVE | Reaction 移除推送 |
| 25 | TYPING_START_PUSH | 他人输入中 |
| 26 | TYPING_STOP_PUSH | 他人停止输入 |
| 30 | MEMBER_JOIN | 成员加入推送 |
| 31 | MEMBER_LEAVE | 成员离开推送 |
| 32 | MEMBER_KICK | 成员被踢推送 |
| 34 | THREAD_CREATE | 话题创建推送 |
| 40 | USER_ONLINE | 用户上线推送 |
| 41 | USER_OFFLINE | 用户离线推送 |
| 50 | CHANNEL_CREATE | 频道创建推送 |
| 51 | CHANNEL_UPDATE | 频道更新推送 |
| 52 | CHANNEL_DELETE | 频道删除推送 |
| 53 | SERVER_UPDATE | 服务器更新推送 |
| 99 | ERROR | 错误推送 |

### 6.5 订阅机制

WebSocket 连接建立后，**不会自动收到任何推送**。必须先发送订阅请求：

1. 用户进入频道 → 发送 `SUBSCRIBE_CHANNEL` (type=5)
2. 用户进入话题 → 发送 `SUBSCRIBE_THREAD` (type=7)
3. 用户离开频道/话题 → 发送对应的 `UNSUBSCRIBE_*` (type=6/8)

收到取消订阅或离开后，不再接收对应范围的推送。

### 6.6 心跳与超时

- 服务端 30 秒无读取 → **自动断开连接**（`IdleStateHandler` 30s reader idle）
- 建议客户端每 **15-20 秒** 发送一次心跳包 `{"type":2}` 保持连接
- 连接断开后需重新握手、鉴权、订阅

### 6.7 消息发送的正确路径

> 通过 REST API 发送消息（会经过频控、敏感词过滤、持久化），WebSocket 只用于接收推送。不要在 WebSocket 的 SEND_MESSAGE 中发送消息内容（该 type 仅传递元数据）。

## 7. 注意事项

### 7.1 游标分页

所有列表接口使用游标分页而非传统页码。前端维护 `cursor` 字符串，翻页时传回。`isLast=true` 表示已到最后一页。

### 7.2 消息发送用 REST，不用 WS

消息发送 → `POST /api/v1/channels/{channelId}/messages`（会走频控、敏感词过滤、持久化、MQ 推送）。

WebSocket `SEND_MESSAGE` type=4 传递的是瘦元数据，不是完整的消息发送通道。

### 7.3 文件上传前置

带文件的消息需要先完成文件上传（获得 `fileId`），再将 `fileId` 放入消息的 `fileIds` 数组。不能直接在消息 body 中传文件二进制。

### 7.4 表情上传用 multipart

自定义表情上传 (`POST /api/v1/servers/{serverId}/emojis`) 使用 `multipart/form-data`，不是 JSON。

### 7.5 CORS

Gateway 层已配置 CORS，前端无需代理。本地开发时直接访问 `http://localhost:8080`。

### 7.6 HTTP 方法

| 方法 | 场景 |
|------|------|
| GET | 查询（参数用 Query String） |
| POST | 创建（参数用 Request Body JSON） |
| PUT | 全量更新 |
| DELETE | 删除 |

### 7.7 响应状态码 204

DELETE 操作成功返回 204 No Content（无响应体）。前端应同时判断 200 和 204 为成功，或检查 `response.ok`。

### 7.8 Swagger 中看不到 401/403

这是正常的 — 鉴权在 Gateway WebFlux 过滤器层，不经过 Spring MVC Controller，springdoc 不会自动生成。参考第 2.5 节的手动文档。
