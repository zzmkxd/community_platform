# 五、Phase 1 → Phase 2 迁移路径 + 六、DDL 设计要点

## 5.1 Phase 1 代码组织（单体，内部分包）

```
community-server/src/main/java/com/community/
  ├── CommunityApplication.java     # 唯一启动类
  ├── common/                       # 共享基础设施
  │   ├── config/                   # RedisConfig, MybatisPlusConfig, ThreadPoolConfig
  │   ├── interceptor/              # TokenInterceptor, CollectorInterceptor（BlackInterceptor 暂不移植）
  │   ├── exception/               # BusinessException, GlobalExceptionHandler
  │   ├── annotation/              # @FrequencyControl, @RedissonLock, @SecureInvoke
  │   ├── aspect/                  # AOP 切面
  │   ├── cache/                   # AbstractRedisStringCache
  │   ├── utils/                   # JwtUtils, CursorUtils, RequestHolder
  │   └── websocket/               # WSBaseResp, WSBaseReq, WSRespTypeEnum, WSReqTypeEnum
  │
  ├── user/
  │   ├── controller/UserController.java, AuthController.java
  │   ├── service/UserService.java, AuthService.java
  │   ├── dao/UserDao.java (extends ServiceImpl<UserMapper, User>)
  │   └── domain/entity/User.java, vo/*, dto/*
  │
  ├── server/
  │   ├── controller/ServerController.java, CategoryController.java,
  │   │              ChannelController.java, MemberController.java,
  │   │              RoleController.java, EmojiController.java
  │   ├── service/ServerService.java, ChannelService.java,
  │   │           MemberService.java, RoleService.java, PermissionService.java
  │   ├── dao/ServerDao.java, ChannelDao.java, ...
  │   └── domain/entity/Server.java, Category.java, Channel.java,
  │                     ServerMember.java, Role.java, MemberRole.java,
  │                     ChannelPermission.java, Emoji.java
  │                     enums/PermissionBit.java, MemberStatusEnum.java
  │                     vo/*, dto/*
  │
  ├── message/
  │   ├── controller/MessageController.java, ThreadController.java,
  │   │              ReactionController.java, SearchController.java
  │   ├── service/MessageService.java, ThreadService.java,
  │   │           ReactionService.java, SearchService.java
  │   ├── service/strategy/msg/
  │   │   ├── AbstractMsgHandler.java, MsgHandlerFactory.java
  │   │   ├── TextMsgHandler.java, ImageMsgHandler.java,
  │   │   │   FileMsgHandler.java, SystemMsgHandler.java
  │   ├── service/strategy/reaction/
  │   │   ├── AbstractReactionStrategy.java, ReactionStrategyFactory.java
  │   ├── event/MessageSendEvent.java, MessageSendListener.java
  │   ├── consumer/MsgSendConsumer.java
  │   ├── dao/MessageDao.java, ThreadDao.java, ReactionDao.java
  │   └── domain/entity/Message.java, Thread.java, Reaction.java
  │                     vo/*, dto/*, enums/MessageTypeEnum.java
  │
  ├── file/
  │   ├── controller/FileController.java
  │   ├── service/FileService.java
  │   ├── config/MinIOConfiguration.java
  │   ├── dao/FileAttachmentDao.java
  │   └── domain/entity/FileAttachment.java, vo/*
  │
  └── websocket/
      ├── NettyWebSocketServer.java
      ├── NettyWebSocketServerHandler.java
      ├── HttpHeadersHandler.java
      ├── NettyUtil.java
      ├── service/WebSocketService.java, WebSocketServiceImpl.java
      └── consumer/PushConsumer.java
```

## 5.2 Phase 2 拆分动作

```
Phase 1 包                →  Phase 2 模块             独立数据库
─────────────────────────────────────────────────────────────
common/       (部分)      →  community-common         无 (纯 JAR)
transaction/              →  community-transaction     无 (纯 JAR)
user/                     →  user-service             user_db
server/                   →  server-service           server_db
message/                  →  message-service          message_db
file/                     →  file-service             file_db
websocket/                →  websocket-server         无数据库
                   新增   →  gateway-server           无数据库
```

**拆分时跨服务调用的关键点**：
- `message-service.sendMsg()` 需要权限检查 → Feign 调 `server-service.checkPermission()`
- `message-service` 构建 MessageVO 需要用户信息 → Feign 调 `user-service.getBatchUsers()`
- `PushConsumer` 需要在线用户 Channel → 仍在 websocket-server 内部，无需跨服务

**Phase 2 暂不做数据库分库**，统一连接同一个 MySQL 实例。分库等到数据量需要时再拆。Phase 2 的重点是服务拆分的正确性。

---

## 六、DDL 设计要点

全部 **15 张表**（含 invite、channel_read_state）在一个 schema `community_platform` 中。关键设计决策：

1. **Server 和 Channel 的关系**：Channel 通过 `server_id` 直连 Server，同时可选关联 `category_id`。排序依赖 `sort_order` 字段。
2. **权限位图**：使用 `BIGINT` 存位掩码，13 个权限位，MySQL 位运算：`permissions & 0x4 = 0x4` 判断 SEND_MESSAGES。
3. **Channel Permission 覆盖**：`target_type` 区分角色覆盖(0)和用户覆盖(1)，`allow_bits` 和 `deny_bits` 独立。deny 优先级高于 allow。
4. **Thread 不独立存消息**：Thread 内的消息复用 `message` 表，通过 `thread_id` 关联。root_msg_id 指向触发生成 thread 的那条消息。
5. **Reaction 唯一约束**：`(message_id, user_id, emoji)` 联合唯一，确保同一用户对同一消息只能添加一次同一 emoji。
6. **Message 的 extra JSON**：利用 MySQL 的 JSON 类型存储附件 ID 列表、URL 预览、@提及列表等扩展信息。
7. **软删除策略**：Server/Channel 使用 status 字段标记删除状态（物理保留，逻辑删除），Message 使用 status 字段（0=正常 1=删除 2=编辑过）。
8. **Invite 邀请链接**：包含唯一 `code`（UUID 短码），可设过期时间和最大使用次数。关联 `server_id` + `inviter_id`。用户通过邀请链接加入时，校验 code 有效性 → 创建 member 记录 → 分配 @everyone 角色。
9. **Channel read state 已读追踪**：`channel_read_state` 表记录用户在每个频道的最后已读消息 ID。唯一约束 `(user_id, channel_id)`。用户拉取消息时自动更新 `last_read_msg_id`。
10. **补充表**：`sensitive_word` 表存储敏感词库（AC 自动机词典），`wx_msg` 表存储微信原始消息记录（可选审计）。
11. **在线状态不存 DB**：用户在线/离线状态仅在 Redis 中维护，不写入 `user` 表。
12. **MQ 主题常量**：所有 RocketMQ Topic 和 Consumer Group 常量统一在 `common/constant/MQConstant.java` 中定义。
13. **Channel type 枚举**：当前仅 `TEXT=0`，`VOICE=1` 预留。
14. **User 表支持双模式**：`username/password/email` 仅用于 seed data 测试账号（无注册端点），`open_id` (UK) 用于微信 OAuth 扫码创建的用户。两种用户形态互补，通过 NULL 值区分。

---

## 补充：尚待补全的组件

以下组件在现有代码骨架中尚未实现，Phase 4.x~5 推进时补全：

| 包 | 待加入组件 | 对应 Phase | 状态 |
|----|----------|-----------|------|
| server/ | Invite Entity + Mapper + Dao + Service + Controller | Phase 3 | ✅ 已完成 |
| server/ | ChannelReadState Entity + Mapper + Dao + Service | Phase 4 | ✅ 已完成 |
| message/ | SoundMsgHandler, MsgHandlerFactory, AbstractMsgHandler, TextMsgHandler, ImageMsgHandler, FileMsgHandler, SystemMsgHandler | Phase 4 | ✅ 已完成 |
| message/ | PushService, MsgSendConsumer | Phase 4 | ❌ 待补（Phase 4.x） |
| message/ | MessageSendEvent/Listener | Phase 4 | ⚠️ 骨架存在（仅 log，未发 MQ） |
| common/ | MentionParser（@提及解析） | 后续 | v1 暂缓 |
| common/ | sensitive/ (AC 自动机敏感词) | 后续 | v1 暂缓 |
| file/ | FileService 真实实现（MinIO 预签名） | Phase 6 | ❌ 待补（当前 throw TODO） |
| message/ | SearchService ES 实现 | Phase 6 | ⚠️ 当前用 MySQL LIKE 占位 |

> 详见 [08-microservice-upgrade-path.md](./08-microservice-upgrade-path.md) 微服务拆分操作手册。
