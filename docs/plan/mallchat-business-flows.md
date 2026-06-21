# MallChat 主要业务流程描述

> 基于项目实际代码的精准描述，作为社区平台设计的参考基线。

---

## 一、微信扫码登录

MallChat 没有用户名/密码注册系统，唯一登录方式是微信公众平台 OAuth 扫码。

### 1.1 登录四阶段

**阶段 1：前端请求登录二维码**

1. 浏览器建立 WebSocket 连接到 `ws://host:8090/`
2. Netty 管道完成握手：`HttpHeadersHandler` 提取 URL 参数中的 token（如有）和客户端 IP → `WebSocketServerProtocolHandler` 升级协议 → `NettyWebSocketServerHandler` 就绪
3. 前端发送 `{"type":1}`（LOGIN 请求）
4. 服务端 `WebSocketServiceImpl.handleLoginReq()` 生成一个整数 `loginCode`（Redis 原子自增），调用微信 API `qrCodeCreateTmpTicket(code, expireSeconds)` 换取二维码 ticket URL
5. 服务端将 `loginCode → Netty Channel` 的映射存入本地 `Caffeine` 缓存（1 小时过期），同时推送 `{"type":1, "data":{"loginUrl":"https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=..."}}` 给前端
6. 前端渲染二维码

**阶段 2：用户扫码**

7. 用户用微信扫描二维码
8. 微信服务器向 `WxPortalController.post()` 发送 SCAN 事件（或 SUBSCRIBE 事件，首次关注时 key 带 `qrscene_` 前缀）
9. `WxMpMessageRouter` 将事件路由到 `ScanHandler`（或 `SubscribeHandler`），最终调用 `WxMsgService.scan()`
10. `scan()` 执行：
    - 提取 `openId` 和 `loginCode`
    - 查数据库 `user` 表：SELECT by openId
    - **已注册且有头像** → 直接发送 `LoginMessageDTO(uid, loginCode)` 到 RocketMQ `user_login_send_msg` Topic（跳过授权，直接登录）
    - **未注册** → 创建新 User 记录（仅含 openId），存 `openid:{openId} → loginCode` 到 Redis（60 分钟 TTL）
    - 向微信回复一条包含 OAuth 授权链接的文本消息，提示用户点击授权

11. 发送 `ScanSuccessMessageDTO(loginCode)` 到 RocketMQ `user_scan_send_msg` Topic

**阶段 3：通知前端扫码成功**

12. `ScanSuccessConsumer` 消费消息（**广播模式**，每个服务实例都收到）
13. 调用 `WebSocketServiceImpl.scanSuccess(loginCode)`：在本实例的 `WAIT_LOGIN_MAP` 中查找 loginCode 对应的 Netty Channel
14. 命中则推送 `{"type":2}`（LOGIN_SCAN_SUCCESS）给前端，前端显示"已扫码，请在微信中点击授权"

**阶段 4：用户授权并完成登录**

15. 用户在微信中点击授权链接，跳转到 `https://open.weixin.qq.com/connect/oauth2/authorize?appid=...&redirect_uri=.../wx/portal/public/callBack&response_type=code&scope=snsapi_userinfo`
16. 用户确认授权后，微信重定向到 `WxPortalController.callBack(?code=...)`
17. `callBack()` 用 code 换取 `accessToken`，再换取 `WxOAuth2UserInfo`（含 openId、nickname、headImgUrl、sex）
18. 调用 `WxMsgService.authorize(userInfo)`：
    - 通过 openId 找到用户，更新昵称/头像/性别（`fillUserInfo()`，带重试处理昵称重复）
    - 从 Redis 取出 `openid:{openId} → loginCode`
    - 发送 `LoginMessageDTO(uid, loginCode)` 到 RocketMQ `user_login_send_msg` Topic

19. `MsgLoginConsumer` 消费消息（**广播模式**）
20. 调用 `WebSocketServiceImpl.scanLoginSuccess(loginCode, uid)`：
    - 在本实例 `WAIT_LOGIN_MAP` 中查找 loginCode 对应的 Channel
    - 调用 `LoginServiceImpl.login(uid)` 签发 JWT（5 天有效期，存入 Redis `userToken:uid_{uid}`）
    - 将 uid 绑定到 Netty Channel（`ONLINE_UID_MAP` 注册）
    - 推送 `{"type":3, "data":{"uid":..., "avatar":..., "token":"...", "name":"...", "power":...}}`（LOGIN_SUCCESS）给前端
    - 发布 `UserOnlineEvent`

### 1.2 关键设计决策

| 决策 | 说明 |
|------|------|
| RocketMQ 广播模式 | 扫码请求和授权回调可能落在不同实例，广播确保所有实例都收到消息各自检查本地 Channel |
| Caffeine 本地缓存 | `WAIT_LOGIN_MAP` 存 loginCode→Channel，不跨实例共享 |
| Token 单设备 | `login()` 复用已有 Redis Token，同 uid 新登录不签发新 Token |
| Token 自动续期 | `LoginServiceImpl.renewalTokenIfNecessary()` 异步检查，剩余 < 2 天则延长至 5 天 |
| HTTP 鉴权 | `TokenInterceptor` 从 `Authorization: Bearer <token>` 提取 JWT → 查 Redis 验证 → 设置 `request.uid` |
| 公开路径 | URI 中第三段为 `public` 的路径（如 `/capi/xxx/public/yyy`）跳过鉴权 |

---

## 二、好友管理

### 2.1 发送好友申请

1. `POST /capi/user/friend/apply` → `FriendController.apply()`
2. `FriendServiceImpl.apply()`（`@RedissonLock` 锁当前用户）：
    - 检查是否已是好友：查 `user_friend` 表（双向记录 `uid1↔uid2`）
    - 检查自己是否已有待审批申请：查 `user_apply` 表
    - 检查对方是否已向自己申请：如果存在 → **自动批准**（跳过审批流程，直接调 `applyApprove()`）
    - 否则创建 `UserApply` 记录（type=1 申请，status=1 待审批）
3. 发布 `UserApplyEvent`
4. `UserApplyListener.notifyFriend()`（异步）：通过 `PushService` 向被申请人推送 WebSocket 通知（含未读申请数 badge）

### 2.2 审批好友申请

1. `PUT /capi/user/friend/apply` → `FriendController.applyApprove()`
2. `FriendServiceImpl.applyApprove()`（`@Transactional` + `@RedissonLock`）：
    - 校验申请记录存在且状态为待审批
    - `userApplyDao.agree()`：更新申请状态为已同意
    - `createFriend()`：创建双向 `UserFriend` 记录（uid1↔uid2 两条）
    - `roomService.createFriendRoom()`：创建单聊房间
        - 生成 roomKey = `{minUid},{maxUid}`（两 uid 排序后拼接）
        - 如果 RoomFriend 已存在但被禁用 → `restoreRoomIfNeed()` 重新启用
        - 否则创建 Room + RoomFriend 记录
    - `chatService.sendMsg()`：发送系统消息"我们已经成为好友了，开始聊天吧"

### 2.3 删除好友

1. `DELETE /capi/user/friend` → `FriendController.delete()`
2. `FriendServiceImpl.deleteFriend()`（`@Transactional`）：
    - 删除 `UserFriend` 记录
    - `roomService.disableFriendRoom()`：禁用单聊房间（软删除，保留消息历史）

---

## 三、群组管理

### 3.1 创建群组

1. `POST /capi/room/group` → `RoomController.addGroup()`
2. `RoomAppServiceImpl.addGroup()`（`@Transactional`）：
    - 限制：每用户最多创建 **1 个**群组（查 `group_member` 中 role=LEADER 的记录数）
    - `roomService.createGroupRoom()`：
        - 创建 `Room` 记录（type=GROUP）
        - 创建 `RoomGroup` 记录（name + avatar）
        - 创建者设为 `GroupRoleEnum.LEADER`
    - `RoomAdapter.buildGroupMemberBatch()`：将初始邀请的成员列表转换为 `GroupMember` 实体
    - `groupMemberDao.saveBatch()`：批量插入群成员
3. 发布 `GroupMemberAddEvent`
4. `GroupMemberAddListener`（两个 `@Async` 方法）：
    - `sendAddMsg()`：发送系统消息"XX 邀请 XX 加入群聊"
    - `sendChangePush()`：向所有群成员推送 WebSocket 成员变更通知 + 清除群成员缓存

### 3.2 邀请成员入群

1. `POST /capi/room/group/member` → `RoomController.addMember()`
2. `RoomAppServiceImpl.addMember()`（`@RedissonLock` + `@Transactional`）：
    - 校验房间存在且非热门群（全员群）
    - 校验操作者是当前群成员
    - 过滤出非已有的新成员 → 构建 `GroupMember` 列表 → 批量插入
    - 发布 `GroupMemberAddEvent`

### 3.3 移除成员

1. `DELETE /capi/room/group/member` → `RoomAppServiceImpl.delMember()`
2. 权限校验：
    - 不能移除群主
    - 管理员只能由群主移除
    - 操作者必须是群主/管理员/超管
3. 物理删除 `GroupMember` → WebSocket 通知 → 清除缓存

### 3.4 退出群聊

1. `DELETE /capi/room/group/member/exit` → `GroupMemberServiceImpl.exitGroup()`
2. 逻辑：
    - **群主退出**：删除整个房间（Room + RoomGroup + 所有会话 + 所有成员 + 消息）
    - **普通成员退出**：仅删除自己的 `GroupMember` 和 `Contact` 记录
    - 热门群（全员群）不允许退出

### 3.5 管理员操作

- `addAdmin()`：群主才能操作；被操作者必须在群内；管理员数量有上限 `MAX_MANAGE_COUNT`
- `revokeAdmin()`：群主才能操作；被操作者必须在群内

---

## 四、消息发送全链路

### 4.1 Controller 层

1. `POST /capi/chat/msg` → `ChatController.sendMsg()`
2. 应用三层 `@FrequencyControl`（target=UID）：
    - 每 5 秒 ≤ 3 次
    - 每 30 秒 ≤ 5 次
    - 每 60 秒 ≤ 10 次

### 4.2 Service 层

3. `ChatServiceImpl.sendMsg()`（`@Transactional`）：
4. **房间校验 `check()`**：
    - 全员群（`hotFlag=YES`）：跳过校验
    - 好友房间：查 `RoomFriend` 确认状态正常且发送者是参与者
    - 群组房间：查 `GroupMember` 确认发送者是群成员
5. **策略选择**：`MsgHandlerFactory.getStrategyNoNull(request.getMsgType())` 获取对应的消息处理器

### 4.3 消息处理器（策略模式）

6. `AbstractMsgHandler.checkAndSaveMsg()`（`@Transactional`）：
    - JSR303 统一校验请求体
    - 子类 `checkMsg()` 扩展校验（如 TextMsg 检查回复消息存在、@用户存在且去重、@全员需管理员权限）
    - `MessageAdapter.buildMsgSave()` 构建 Message 实体 → `messageDao.save()` 入库
    - 子类 `saveMsg()` 保存扩展数据（如 TextMsg 执行 AC 自动机敏感词过滤、URL 元数据提取、@用户列表落盘）

**支持的消息类型**（8 种）：

| 类型 | type 值 | 处理要点 |
|------|---------|---------|
| TEXT | 1 | 敏感词过滤、URL 提取、@提及、回复引用 + gapCount |
| RECALL | 2 | 撤回消息 |
| IMG | 3 | 图片尺寸校验 |
| FILE | 4 | 文件关联 |
| SOUND | 5 | 语音消息 |
| VIDEO | 6 | 视频消息 |
| EMOJI | 7 | 表情消息 |
| SYSTEM | 8 | 系统通知（成员加入/离开/群名变更等） |

**自注册机制**：每个 `AbstractMsgHandler` 子类的 `@PostConstruct init()` 读取泛型 type 参数，调用 `MsgHandlerFactory.register()` 注册到静态 Map。

### 4.4 事件管道

7. `ChatServiceImpl.sendMsg()` 发布 `MessageSendEvent(this, msgId)`

8. `MessageSendListener`（三个 `@TransactionalEventListener`）：
    - **`messageRoute()`** — `phase=BEFORE_COMMIT`：通过 `MQProducer.sendSecureMsg()` 发 RocketMQ，内部 `@SecureInvoke` 确保事务提交后才真实投递
    - **`handlerMsg()`** — `phase=AFTER_COMMIT, fallbackExecution=true`：仅热门群触发 AI 自动回复
    - **`publishChatToWechat()`** — `phase=AFTER_COMMIT`：消息含 @提及 → 通过微信公众号模板消息推送给被提及用户（当前因微信审核暂停而注释）

### 4.5 MQ 消费

9. `MsgSendConsumer` 消费 `chat_send_msg` Topic：
    - 从 DB 加载完整 Message + 关联的 User/Room
    - 调用 `MessageAdapter.buildChatMessageResp()` 构建完整消息 VO（含发送者信息）
    - 按房间类型决定推送范围：
        - **热门群**：`PushService.sendPushMsg(wsBaseResp)` → 发送给所有在线用户
        - **普通群**：获取所有群成员 uid 列表 → 推送 + 批量更新 `Contact` 会话时间
        - **好友单聊**：获取双方 uid → 推送 + 更新会话时间
    - 更新 `Room.activeTime` + 清除 Room 缓存

### 4.6 WebSocket 推送

10. `PushService.sendPushMsg()` → RocketMQ `websocket_push` Topic
11. `PushConsumer` 消费（**广播模式**）：
    - `WSPushTypeEnum.USER`：遍历 `uidList`，逐一调用 `webSocketService.sendToUid(uid, wsBaseResp)`
    - `WSPushTypeEnum.ALL`：调用 `webSocketService.sendToAllOnline(wsBaseResp)`
12. `sendToUid()`：从 `ONLINE_UID_MAP` 获取该 uid 的所有 Channel（支持多标签页），逐一写入 `TextWebSocketFrame(JSON)`

### 4.7 消息标记（点赞/点踩）

- 策略模式 `MsgMarkFactory`：`LikeStrategy`（点赞）、`DisLikeStrategy`（点踩）
- 互斥逻辑：执行点赞时自动取消点踩，反之亦然
- `MessageMarkListener`：标记次数达到阈值 → 给消息作者发放 `LIKE_BADGE` 徽章；同时广播 WS 通知（type=8 MARK）

---

## 五、会话管理

### 5.1 Contact 表

- 追踪**每个用户在每个房间**的会话状态：`uid + room_id + last_msg_id + active_time`
- 消息发送后 `MsgSendConsumer` 批量调 `contactDao.refreshOrCreateActiveTime()` 更新
- 前端获取会话列表时按 `activeTime` 降序排列

### 5.2 消息列表

- 获取房间历史消息：光标分页 `CursorPageBaseReq`（`cursor` + `pageSize`），按 `id DESC`
- `ChatServiceImpl.getMsgResp()` 返回完整消息 VO：含发送者信息、消息体、回复引用、URL 元数据

---

## 六、WebSocket 连接管理

### 6.1 三层 Map 结构

| Map | Key | Value | 用途 |
|-----|-----|-------|------|
| `ONLINE_WS_MAP` | `Channel` | `WSChannelExtraDTO(uid)` | 追踪所有已连接 Channel |
| `ONLINE_UID_MAP` | `Long uid` | `CopyOnWriteArrayList<Channel>` | 一用户多标签页 |
| `WAIT_LOGIN_MAP` | `Integer loginCode` | `Channel` | 扫码登录等待队列 |

### 6.2 生命周期

- `connect(channel)` → 注册到 `ONLINE_WS_MAP`
- `authorize(channel, token)` → JWT 验证 → `loginSuccess()` → 注册到 `ONLINE_UID_MAP` + 推送 LOGIN_SUCCESS + 发布 `UserOnlineEvent`
- `removed(channel)` → 清理三层 Map → 如果用户完全离线则发布 `UserOfflineEvent`
- 30 秒读空闲（`IdleStateHandler(30)`） → 断开连接

### 6.3 WebSocket 协议枚举

**请求（客户端 → 服务端）**：

| type | 含义 | payload |
|------|------|---------|
| 1 | LOGIN | 无（请求生成二维码） |
| 2 | HEARTBEAT | 无（心跳保活） |
| 3 | AUTHORIZE | `{token}` |

**响应（服务端 → 客户端）**：

| type | 含义 | payload |
|------|------|---------|
| 1 | LOGIN_URL | `{loginUrl}` — 微信二维码 ticket |
| 2 | LOGIN_SCAN_SUCCESS | 空 — 扫码成功，等待授权 |
| 3 | LOGIN_SUCCESS | `{uid, avatar, token, name, power}` |
| 4 | MESSAGE | `ChatMessageResp` — 新消息 |
| 5 | ONLINE_OFFLINE_NOTIFY | `{uid, online}` |
| 6 | INVALIDATE_TOKEN | 空 — Token 失效 |
| 7 | BLACK | 被拉黑通知 |
| 8 | MARK | `{userId, msgId, markType, actType}` |
| 9 | RECALL | `{msgId, recallUid}` |
| 10 | APPLY | `{uid, unreadCount}` — 好友申请通知 |
| 11 | MEMBER_CHANGE | `{roomId, uid}` — 群成员变更 |

---

## 七、横切关注点

### 7.1 频控体系

- `@FrequencyControl` 注解 + `FrequencyControlAspect` AOP
- 三层策略：`TotalCountWithInFixTimeFrequencyController`（固定窗口计数）
- 策略工厂 `FrequencyControlStrategyFactory` 可扩展（固定窗口/滑动窗口/令牌桶）
- Key 格式：`{prefixKey}:{methodName}:index:{i}:{targetKey}`（Redis 存储）

### 7.2 分布式锁

- `@RedissonLock` 注解 + `RedissonLockAspect` AOP（`@Order(0)` 在事务前执行）
- Redisson `RLock` 实现

### 7.3 安全调发

- `@SecureInvoke` 注解 + `SecureInvokeAspect`
- 本地消息表 `secure_invoke_record` + `@Scheduled` 定时重试
- 用于 RocketMQ 发送：确保事务提交后才投递 MQ 消息

### 7.4 缓存体系

- `AbstractRedisStringCache`：批量缓存抽象（`getBatch()` → 先查缓存 → 未命中查 DB → 回填缓存）
- 用于：`UserInfoCache`、`RoomCache`、`RoomGroupCache`、`RoomFriendCache`、`HotRoomCache`

### 7.5 敏感词过滤

- AC 自动机（`ACFilter` + `ACTrie`）
- 词库来源：`sensitive_word` 数据库表（`MyWordFactory.getWordList()`）
- 在 `TextMsgHandler.saveMsg()` 中 inline 过滤，将命中位置字符替换为 `*`

---

## 八、线程池

| 池名 | 核心/最大 | 队列 | 拒绝策略 | 用途 |
|------|----------|------|---------|------|
| `mallchatExecutor` | 10/10 | 200 | CallerRunsPolicy | 通用异步 |
| `websocketExecutor` | 16/16 | 1000 | DiscardPolicy | WS 消息处理 |
| `aichatExecutor` | 10/10 | 15 | DiscardPolicy | AI 聊天 |
