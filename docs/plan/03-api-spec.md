# 三、API 完整规格 + 四、前端交互时序

## 3.1 鉴权相关 (user/)

登录方式：主路径为微信 OAuth 扫码。用户名/密码仅作 seed data 测试通道（无注册端点，DDL 预置账号）。

```
=== 登录 (seed data 测试通道) ===
POST   /api/v1/auth/login          { username, password }         → { userId, token, nickname }

=== Token ===
POST   /api/v1/auth/refresh        { token }                      → { token }

=== 用户 ===
GET    /api/v1/users/me                                           → UserVO
PUT    /api/v1/users/me             { nickname?, avatar?, email? } → UserVO
GET    /api/v1/users/{id}                                         → UserVO (public)
POST   /api/v1/users/me/bind-wx    { code }                       → UserVO  (绑定微信)

=== 微信回调（公开路径 /wx/portal/public，Phase 2+ 可选移植）===
GET    /wx/portal/public                       → 微信服务器验证（signature/timestamp/nonce/echostr）
POST   /wx/portal/public                       → 微信事件推送（SCAN/SUBSCRIBE 扫码事件）
GET    /wx/portal/public/callBack?code=         → OAuth2 授权回调（code 换 accessToken → 用户信息 → 签发 JWT）
```

## 3.2 服务器/频道相关 (server/)

```
=== Server ===
POST   /api/v1/servers                     { name, description? }           → ServerVO
GET    /api/v1/servers                                                        → [ServerVO]  (我的服务器列表)
GET    /api/v1/servers/discover?cursor=&pageSize=30                           → CursorPage<ServerVO>  (公开可加入的服务器，join_mode=FREE)
GET    /api/v1/servers/{serverId}                                             → ServerVO (含 categories + channels)
PUT    /api/v1/servers/{serverId}          { name?, description?, icon? }     → ServerVO
DELETE /api/v1/servers/{serverId}                                              → 204

=== Category ===
POST   /api/v1/servers/{serverId}/categories       { name, sortOrder? }    → CategoryVO
PUT    /api/v1/servers/{serverId}/categories/{id}   { name }                → CategoryVO
DELETE /api/v1/servers/{serverId}/categories/{id}                            → 204

=== Channel ===
POST   /api/v1/servers/{serverId}/channels         { categoryId?, name, type, topic?, ... } → ChannelVO
GET    /api/v1/servers/{serverId}/channels                                          → [CategoryVO{channels[]}]  (嵌套结构)
GET    /api/v1/servers/{serverId}/channels/{chanId}                                 → ChannelVO
PUT    /api/v1/servers/{serverId}/channels/{chanId}  { name?, topic?, ... }         → ChannelVO
DELETE /api/v1/servers/{serverId}/channels/{chanId}                                  → 204

=== Member ===
GET    /api/v1/servers/{serverId}/members?cursor=&pageSize=50    → CursorPage<MemberVO>
POST   /api/v1/servers/{serverId}/members                        → MemberVO  (加入服务器)
DELETE /api/v1/servers/{serverId}/members/{userId}               → 204        (踢出/离开)
PUT    /api/v1/servers/{serverId}/members/me/nickname  { nickname } → MemberVO
PUT    /api/v1/servers/{serverId}/members/transfer-ownership  { newOwnerId }    → 204  (转让 owner，仅当前 owner 可操作)

=== Role ===
POST   /api/v1/servers/{serverId}/roles            { name, color?, permissions, position? } → RoleVO
GET    /api/v1/servers/{serverId}/roles                                                       → [RoleVO]
PUT    /api/v1/servers/{serverId}/roles/{roleId}   { name?, color?, permissions? }            → RoleVO
DELETE /api/v1/servers/{serverId}/roles/{roleId}                                              → 204
POST   /api/v1/servers/{serverId}/members/{userId}/roles       { roleIds[] }                  → [MemberRoleVO]
DELETE /api/v1/servers/{serverId}/members/{userId}/roles/{roleId}                             → 204

=== Channel Permission ===
PUT    /api/v1/channels/{channelId}/permissions  { targetType, targetId, allowBits, denyBits } → ChannelPermissionVO
GET    /api/v1/channels/{channelId}/permissions                                               → [ChannelPermissionVO]
DELETE /api/v1/channels/{channelId}/permissions/{permId}                                       → 204

=== Emoji ===
POST   /api/v1/servers/{serverId}/emojis      { name, imageFile }   → EmojiVO
GET    /api/v1/servers/{serverId}/emojis                             → [EmojiVO]
DELETE /api/v1/servers/{serverId}/emojis/{emojiId}                   → 204

=== Invite ===
POST   /api/v1/servers/{serverId}/invites       { maxUses?, expireHours? }  → InviteVO
POST   /api/v1/servers/{serverId}/invites/join  { code }                     → ServerVO
```

## 3.3 消息相关 (message/)

```
=== Message ===
POST   /api/v1/channels/{channelId}/messages          { content, msgType, threadId?, replyMsgId?, fileIds[]? } → MessageVO
GET    /api/v1/channels/{channelId}/messages?cursor=&pageSize=50&threadId=                                     → CursorPage<MessageVO>
GET    /api/v1/channels/{channelId}/messages/{msgId}                                                           → MessageVO
PUT    /api/v1/channels/{channelId}/messages/{msgId}  { content }                                              → MessageVO
DELETE /api/v1/channels/{channelId}/messages/{msgId}                                                           → 204

=== Thread ===
POST   /api/v1/channels/{channelId}/threads    { rootMsgId, name? }                 → ThreadVO
GET    /api/v1/channels/{channelId}/threads?cursor=&pageSize=30                     → CursorPage<ThreadVO>  (按 last_active 降序)
GET    /api/v1/threads/{threadId}                                                    → ThreadVO
GET    /api/v1/threads/{threadId}/messages?cursor=&pageSize=50                       → CursorPage<MessageVO>
PUT    /api/v1/threads/{threadId}              { name?, status? }                     → ThreadVO

=== Reaction ===
POST   /api/v1/messages/{msgId}/reactions?emoji=👍                                   → { emoji, userId, totalCount }
DELETE /api/v1/messages/{msgId}/reactions?emoji=👍                                   → { emoji, userId, totalCount }
GET    /api/v1/messages/{msgId}/reactions                                             → [ { emoji, count, users[], reacted } ]

=== Unread ===
GET    /api/v1/servers/{serverId}/unread                                            → [ { channelId, unreadCount, lastReadMsgId } ]

=== Search ===
GET    /api/v1/servers/{serverId}/search?q=&channelId=&from=&to=&page=              → CursorPage<MessageVO>
```

## 3.4 文件相关 (file/)

```
POST   /api/v1/upload/presigned  { fileName, fileSize, mimeType }  → { uploadUrl, fileId }
POST   /api/v1/upload/confirm    { fileId }                        → FileVO
GET    /api/v1/files/{fileId}                                      → FileVO
GET    /api/v1/files/{fileId}/download                              → { downloadUrl }
```

---

## 四、前端交互核心时序

### 4.1 进入服务器完整流程

```
1. 用户点击左侧服务器图标
2. GET /api/v1/servers/{id} → 返回嵌套结构：
   {
     server: { id, name, icon },
     categories: [
       { id, name, channels: [{ id, name, type, topic }] },
       ...
     ],
     myRoles: [{ id, name, permissions }]
   }
3. 前端渲染频道侧边栏
4. 用户点击第一个频道

5. WebSocket 发送：
   {"type": 5, "data": {"channelId": 1}}     // SUBSCRIBE_CHANNEL
   {"type": 5, "data": {"channelId": 2}}     // 可批量订阅
   ...

6. HTTP 获取消息：
   GET /api/v1/channels/1/messages?cursor=&pageSize=50
   → { cursor, isLast, list: [MessageVO...] }

7. 此后该频道的消息通过 WebSocket 实时到达
   {"type": 20, "data": { channelId, message: {...} }}
```

### 4.2 前端 WebSocket 状态机

```
[未连接] → ws://host:8091/ → [已连接]
  │
  ▼ 收到 type=3 LOGIN_SUCCESS → [已认证]
  │
  ├── 进入服务器 → 批量 SUBSCRIBE_CHANNEL → [订阅中]
  │
  ├── 在输入框聚焦 → TYPING_START → (防抖 2s 后不发重复)
  ├── 失焦或发送 → TYPING_STOP
  │
  ├── 进入 Thread 视图 → SUBSCRIBE_THREAD
  ├── 离开 Thread 视图 → UNSUBSCRIBE_THREAD
  │
  └── 离开服务器 → UNSUBSCRIBE_CHANNEL × N
```
