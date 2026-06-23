# 社群平台 开发日志 (2)

> **续篇** — 前篇 `dev-log.md` 覆盖 Phase 0 ~ Phase 4.6（项目骨架 → CI/CD）。本文档记录 Phase 5 起的工作。
>
> 前篇待办 T1~T11 已完成。**T7 (PushService) 2026-06-22 审计纠正：原误标 ✅，实际未实现。**

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前 Phase**: Phase 6 文件模块（6.1-6.2 完成，6.3 ES 搜索待补）+ Bug 修复 + 功能缺口补全
- **最新提交**: `f5f20cc` — Bug 修复 + Redisson/频控 AOP/本地缓存功能补全
- **日期**: 2026-06-23

---

## Phase 0-6 功能缺口审计 (2026-06-23，提交后复核)

> 对比 `docs/plan/` 计划文档 + `dev-log-2.md` 待办清单 vs 实际代码，排除 Phase 7/7+/8 架构升级内容，
> 聚焦 **现有功能** 的缺口与不足。已完成项（今日 Round 1-4）不再列出。

### 🔴 运行时影响（4 项）

| # | 条目 | 现状 | 位置 |
|---|------|------|------|
| G1 | **@EnableAsync 无自定义 Executor** — `@Async` 方法回退到 `SimpleAsyncTaskExecutor`（每任务新建线程），高并发下线程泄漏 | `CommunityApplication.java:14` 无 executor 绑定 | `CommunityApplication.java` + `MessageSendListener.java:21` |
| G2 | **Message.extra JSON→String 缺 TypeHandler** — MyBatis-Plus 可能反序列化异常，当前手动 JSONUtil 读写绕过，但仍存风险 | `Message.java:28` String 字段无 `@TableField(typeHandler=...)` | `message/domain/entity/Message.java` |
| G3 | **WebSocketServiceImpl.handleSendMessage 桩** — WS 通道发送消息仅记日志，不做处理 | 明确桩（需经 REST API），但无错误响应通知客户端 | `WebSocketServiceImpl.java:142-144` |
| G4 | **WebSocketServiceImpl.loginSuccess hasPower = false** — 始终硬编码，未接入角色系统 | TODO 注释，token 中无 role 字段传递 | `WebSocketServiceImpl.java:277` |

### 🟡 功能不完整（5 项）

| # | 条目 | 现状 | 位置 |
|---|------|------|------|
| G5 | **SearchServiceImpl 仍为 MySQL LIKE** — ES 搜索完全缺失（Phase 6.3） | 无 ElasticsearchConfig / MessageDocument / SearchRepository | `SearchServiceImpl.java:36` |
| G6 | **docker-compose 缺 ES 容器** — 6.3 依赖 | compose 仅 6 服务：mysql/redis/minio/rocketmq×2/app | `docker-compose.yml` |
| G7 | **SearchServiceImpl + ThreadServiceImpl Reaction 残留** — 搜索和 Thread 消息列表传递空 reactions，不附加 ReactionVO | 已修复主消息列表，搜索和 Thread 未修复 | `SearchServiceImpl.java:38` + `ThreadServiceImpl.java:129` |
| G8 | **Compose 端口与 local.properties 不同步** — compose 暴露 MySQL:3308/Redis:6381/MinIO:9004/RocketMQ:9878，local.properties 使用对应端口，但 Docker profile 应统一 | 无 runtime 影响 | compose vs `application-local.properties` |
| G9 | **bind-wx 实际返回 ApiResult<UserVO>** — A9 已修复，但 `AccountBindReq` DTO 是否正确待验证 | 可能不匹配前端预期 | `UserController.java:31` |

### ⚪ 低优先级（7 项）

| # | 条目 | 现状 |
|---|------|------|
| G10 | **6 个事件未实现** — MemberJoin/Kick/RoleUpdate/OwnershipTransfer/ServerCreate/ThreadCreate | 仅 `ChannelMessageSendEvent` 存在 |
| G11 | **WxMsg 持久化表** — 微信原始消息审计（#16，后续按需） | 无 DDL/Entity/Mapper |
| G12 | **@SecureInvoke + MQProducer** — 事务安全投递（#23，Phase 8） | 直接 RocketMQTemplate 发送 |
| G13 | **Server 实体缺 join_mode 字段** — FREE/INVITE/APPLY 三种加入模式 | 仅 INVITE 模式 |
| G14 | **getDiscoverableServers 无游标分页** — 返回全量 List | 数据量小时可接受 |
| G15 | **ServerMemberApply 实体/VO/DDL 缺失** — 加入审批流（Phase 7） | 架构预留 |
| G16 | **mallchat-flows-visual.html 未复制** — MallChat 有，社区平台未移植 | 文档项 |

### ✅ 已确认修复（7 项，今日提交 `f5f20cc`）

| # | 条目 |
|---|------|
| — | A1/A2 getById → lambdaQuery (ImageMsgHandler + FileMsgHandler) |
| — | A3 MessageAdapter attachments 组装 (4 路径 + WS) |
| — | ExtraBody Java record → static class 序列化修复 |
| — | D2/D3 AOP 切面 + Redisson 集成 (RedissonLockAspect + FrequencyControlAspect) |
| — | D5 AbstractLocalCache (Caffeine) |
| — | B4 CursorUtils @SuppressWarnings |
| — | B5 WxMsgService @SuppressWarnings("deprecation") |

---

## 完整待办汇总（32 项，2026-06-23 Plan-vs-Code 审计更新）

### 一、🔴 阻断性：消息推送管线（Phase 4.x — 阻断 Phase 5.6 验证）

```
① MessageSendListener  →  发 RocketMQ SEND_MSG_TOPIC     (P4-4, 3 行代码)
② PushMessageDTO        →  新建 data class                 (新增)
③ MsgSendConsumer       →  消费 SEND_MSG → 转 PushDTO     (P4-1, 新类)
④ PushService           →  RocketMQTemplate 封装发 PUSH   (T7)
⑤ PushConsumer          →  解析 PushDTO → 分派            (5.3.1, 替换 TODO)
⑥ pushToChannel/Thread  →  Redis SMEMBERS → sendToUid     (5.3.3, 替换 TODO)
```

| # | 来源 | 条目 | 现状 | 文件 |
|---|------|------|------|------|
| ~~1~~ | ~~P4-4~~ | ~~MessageSendListener 发 MQ~~ | ✅ 已修复 (`36bc4be`) | 注入 RocketMQTemplate → SEND_MSG_TOPIC |
| ~~2~~ | ~~新增~~ | ~~PushMessageDTO~~ | ✅ 已创建 (`36bc4be`) | common/domain/dto/PushMessageDTO.java |
| ~~3~~ | ~~P4-1~~ | ~~MsgSendConsumer~~ | ✅ 已创建 (`36bc4be`) | message/consumer/MsgSendConsumer.java |
| ~~4~~ | ~~T7~~ | ~~PushService~~ | ✅ 已创建 (`36bc4be`) | message/service/PushService.java + impl |
| ~~5~~ | ~~5.3.1~~ | ~~PushConsumer 实现~~ | ✅ 已修复 (`36bc4be`) | 解析 PushMessageDTO → 分派 |
| ~~6~~ | ~~5.3.3~~ | ~~pushToChannel / pushToThread~~ | ✅ 已修复 (`36bc4be`) | Redis SMEMBERS → sendToUid |

### 二、🟡 Phase 4.x 其他遗留

| # | 来源 | 条目 | 说明 |
|---|------|------|------|
| ~~7~~ | ~~P4-3~~ | ~~autoArchiveThreads~~ | ✅ 已修复 (`7af4244`) | @Scheduled + @EnableScheduling |
| ~~8~~ | ~~P4-6r~~ | ~~Reaction 残留~~ | ✅ 已修复 (`7af4244`) | SearchServiceImpl + ThreadServiceImpl 批量查 Reaction |

### 三、🟡 Phase 5 未完成（推送管线打通后）

| # | 来源 | 条目 | 说明 |
|---|------|------|------|
| ~~9~~ | ~~5.4.1-2~~ | ~~Handler 路由 TYPING_START / TYPING_STOP~~ | ✅ 已修复 (`7af4244`) | NettyWebSocketServerHandler switch 新增两路 |
| ~~10~~ | ~~新增~~ | ~~WebSocketService.handleTypingStart/Stop~~ | ✅ 已修复 (`7af4244`) | 解析 channelId/threadId → pushToChannel/Thread |
| ~~11~~ | ~~5.5.1-2~~ | ~~MEMBER_ONLINE / MEMBER_OFFLINE~~ | ✅ 已修复 (`7af4244`) | loginSuccess 广播上线, removed 广播离线 |
| ~~12~~ | ~~5.3.4~~ | ~~WSAdapter 缺 10 个 build 方法~~ | ✅ 已修复 (`7af4244`) | 10 个新方法覆盖所有 WSRespTypeEnum 推送类型 |

### 四、🔵 Backlog 功能缺口（来自 dev-log T4-T9）

| # | 来源 | 条目 | 现状 |
|---|------|------|------|
| ~~13~~ | ~~T4~~ | ~~EmojiMsgHandler~~ | ✅ 已创建 (`待提交`) | message/service/strategy/msg/EmojiMsgHandler.java |
| ~~14~~ | ~~T5~~ | ~~MentionParser（@提及正则 + uid 解析）~~ | ✅ 已创建 (`待提交`) | common/utils/MentionParser.java |
| ~~15~~ | ~~T8~~ | ~~AC 自动机敏感词~~ | ✅ 已移植 (`待提交`) | common/algorithm/sensitiveWord/ + common/sensitive/ 共 11 个文件 |
| 16 | T9 | WxMsg 持久化表 | 后续按需（微信原始消息审计） |

### 五、🔵 File 模块预检（Phase 6 前置）

| # | 条目 | 说明 |
|---|------|------|
| ~~17~~ | ~~FileService 3 方法~~ | ✅ 已实现 | MinIO presigned URL upload + confirm + download |
| ~~18~~ | ~~MinIOConfiguration~~ | ✅ 已创建 | file/config/MinIOConfiguration.java + common/config/OssProperties.java |
| ~~19~~ | ~~MessageAdapter 文件关联~~ | ✅ 已修复 — MessageAdapter.buildFileVO + attachments 组装（send/get/edit/list + WS 推送） |
| 20 | docker-compose 验证 MinIO | 已配置 9004-5，需联调 |

### 六、🔵 Search 模块预检（Phase 6 前置）

| # | 条目 | 说明 |
|---|------|------|
| 21 | SearchService ES 实现 | 当前 MySQL LIKE 占位，需替换为 ES Client |
| 22 | docker-compose 缺 ES 容器 | 需添加 Elasticsearch 8.11 服务 |

### 七、⚪ 技术债

| # | 来源 | 条目 |
|---|------|------|
| 23 | T12 | @SecureInvoke + MQProducer 事务安全投递 |
| ~~24~~ | ~~T14~~ | ~~CursorUtils.java unchecked 泛型警告~~ ✅ `@SuppressWarnings` |
| ~~25~~ | ~~T15~~ | ~~WxMsgService.java deprecated API~~ ✅ `@SuppressWarnings("deprecation")` |

### 八、⚪ 文档

| # | 来源 | 条目 |
|---|------|------|
| 26 | T13 | mallchat-flows-visual.html 未复制（MallChat plan 有，社区平台未移植） |

### 九、⏳ Phase 7-8 架构升级

| # | 来源 | 条目 |
|---|------|------|
| 27 | T16 | 包级接口隔离审计（Phase 7+） |
| 28 | T17 | Nacos + Gateway + Feign 微服务拆分（Phase 8） |
| 29 | T18 | 配置中心迁移（MySQL/Redis/RocketMQ → Nacos） |

### 十、🔴 Plan-vs-Code 审计新增 (2026-06-23)

> 全量对比 `docs/plan/` 与代码实际状态，共 9 处结构差异（D1-D9）。以下为需实际编码的条目。

| # | 来源 | 条目 | 说明 |
|---|------|------|------|
| ~~30~~ | ~~D2~~ | ~~**AOP 切面实现**~~ | ✅ 已修复 — RedissonLockAspect + FrequencyControlAspect + spring-boot-starter-aop |
| ~~31~~ | ~~D3~~ | ~~**RedisConfig / Redisson 集成**~~ | ✅ 已修复 — RedisConfig (RedissonClient + RedisTemplate Bean) |
| ~~32~~ | ~~D5~~ | ~~**AbstractLocalCache (Caffeine)**~~ | ✅ 已创建 — Caffeine LoadingCache 本地缓存抽象 |

### 十一、🔵 补充说明（无需编码）

| # | 来源 | 条目 | 决议 |
|---|------|------|------|
| — | D1 | community-transaction 模块 | Phase 8 微服务拆分时按需补（当前单体无需 `@SecureInvoke` 事务安全投递） |
| — | D4 | RestTemplateConfig | Phase 8 Feign 前无需 |
| — | D6 | reaction/ 策略目录为空 | 保持现状（ReactionServiceImpl inline 逻辑功能等价） |
| — | D7 | FileVO 在 message/domain/vo/ | 后续可迁移到 file/domain/vo/ |
| — | D8 | server/domain/dto/ 为空 | 保持现状 |
| — | D9 | WxMsgService 是具体类非接口 | 保持现状（功能等价） |

---

## Phase 4.x: 消息推送管线缺口

> 即上表 #1~#6。补全后消息全链路才完整。

### 当前链路断裂点

```
MessageServiceImpl.sendMessage()
  → AbstractMsgHandler.checkAndSaveMsg() ✅
  → eventPublisher.publishEvent(ChannelMessageSendEvent) ✅
  → MessageSendListener.onMessageSend()  ⚠️ 仅 log，未发 MQ        ← 缺口 #1
  → MsgSendConsumer                     ❌ 不存在                  ← 缺口 #3
  → PushService.sendPushMsg()           ❌ 不存在                  ← 缺口 #4
  → PushConsumer.onMessage()             ⚠️ 仅 log                 ← 缺口 #5
  → WebSocketService.pushToChannel()     ❌ TODO 空壳              ← 缺口 #6
  → WebSocketService.sendToUid()         ✅ 可用
```

---

## Phase 5: 基础设施现状

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 已就绪（13/26 checkbox）

| 组件 | 状态 | 说明 |
|------|------|------|
| `NettyWebSocketServer` | ✅ | 端口 8091，Pipeline 完整 |
| `HttpHeadersHandler` | ✅ | 提取 Token + IP |
| `NettyUtil` | ✅ | Channel Attribute 管理 |
| `NettyWebSocketServerHandler` | ✅ | switch 分发 7 种请求（LOGIN/HEARTBEAT/SEND_MESSAGE/SUBSCRIBE×3/UNSUBSCRIBE×2） |
| `WebSocketService` 接口 | ✅ | 55 行，完整定义 |
| `WebSocketServiceImpl` | ✅ | connect/authorize/removed/subscribe*/sendToUid/sendToAllOnline |
| `WSAdapter` | ⚠️ | 8 个 build 方法，缺 10 个 |
| `PushConsumer` | ⚠️ | 骨架存在，仅 log |
| `WSReqTypeEnum` | ✅ | 10 种请求（含 TYPING_START/STOP） |
| `WSRespTypeEnum` | ✅ | 18 种响应（含 TYPING/MEMBER/THREAD/CHANNEL/SERVER 推送） |

### 计划 checkbox 逐项核对

| 章节 | 完成 | 总数 | 缺口 |
|------|------|------|------|
| 5.1 Netty 服务器 | 4 | 4 | — |
| 5.2 连接管理 | 7 | 7 | — |
| 5.3 消息推送 | 1 | 4 | pushToChannel/Thread, PushConsumer, WSAdapter |
| 5.4 输入状态 | 0 | 3 | 全部未实现 |
| 5.5 在线状态 | 0 | 2 | 全部未实现 |
| 5.6 验证 | 2 | 7 | 推送/输入/Thread 场景不可验证 |
| **合计** | **14** | **27** | **13 条待补** |

---

## Phase 6: 文件 + 搜索 — 待开始

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 文件模块

| 组件 | 状态 |
|------|------|
| `FileController` (4 端点) | ✅ |
| `FileService` 接口 | ✅ |
| `FileServiceImpl` | ❌ 全部 throw TODO |
| `FileAttachment` Entity + Mapper + Dao | ✅ |
| MinIO 容器 (docker-compose) | ✅ 9004-5 |
| MinIOConfiguration (`@Configuration` Bean) | ❌ |

### 搜索模块

| 组件 | 状态 |
|------|------|
| `SearchController` | ✅ |
| `SearchService` 接口 | ✅ |
| `SearchServiceImpl` | ⚠️ MySQL LIKE 占位 |
| ES 容器 (docker-compose) | ❌ |
| ElasticsearchConfig | ❌ |

---

---

## 审计记录：代码 vs 计划 (2026-06-23)

### 🔴 高优先级

| # | 类别 | 问题 | 位置 |
|---|------|------|------|
| A1 | **端口不同步** | docker-compose 暴露端口与 `application-local.properties` 不一致：MySQL `3308`→`3307`、Redis `6381`→`6380`、MinIO `9004`→`9000`、RocketMQ `9878`→`9877`。Docker 启动后用 local profile 连不上基础设施。 | compose vs local.properties |
| A2 | **SQL 注入** | `SearchServiceImpl` channelId 通过字符串拼接注入 SQL：`.last("AND channel_id = " + channelId)`，应改用 `eq()` | `SearchServiceImpl.java:39` |
| A3 | **端点未实现** | `GET /api/v1/files/{fileId}` 抛 `UnsupportedOperationException("TODO")` | `FileController.java:37` |
| A4 | **分页缺失** | `GET /api/v1/servers/discover` 计划要求游标分页 `CursorPage<ServerVO>`，实际返回全量 `List<ServerVO>`，无分页参数 | `ServerController.java` |

### 🟡 中优先级

| # | 问题 | 位置 |
|---|------|------|
| ~~A5~~ | ~~**ImageMsgHandler/FileMsgHandler getById bug**~~ | ✅ 已修复 — `getById` → `lambdaQuery().eq(id).one()` |
| A6 | **SensitiveWordMapper** 缺少 `@Mapper` 注解（15 个其他 Mapper 都有，打破惯例） | `SensitiveWordMapper.java` |
| A7 | **DDL 注释过时** — message 表 `msg_type` 注释缺 `6=EMOJI` | `ddl.sql:197` |
| A8 | **WebSocket 端口硬编码** — `NettyWebSocketServer.WEB_SOCKET_PORT = 8091` 未外部化到配置 | `NettyWebSocketServer.java:29` |
| A9 | **bind-wx 返回 null** — spec 要 `UserVO`，实际返回 `Void`（`ApiResult.success(null)`） | `UserController.java` |
| A10 | **登录权限 TODO** — `loginSuccess()` 始终 `hasPower = true`，未接入角色系统 | `WebSocketServiceImpl.java:278` |
| A11 | **WS handleSendMessage 桩** — 通过 WebSocket 发送的消息只记日志，不做处理 | `WebSocketServiceImpl.java:144` |

### ⚪ 低优先级

| # | 问题 |
|---|------|
| A12 | DELETE reactions 返回 204 No Content，spec 定义 `{ emoji, userId, totalCount }` |
| A13 | POST reactions 返回 `List<ReactionVO>`，spec 定义单个对象 |
| A14 | POST `/servers` 额外接受 `icon` 字段（spec 只有 name + description） |
| A15 | `server.port` 未显式配置，依赖 Spring Boot 默认 8080 |
| A16 | ES 搜索 (Phase 6.3) 完全缺失：无 ElasticsearchConfig / MessageDocument / SearchRepository |
	
	### 运行时测试发现 (2026-06-23)
	
	| # | 问题 | 位置 |
	|---|------|------|
	| A19 | **MinIO 配置不完整** — 缺 `region("us-east-1")` 导致 presigned URL 生成失败；bucket 不存在需手动创建 | `MinIOConfiguration.java` |
	| A20 | **FileAttachmentDao.getById 异常** — `getById(id)` 返回 null 但 DB 有记录，同表 `lambdaQuery().eq(id).one()` 正常。根因未明，统一改用 lambdaQuery | `FileServiceImpl.java` |
	| A21 | **FileVO 缺 status 字段** — GET /files/{fileId} 无法区分 PENDING/UPLOADED，PENDING 文件调用 getDownloadUrl 抛异常 | `FileVO.java` |
	| A22 | **GET /files/{fileId} 对 PENDING 文件报错** — buildFileVO 无条件调 getDownloadUrl，PENDING 文件应返回 metadata（downloadUrl=null） | `FileServiceImpl.java` |

### 实体 vs DDL 字段映射审计

| # | 表 | 问题 |
|---|------|------|
| A17 | `sensitive_word` | **SensitiveWord.word 缺 `@TableId`** — DDL 中 word 是主键，entity 无 `@TableId(type = IdType.INPUT)` |
| A18 | `message` | **extra 列 JSON→String 无 TypeHandler** — 可能序列化/反序列化异常 |

### Event / MQ 配线审计

| 结果 |
|------|
| ✅ Spring Event 链完整：1 事件+1 发布者+1 监听器→MQ |
| ✅ RocketMQ 4 主题 4 消费者全部配对，无孤立端点 |
| ⚠️ `@EnableAsync` 无自定义 Executor（高并发下每任务新建线程） |
| ⚠️ 计划文档 6 个事件尚未实现：MemberJoinEvent, MemberKickEvent, MemberRoleUpdateEvent, OwnershipTransferEvent, ServerCreateEvent, ThreadCreateEvent |
| ⚠️ `05-dev-phases-0-4.md` 部分 checkbox 标未完成但代码已实现（文档过时） |

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-22 | 创建 2 号日志，续接 dev-log.md | `a08329a` |
| 2026-06-22 | 完整待办汇总（29 项）+ T7 纠错 | `0cfd4ea` |
| 2026-06-22 | **打通推送管线 6 项** — MessageSendListener → MQ → MsgSendConsumer → PushService → PushConsumer → pushToChannel/Thread | `36bc4be` |
| 2026-06-22 | **Phase 4.x 遗留 + Phase 5 未完成 6 项** — autoArchive + Reaction 残留 + Typing + Online/Offline + WSAdapter 全 18 类型 | `7af4244` |
| 2026-06-23 | **Backlog: 敏感词过滤系统 + EmojiMsgHandler** — DFAFilter + ACTrie + SensitiveWordBs + MyWordFactory + TextMsgHandler 集成 + MessageTypeEnum.EMOJI | 待提交 |
| 2026-06-23 | **Backlog: MentionParser + JsonUtils + FutureUtils** — @提及正则解析器 + Jackson 工具类 + CompletableFuture 工具集 | 待提交 |
| 2026-06-23 | **Phase 6: MinIOConfiguration + FileServiceImpl** — OssProperties 配置绑定 + MinioClient Bean + 预签名上传/确认/下载 三方法实现 | 待提交 |
| 2026-06-23 | **审计修复 (A1-A11)** — 端口同步 + SQL注入修复 + FileController.getFile + Server分页 + 文件上传校验 + WS端口外部化 + bind-wx返回值 + @Mapper/@TableId补完 | 待提交 |
| 2026-06-23 | **API 全量测试 + 运行时修复 (A19-A22)** — MinIO region+bucket 自动创建 + FileServiceImpl getById→lambdaQuery + FileVO.status + PENDING 文件 GET 修复 | 待提交 |
| 2026-06-23 | **Plan-vs-Code 全量审计** — API 46/46 端点比对通过 + 9 处结构差异 (D1-D9) + 计划文档 checkbox 同步 + 新增待办 30-32 (AOP切面/Redisson/Caffeine) | 待提交 |
| 2026-06-23 | **Round 1: Bug 修复 (A1-A3)** — ImageMsgHandler/FileMsgHandler getById→lambdaQuery + MessageAdapter attachments 组装 (4 路径) | 待提交 |
| 2026-06-23 | **Round 2: Redisson 分布式锁 (B1)** — RedisConfig + SpElUtils + RedissonLockAspect + LockService + spring-boot-starter-aop | 待提交 |
| 2026-06-23 | **Round 3: 频控 AOP (B2)** — FrequencyControlAspect + TotalCountWithInFixTime + 策略工厂 + RedisUtils.inc(TTL) | 待提交 |
| 2026-06-23 | **Round 4: 收尾 (B3-B5)** — AbstractLocalCache + CursorUtils @SuppressWarnings + WxMsgService @SuppressWarnings | 待提交 |
| 2026-06-23 | **Bug 修复: ExtraBody 序列化** — ImageMsgHandler/FileMsgHandler 中 Java `record` 改为 static class（Hutool JSONUtil 无法序列化 record 导致 extra 为 `{}`） | `f5f20cc` |
| 2026-06-23 | **Phase 0-6 功能缺口审计** — 对比计划+代办 vs 代码，新增 G1-G16 缺口清单（4 运行时 + 5 功能不完整 + 7 低优先级），确认 7 项已修复 | `f5f20cc` |

---

## 参考

- 前篇开发日志: `docs/dev-log.md`
- 项目计划: `docs/plan/`
- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
