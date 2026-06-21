# 二、核心业务流程

## 2.1 用户完整旅程

```
1. 注册账号 (POST /api/v1/auth/register)
2. 登录获得 JWT (POST /api/v1/auth/login)
3. 创建服务器 (POST /api/v1/servers)
    → 自动成为 owner，获得 @everyone 角色
4. 创建分类 (POST /api/v1/servers/{id}/categories)
5. 创建频道 (POST /api/v1/servers/{id}/channels)
6. 邀请成员 (系统生成邀请链接或直接添加)
7. 发消息 (POST /api/v1/channels/{id}/messages)
8. 实时接收 (WebSocket 推送)
9. 创建 Thread (POST /api/v1/channels/{id}/threads)
10. 搜索历史 (GET /api/v1/servers/{id}/search?q=)
```

## 2.2 发送消息全链路（核心流程）

```
Client 发送消息
  │
  ▼
[Gateway / 拦截器] Token 校验 → 提取 uid
  │
  ▼
[MessageController.sendMsg()]
  │
  ▼
[MessageService.sendMsg()]
  ├── 1. 权限检查（Feign → server-service）
  │      ├── 用户是否是服务器成员？
  │      ├── 用户在目标频道是否有 SEND_MESSAGES 权限？
  │      └── Thread-only 频道是否带了 thread_id？
  ├── 2. 频控（@FrequencyControl，复用 MallChat 模式）
  ├── 3. 敏感词过滤（可复用 MallChat AC 自动机）
  ├── 4. 获取 MsgHandler（MsgHandlerFactory.getStrategy(msgType)）
  │      └── TextMsgHandler / ImageMsgHandler / FileMsgHandler / SystemMsgHandler
  ├── 5. handler.checkAndSaveMsg()
  │      ├── checkMsg() → 子类校验（如图片尺寸）
  │      ├── 构建 Message 实体 → messageDao.save()
  │      └── saveMsg() → 子类保存（如 file_attachment 关联）
  ├── 6. 发布 MessageSendEvent（Spring ApplicationEvent）
  │
  ▼
[MessageSendListener] @TransactionalEventListener
  │  发送 RocketMQ 消息到 SEND_MSG_TOPIC
  │
  ▼
[MsgSendConsumer] RocketMQ 消费
  ├── 1. 从 DB 加载完整 Message
  ├── 2. 构建 ChatMessageResp（含 fromUser、reactions、attachments）
  ├── 3. 如果是 Thread 消息 → push 给订阅该 thread 的用户
  │    如果是频道消息 → push 给订阅该 channel 的用户
  ├── 4. 更新 channel.last_msg_id, thread.message_count
  │
  ▼
[PushService.sendPushMsg()] → RocketMQ PUSH_TOPIC
  │
  ▼
[PushConsumer] (在 websocket-server)
  ├── 1. 查询订阅了目标 channel/thread 的在线用户
  ├── 2. 通过 WebSocketService.lookup(userId) 获取 Channel
  ├── 3. 写入 TextWebSocketFrame(JSON)
  │
  ▼
Client 浏览器收到 WS 帧 → 更新 UI
```

**与 MallChat 的关键差异**：
- MallChat 是"房间内所有人推送"，新平台是"只推送给订阅了该频道的人"
- 多了权限检查环节（MallChat 只检查是否是群成员）
- 多了 Thread 维度的推送路由

## 2.3 权限解析流程

```
用户请求操作（如：在 Channel#3 发送消息）
  │
  ▼
[PermissionService.checkPermission(serverId, userId, channelId, "SEND_MESSAGES")]
  │
  ├── 1. 查 user 在 server 中的所有角色
  │      SELECT r.* FROM role r
  │      JOIN member_role mr ON r.id = mr.role_id
  │      WHERE mr.user_id = ? AND r.server_id = ?
  │
  ├── 2. 计算基础权限 = 所有角色 permissions 的 OR 运算
  │      effectivePermissions = role1.permissions | role2.permissions | ...
  │
  ├── 3. 如果 effectivePermissions 包含 ADMINISTRATOR → 直接放行
  │
  ├── 4. 查 channel 的权限覆盖
  │      SELECT * FROM channel_permission
  │      WHERE channel_id = ?
  │        AND ( (target_type=0 AND target_id IN (用户角色ID列表))  ← 角色级覆盖
  │           OR (target_type=1 AND target_id = userId) )            ← 用户级覆盖
  │
  ├── 5. 应用覆盖规则
  │      对每个覆盖：effectivePermissions = (effectivePermissions | allow) & ~deny
  │      优先级：用户级覆盖 > 角色级覆盖 > 基础角色权限
  │
  └── 6. 检查目标权限位
         return (effectivePermissions & SEND_MESSAGES) != 0;
```

## 2.4 Thread 生命周期

```
[用户在频道消息上点"创建话题"]
  │
  ▼
POST /api/v1/channels/{id}/threads  { rootMsgId, name? }
  │
  ├── 1. 校验：频道非 thread-only 也可创建 thread
  ├── 2. 创建 Thread 记录（channel_id, root_msg_id, status=ACTIVE）
  ├── 3. 复制 root_msg 的内容作为 Thread 首条上下文
  ├── 4. 通知订阅者（WS type=34 THREAD_CREATE）
  │
  ▼
[其他用户在 Thread 内回复]
  POST /api/v1/channels/{id}/messages  { threadId, content }
  → 消息的 thread_id 非空 → 只在 Thread 视图显示，不在频道主时间线
  │
  ▼
[Thread 归档]
  PUT /api/v1/threads/{id}  { status: ARCHIVED }
  → 仍可查看，不可新回复 → 24h 后自动归档或手动
```

## 2.5 Reaction 流程

```
[用户对消息添加反应]
  │
  ▼
POST /api/v1/messages/{msgId}/reactions?emoji=👍
  │
  ├── 1. 权限检查：用户是否有 ADD_REACTIONS 权限
  ├── 2. 查是否已存在同 emoji 反应 → 存在则视为"取消"
  ├── 3. 插入/删除 reaction 记录
  ├── 4. 查询该消息所有 reaction，按 emoji 聚合
  │      SELECT emoji, COUNT(*) as count, GROUP_CONCAT(user_id) as users
  │      FROM reaction WHERE message_id = ? GROUP BY emoji
  │
  ├── 5. 推送 WS_REACTION_ADD/REMOVE 给频道订阅者
  │      payload: { messageId, channelId, userId, emoji, totalCount, users[] }
  │
  ▼
客户端收到后更新消息的 Reaction 展示行
```

## 2.6 文件上传流程

```
前端选择文件
  │
  ▼
POST /api/v1/upload/presigned  { fileName, fileSize, mimeType }
  │
  ├── 1. 生成 objectKey = "uploads/{userId}/{timestamp}_{uuid}_{fileName}"
  ├── 2. 调用 MinIO 生成预签名 PUT URL（5 分钟有效）
  ├── 3. 插入 file_attachment 记录（status=PENDING, message_id=null）
  ├── 4. 返回 { uploadUrl, fileId }
  │
  ▼
前端直接 PUT 文件到 MinIO（不经过后端，省带宽）
  │
  ▼
POST /api/v1/upload/confirm  { fileId }
  │
  ├── 1. 校验文件已存在于 MinIO
  ├── 2. 更新 file_attachment.status=UPLOADED
  ├── 3. 如果是图片，生成缩略图元数据（width/height）
  ├── 4. 返回文件元数据
  │
  ▼
前端发消息时带上 fileId
  POST /api/v1/channels/{id}/messages  { content, msgType: IMAGE, fileIds: [123] }
  → 后端关联 file_attachment.message_id = newMessageId
```
