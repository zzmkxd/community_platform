# 社群平台 开发日志 (2)

> **续篇** — 前篇 `dev-log.md` 覆盖 Phase 0 ~ Phase 4.6（项目骨架 → CI/CD）。本文档记录 Phase 5 起的工作。
>
> 前篇待办 T1~T11 已完成。**T7 (PushService) 2026-06-22 审计纠正：原误标 ✅，实际未实现。**

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前 Phase**: Phase 6 文件模块（6.1-6.2 完成，6.3 ES 搜索待补）+ 代码质量审计
- **最新提交**: `7fff1c6` — 清理死代码与过时文档 + 审计表同步
- **日期**: 2026-06-23

---

## Phase 0-6 功能缺口审计 (2026-06-23，提交后复核)

> 对比 `docs/plan/` 计划文档 + `dev-log-2.md` 待办清单 vs 实际代码，排除 Phase 7/7+/8 架构升级内容，
> 聚焦 **现有功能** 的缺口与不足。已完成项（今日 Round 1-4）不再列出。

### 🔴 运行时影响（4 项）

| # | 条目 | 现状 | 位置 |
|---|------|------|------|
| G1 | ~~**@EnableAsync 无自定义 Executor**~~ ✅ **已修复** — ThreadPoolConfig implements AsyncConfigurer + @EnableAsync 移至 Config 类，@Async 绑定到 communityExecutor | `CommunityApplication.java` + `ThreadPoolConfig.java` |
| G2 | ~~**Message.extra JSON→String 缺 TypeHandler**~~ ✅ **已修复** — MessageExtra POJO + @TableName(autoResultMap=true) + @TableField(typeHandler=JacksonTypeHandler.class) | `message/domain/entity/Message.java` + `MessageExtra.java` |
| G3 | ~~**WebSocketServiceImpl.handleSendMessage 桩**~~ ✅ **已修复** — WSAdapter.buildInvalidSendMsgResp() + handleSendMessage 推送 ERROR 通知客户端 | `WebSocketServiceImpl.java` + `WSAdapter.java` |
| G4 | ~~**WebSocketServiceImpl.loginSuccess hasPower = false**~~ ✅ **已修复** — uid==1 admin shortcut，后续按需扩展 per-server 角色查询 | `WebSocketServiceImpl.java:277` |

### 🟡 功能不完整（5 项）

| # | 条目 | 现状 | 位置 |
|---|------|------|------|
| G5 | **SearchServiceImpl 仍为 MySQL LIKE** — ES 搜索完全缺失（Phase 6.3） | 无 ElasticsearchConfig / MessageDocument / SearchRepository | `SearchServiceImpl.java:36` |
| G6 | **docker-compose 缺 ES 容器** — 6.3 依赖 | compose 仅 6 服务：mysql/redis/minio/rocketmq×2/app | `docker-compose.yml` |
| G7 | ~~**SearchServiceImpl + ThreadServiceImpl Reaction 残留**~~ ✅ **误报** — 两处已在前序提交中完成 reaction 批量组装（`buildReactionMap` / `buildReactionMapForMessages`），本次确认无需改动 | `SearchServiceImpl.java:55-60` + `ThreadServiceImpl.java:138-145` |
| G8 | ~~**Compose 端口与 local.properties 不同步**~~ ✅ **无问题** — compose 暴露端口 (MySQL:3308/Redis:6381/MinIO:9004/RocketMQ:9878) 与 local.properties 完全一致，Docker profile 统一是优化项非缺陷 | compose `3308:3306/6381:6379/9004:9000/9878:9878` vs `application-local.properties` |
| G9 | ~~**bind-wx 实际返回 ApiResult<UserVO>**~~ ✅ **已修复** — A9 已修正返回值类型，`AccountBindReq` DTO 存在 | `UserController.java:31` |

### ⚪ 低优先级（7 项）

| # | 条目 | 现状 |
|---|------|------|
| G10 | **6 个事件未实现** — MemberJoin/Kick/RoleUpdate/OwnershipTransfer/ServerCreate/ThreadCreate | 仅 `ChannelMessageSendEvent` 存在 |
| G11 | **WxMsg 持久化表** — 微信原始消息审计（#16，后续按需） | 无 DDL/Entity/Mapper |
| G12 | **@SecureInvoke + MQProducer** — 事务安全投递（#23，Phase 8） | 直接 RocketMQTemplate 发送 |
| G13 | **Server 实体缺 join_mode 字段** — FREE/INVITE/APPLY 三种加入模式 | 仅 INVITE 模式 |
| G14 | ~~**getDiscoverableServers 无游标分页**~~ ✅ **误报** — `ServerServiceImpl.java:121` 已使用 `CursorUtils.getCursorPageByMysql()`，返回 `CursorPageBaseResp<ServerVO>` | `ServerServiceImpl.java:121-141` |
| G15 | **ServerMemberApply 实体/VO/DDL 缺失** — 加入审批流（Phase 7） | 架构预留 |
| G16 | **mallchat-flows-visual.html 未复制** — MallChat 有，社区平台未移植 | 文档项 |

### 🔵 二次审计新增发现 (2026-06-23, 提交 6ee4354 后复核)

| # | 条目 | 严重程度 | 现状 |
|---|------|----------|------|
| N1 | ~~**WSAdapter 死代码**~~ ✅ **已修复** — MemberServiceImpl/InviteServiceImpl 注入 PushService + pushToServer；ChannelServiceImpl/ServerServiceImpl 同步补全 WS 推送 | 🟡→✅ | `PushService.pushToServer()` + 5 服务文件补全 WS 推送 |
| N2 | **MessageTypeEnum 缺 VIDEO (type=7)** — 计划标注"留待补充"，无 VideoMsgHandler | ⚪ 低 | `MessageTypeEnum.java` |
| N3 | **零测试** — 全项目 215 个 Java 文件，0 个 test 目录/测试类/冒烟脚本 | 🟡 中等 | Phase 7.1 规划中 |
| N4 | ~~**community.es.uris 配置残留**~~ ✅ **已修复** — 注释掉，标注 Phase 6.3 待 ES 容器 | ⚪→✅ | `application-local.properties:21` |

### ✅ 已确认修复/排除

| # | 条目 |
|---|------|
| — | A1/A2 getById → lambdaQuery (ImageMsgHandler + FileMsgHandler) |
| — | A3 MessageAdapter attachments 组装 (4 路径 + WS) |
| — | ExtraBody Java record → static class 序列化修复 |
| — | D2/D3 AOP 切面 + Redisson 集成 (RedissonLockAspect + FrequencyControlAspect) |
| — | D5 AbstractLocalCache (Caffeine) |
| — | B4 CursorUtils @SuppressWarnings |
| — | B5 WxMsgService @SuppressWarnings("deprecation") |
| — | G1 @Async → communityExecutor 绑定 (提交 `6ee4354`) |
| — | G2 MessageExtra + JacksonTypeHandler (提交 `6ee4354`) |
| — | G3 WS 发消息错误通知 (提交 `6ee4354`) |
| — | G4 hasPower admin shortcut (提交 `6ee4354`) |
| — | G7 确认误报 — Search/Thread 已有 reaction 组装 |
| — | G8 确认无问题 — compose 端口与 local.properties 完全一致 |
| — | G14 确认误报 — getDiscoverableServers 已有游标分页 |
| — | N1 WS 推送补全 (本次提交) |
| — | N4 死配置清理 (本次提交) |

### 📊 缺口状态总表 (16+4=20 项)

| 状态 | 数量 | 条目 |
|------|------|------|
| ✅ 已修复/排除 | **16** | G1-G4, G7, G8, G9, G14, N1, N4 + A1-A3, D2/D3, D5, B4, B5, ExtraBody |
| 🔶 Phase 6.3 | **2** | G5 (ES搜索), G6 (ES容器) |
| 🔷 Phase 7+ 规划 | **5** | G10, G13, G15 + N2 (VIDEO) + N3 (零测试) |
| ⬜ 无限期延后 | **3** | G11 (WxMsg表), G12 (@SecureInvoke), G16 (flows-visual.html) |

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
| ~~13~~ | ~~T4~~ | ~~EmojiMsgHandler~~ | ✅ 已创建 (``6ee4354``) | message/service/strategy/msg/EmojiMsgHandler.java |
| ~~14~~ | ~~T5~~ | ~~MentionParser（@提及正则 + uid 解析）~~ | ✅ 已创建 (``6ee4354``) | common/utils/MentionParser.java |
| ~~15~~ | ~~T8~~ | ~~AC 自动机敏感词~~ | ✅ 已移植 (``6ee4354``) | common/algorithm/sensitiveWord/ + common/sensitive/ 共 11 个文件 |
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

### 十二、🔴 代码质量与可复用性审计 (2026-06-23)

> 全量对比 community-platform 自身质量 + MallChat 可复用模式，识别重复代码、硬编码、性能隐患、缺失模式。

#### 十二-A: 自身代码质量问题 (CQ 系列)

| # | 类型 | 条目 | 位置 | 严重度 |
|---|------|------|------|--------|
| CQ1 | 重复代码 | **buildReactionMap 三处重复** — Reaction 聚合逻辑（按 emoji 分组、计数、判断当前用户）在 MessageServiceImpl/SearchServiceImpl/ThreadServiceImpl 各实现一遍，~30行×3 | `MessageServiceImpl.java:223-253` `SearchServiceImpl.java:69-102` `ThreadServiceImpl.java:186-219` | 🟡 |
| CQ2 | 重复代码 | **buildAttachments 两处重复** — FileAttachment 批量查询+分组逻辑在 MessageServiceImpl/MsgSendConsumer 各实现一遍 | `MessageServiceImpl.java:255-296` `MsgSendConsumer.java:82-90` | 🟡 |
| CQ3 | 重复代码 | **requireMember 三处重复** — 成员身份校验 10 行代码在 ChannelServiceImpl/ChannelPermissionServiceImpl/EmojiServiceImpl 逐字重复 | 3 个 ServiceImpl，各 ~10 行 | 🟡 |
| CQ4 | 重复代码 | **requireServerOwner 两处重复** — Server owner 校验在 ChannelPermissionServiceImpl/RoleServiceImpl 重复 | 2 个 ServiceImpl，各 ~10 行 | 🟡 |
| CQ5 | 重复代码 | **Entity→VO 转换重复** — toChannelVO/toCategoryVO + 频道分组（含"未分类"桶）在 ChannelServiceImpl/ServerServiceImpl 各自实现 | `ChannelServiceImpl.java:126-159,242-259` `ServerServiceImpl.java:157-192,307-315` | 🟡 |
| CQ6 | 硬编码 | **hasPower = uid==1L** — 管理员判断写死用户 ID 1，任何拿到 uid=1 的用户自动拥有 power | `WebSocketServiceImpl.java:278` | 🔴 |
| CQ7 | 硬编码 | **分页默认值分散** — pageSize 默认值在 MessageServiceImpl(50)/MemberServiceImpl(10)/ServerServiceImpl(30)/ThreadServiceImpl(25,50) 各自定义，无统一常量 | 4 个 ServiceImpl | 🟡 |
| CQ8 | 硬编码 | **配置未外部化** — 24h 归档阈值、MinIO region("us-east-1")、WS 端口 8091、Token TTL 5天、登录码过期 1h、文件上传 10min/下载 1h 过期、@everyone 默认权限掩码 | 8 处 | 🟡 |
| CQ9 | 硬编码 | **"@everyone" 魔法字符串** — 3 处使用裸字符串，无公共常量 | `ServerServiceImpl.java:56` `MemberServiceImpl.java:95` `InviteServiceImpl.java:111` | ⚪ |
| CQ10 | 异常处理 | **MsgSendConsumer/PushConsumer 吞异常** — catch(Exception) 仅 log，不重试不死信，消息静默丢失 | `MsgSendConsumer.java:77-79` `PushConsumer.java:34-36` | 🔴 |
| CQ11 | 异常处理 | **错误码语义错配** — TextMsgHandler 内容过长返回 MESSAGE_NOT_FOUND；SoundMsgHandler 参数无效返回 FILE_NOT_FOUND；ChannelPermission 记录不存在返回 NO_PERMISSION | `TextMsgHandler.java:27,30` `SoundMsgHandler.java:25` `ChannelPermissionServiceImpl.java:90-91` | 🟡 |
| CQ12 | 异常处理 | **BusinessException 丢失堆栈** — fillInStackTrace() 覆写为 return this，生产排查困难 | `BusinessException.java:35-37` | 🟡 |
| CQ13 | 性能 | **pushToServer O(N) 全量加载** — 10 万成员服务器全部 load 进内存 + 10 万次 MQ 发送 | `PushServiceImpl.java:63-75` | 🔴 |
| CQ14 | 性能 | **getUnreadCounts N+1** — 每个频道各发 2 次 DB 查询（readState + COUNT），50 个频道 = 101 次查询 | `ChannelReadStateServiceImpl.java:59-74` | 🟡 |
| CQ15 | 性能 | **checkPermission 4 次串行查询无缓存** — 每次消息发送都执行 member→roles→definitions→overrides 全链路 | `PermissionServiceImpl.java:28-87` | 🟡 |
| CQ16 | 性能 | **buildFileVO 每次调 MinIO** — 批量消息中每个文件都生成预签名 URL，50 条带附件消息 = 50+ MinIO HTTP 调用 | `FileServiceImpl.java:144` | 🟡 |
| CQ17 | 规范 | **注入方式不统一** — Service 用 @RequiredArgsConstructor 构造器注入，Handler 用 @Autowired 字段注入 | AbstractMsgHandler/TextMsgHandler/FileMsgHandler/ImageMsgHandler | ⚪ |
| CQ18 | 规范 | **@Scheduled 在 Service 上** — ThreadServiceImpl.autoArchiveThreads() 的 @Scheduled 在普通 Service 类上，若被 @Transactional 代理行为不可预测 | `ThreadServiceImpl.java:170` | ⚪ |
| CQ19 | 规范 | **MAX_MUM_SIZE 拼写错误** — 应为 MAXIMUM_SIZE | `WebSocketServiceImpl.java:43` | ⚪ |

#### 十二-B: MallChat 可复用模式差距 (CR 系列)

| # | 模式 | 说明 | MallChat 参考 | 优先级 |
|---|------|------|-------------|--------|
| CR1 | **@SecureInvoke 本地消息表** | MQ 发送无重试保障，MessageSendListener 发 RocketMQ 失败→消息静默丢失。MallChat 用 @SecureInvoke + secure_invoke_record 表 + @Scheduled 重试实现 at-least-once | `mallchat-transaction/` 模块 (SecureInvokeAspect + MQProducer + SecureInvokeRecord) | 🔴 |
| CR2 | **PushConsumer BROADCASTING** | 当前用 RocketMQListener<String> 手动 JSON 解析，未用 BROADCASTING 模式。多实例部署时消息重复消费 | MallChat PushConsumer 用 `messageModel = BROADCASTING` + `RocketMQListener<PushMessageDTO>` | 🟡 |
| CR3 | **完整事件系统** | 社区平台仅 1 个 Event (ChannelMessageSendEvent)，MallChat 有 10+ (UserOnline/Offline/Register/Black/Apply/GroupMemberAdd/MessageMark/Recall 等)。WebSocketServiceImpl 直接处理在线/离线逻辑，未通过事件解耦 | `mallchat-chat-server/.../common/event/` 目录 10+ 事件 | 🟡 |
| CR4 | **URL 链接预览** | 文本消息中的 URL 无预览卡片。MallChat 有 UrlDiscover 策略链 (通用URL + 微信特化 WxUrlDiscover + 优先级选择 PrioritizedUrlDiscover)，发消息时自动抓取标题 | `common/utils/discover/` 目录 (UrlDiscover + AbstractUrlDiscover + WxUrlDiscover + CommonUrlDiscover) | 🟡 |
| CR5 | **BatchCache 缓存框架** | AbstractRedisStringCache + AbstractLocalCache 刚被清理删除，但 PermissionService.checkPermission() 等高频操作急需 Caffeine 本地缓存 + Redis 二级缓存 | MallChat `common/service/cache/` 目录 (BatchCache + AbstractRedisStringCache + AbstractLocalCache) | 🟡 |
| CR6 | **MsgMark 策略模式** | Reaction 功能用 inline 逻辑，未用策略模式。后续扩展自定义 emoji/reaction 策略时需重构 | MallChat `chat/service/strategy/mark/` (MsgMarkFactory + AbstractMsgMarkStrategy + LikeStrategy + DisLikeStrategy) | ⚪ |
| CR7 | **AbstractMsgHandler 泛型** | 社区版 AbstractMsgHandler 丢失 MallChat 的 `<Req>` 泛型参数，无法做类型安全的消息体反序列化 | MallChat `AbstractMsgHandler<Req>` 泛型 + `toBean()` | ⚪ |
| CR8 | **WS 扇出线程池** | WebSocketServiceImpl.sendToAllOnline/sendToUid 同步遍历，阻塞 Netty EventLoop。MallChat 每次 send 包 threadPoolTaskExecutor.execute() | MallChat WebSocketServiceImpl 使用 executor 异步发送 | ⚪ |
| CR9 | **敏感词 AC 过滤器** | 当前仅有 DFAFilter，MallChat 额外有 ACFilter + ACProFilter，大数据量下 AC 自动机性能优于 DFA | MallChat `algorithm/sensitiveWord/ACFilter.java` + `ACProFilter.java` | ⚪ |
| CR10 | **错误码按域分离** | 社区平台全部错误码在 BusinessErrorEnum，MallChat 按 CommonErrorEnum/BusinessErrorEnum/GroupErrorEnum/HttpErrorEnum 分域管理 | MallChat 4 个 ErrorEnum 分域 | ⚪ |

### 十三、社区平台优于 MallChat 的设计 (已实现，无需行动)

| # | 设计 | 说明 |
|---|------|------|
| ✅ | **RBAC 权限系统** | 13 位权限位掩码 + 角色层级 position + 频道级 allow/deny 覆盖，远超 MallChat 的 ADMIN/CHAT_MANAGER 二元角色 |
| ✅ | **Thread 话题系统** | MallChat 完全没有，含自动归档 @Scheduled |
| ✅ | **频道已读追踪** | channel_read_state 表 + 未读计数 API，MallChat 无此能力 |
| ✅ | **WS 频道/Thread 订阅** | 比 MallChat 的全局广播更细粒度，Redis Set 存储订阅关系 |
| ✅ | **WS Token 提取** | HttpHeadersHandler 在 HTTP 升级阶段提取 token，比 MallChat 的 WS AUTHORIZE 消息更干净 |
| ✅ | **MentionParser** | @username/@all/@everyone/@所有人 文本解析，MallChat 靠请求体传 atUidList | |

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

## 代码质量与可复用性审计 (2026-06-23)

> 全量审查 community-platform 215 个 Java 文件 + 对比 MallChat 设计模式，识别重复代码、硬编码、性能隐患、缺失模式。

### 一、重复代码（5 处）

#### CQ1: buildReactionMap 三处重复

完全相同的算法——按 emoji 分组、COUNT、GROUP_CONCAT user_id、当前用户是否已反应——在三个 ServiceImpl 独立实现，每处 ~30 行：

| 文件 | 方法 | 行号 |
|------|------|------|
| `MessageServiceImpl.java` | `buildReactionMap()` | 223-253 |
| `SearchServiceImpl.java` | `buildReactionMap()` | 69-102 |
| `ThreadServiceImpl.java` | `buildReactionMapForMessages()` | 186-219 |

SearchServiceImpl 版本多了一个 `RequestHolder.get()` null 守卫，其余两份没有——行为不一致的隐患。**建议**：提取到 `MessageAdapter.buildReactionMap(List<Long> msgIds)`。

#### CQ2: buildAttachments 两处重复

FileAttachment 批量查询 + `Map<Long, List<FileVO>>` 分组逻辑在 `MessageServiceImpl:255-296` 和 `MsgSendConsumer:82-90` 独立实现，结构完全相同。

#### CQ3: requireMember 三处重复

```java
ServerMember member = memberDao.lambdaQuery()
    .eq(ServerMember::getServerId, serverId)
    .eq(ServerMember::getUserId, userId)
    .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getCode()).one();
if (member == null) { throw new BusinessException(BusinessErrorEnum.NOT_MEMBER); }
```

在 `ChannelServiceImpl:219-229`、`ChannelPermissionServiceImpl:113-123`、`EmojiServiceImpl:84-93` 逐字重复。**建议**：提取到 `server/service/MembershipValidator`。

#### CQ4: requireServerOwner 两处重复

同在 `ChannelPermissionServiceImpl:101-111` 和 `RoleServiceImpl:160-170`。

#### CQ5: Entity→VO 转换重复

`toChannelVO()`/`toCategoryVO()` + 频道分组（含"未分类"桶）在 `ChannelServiceImpl:126-159,242-259` 和 `ServerServiceImpl:157-192,307-315` 各自实现。

---

### 二、硬编码值

#### 严重 (CQ6)

**`hasPower = uid==1L`** — `WebSocketServiceImpl.java:278`。管理员判断写死用户 ID 1，应查角色权限。

#### 中等 (CQ7-CQ8)

| 位置 | 硬编码值 | 应及时 |
|------|---------|--------|
| 4 个 ServiceImpl | pageSize 默认值 10/25/30/50 各不相同 | 统一常量 |
| `ThreadServiceImpl:172` | `minusHours(24)` | `@Value` 配置化 |
| `MinIOConfiguration:18` | `region("us-east-1")` | `@Value` 或 OssProperties |
| `NettyWebSocketServer:29` | `WEB_SOCKET_PORT = 8091` | 外部化到配置 |
| `AuthServiceImpl:26` | `TOKEN_TTL = TimeUnit.DAYS.toSeconds(5)` | `@Value` 配置化 |
| `WebSocketServiceImpl:39-43` | `EXPIRE_TIME = 1h`, `MAX_MUM_SIZE = 10000` | 配置化 + 拼写修正 |
| `ServerServiceImpl:56-62` | `@everyone` 默认权限掩码硬编码 OR 运算 | `application.yml` 模板 |
| `FileServiceImpl:28-29` | `UPLOAD_EXPIRE_MINUTES = 10`, `DOWNLOAD_EXPIRE_HOURS = 1` | 移入 OssProperties |

#### 轻微 (CQ9)

`"@everyone"` 魔法字符串在 3 处使用裸字符串，无 `EVERYONE_ROLE_NAME` 公共常量。

---

### 三、异常处理缺陷

#### CQ10: Consumer 吞异常（严重）

`MsgSendConsumer.java:77-79`、`PushConsumer.java:34-36` — `catch(Exception)` 仅 log，不重试不死信，消息静默丢失。

#### CQ11: 错误码语义错配

| 位置 | 场景 | 返回错误码 | 应返回 |
|------|------|-----------|--------|
| `TextMsgHandler:27,30` | content 空白/超 4000 字符 | `MESSAGE_NOT_FOUND` | `MESSAGE_CONTENT_INVALID` |
| `FileMsgHandler:35` | 文件未上传 | `FILE_UPLOAD_FAILED` | `FILE_NOT_UPLOADED` |
| `SoundMsgHandler:25` | 语音参数无效 | `FILE_NOT_FOUND` | `SOUND_MSG_INVALID` |
| `ChannelPermissionServiceImpl:90-91` | 权限记录不存在 | `NO_PERMISSION` | `PERMISSION_RECORD_NOT_FOUND` |

#### CQ12: BusinessException 零堆栈

`BusinessException.java:35-37` 覆写 `fillInStackTrace()` 为 `return this`，业务异常无堆栈，生产排查只能靠 grep。

---

### 四、性能隐患

#### CQ13: pushToServer 全量加载（严重）

`PushServiceImpl.java:63-75`：10 万成员服务器 → 10 万条 Member load 进内存 + 10 万次 MQ 发送。应改游标分页 + 批量 MQ。

#### CQ14: getUnreadCounts N+1

`ChannelReadStateServiceImpl.java:59-74`：每个频道各发 readState + COUNT 查询。50 个频道 = **101 次 DB 往返**。应改为单条 JOIN SQL。

#### CQ15: checkPermission 4 次串行查询无缓存

`PermissionServiceImpl.java:28-87`：每次消息发送 → member → roles → definitions → overrides，全无缓存。应加 Caffeine 本地缓存（TTL 30s）。

#### CQ16: buildFileVO 每次调 MinIO

`FileServiceImpl.java:144`：批量消息中每个文件的 VO 都生成预签名 URL，50 条消息各带附件 = 50+ MinIO HTTP 调用。应改为客户端按需请求 `/files/{id}/download`。

---

### 五、MallChat 可复用但未移植的模式

#### 高优先级（有可靠性/性能风险）

| # | 模式 | 风险 | MallChat 参考 |
|---|------|------|-------------|
| **CR1** | **@SecureInvoke 本地消息表** | MQ 发送无重试→消息丢失(at-most-once) | `mallchat-transaction/` 模块 5 个文件 ~300 行 |
| **CR2** | **PushConsumer BROADCASTING** | 多实例消息重复消费 + 手动 JSON 解析 | MallChat `messageModel = BROADCASTING` + 类型化 DTO |

#### 中优先级（扩展性提升）

| # | 模式 | 收益 | 文件量 |
|---|------|------|--------|
| **CR3** | **完整事件系统** | 解耦 WS 连接管理 vs DB/Cache 操作 | 10+ Event + Listener |
| **CR4** | **URL 链接预览** | 文本消息中 URL 自动生成预览卡片 | 6 文件 (UrlDiscover 策略链) |
| **CR5** | **BatchCache 缓存框架** | CQ15 直接受益（PermissionService 加缓存） | 3 文件（已删除，可从 MallChat 恢复） |

#### 低优先级（锦上添花）

| # | 模式 | 何时补 |
|---|------|--------|
| CR6 | MsgMark 策略模式 | 新增 Reaction 类型时 |
| CR7 | AbstractMsgHandler `<Req>` 泛型 | 重构消息处理链时 |
| CR8 | WS 扇出加线程池 | 大服务器场景 |
| CR9 | 敏感词 AC/ACPro 过滤器 | 敏感词库 > 1 万条 |
| CR10 | 错误码按域分离 (Common/Business/Group/Http) | 新增模块时 |

---

### 六、社区平台优于 MallChat 的设计（已实现，无需行动）

| 设计 | 说明 |
|------|------|
| RBAC 权限系统 | 13 位权限位掩码 + 角色层级 + 频道级 allow/deny 覆盖 |
| Thread 话题系统 | MallChat 完全没有，含自动归档 |
| 频道已读追踪 | channel_read_state 表 + 未读计数 API |
| WS 频道/Thread 订阅 | Redis Set 存储订阅关系，比 MallChat 全局广播更细粒度 |
| WS Token 提取 | HttpHeadersHandler 在 HTTP 升级阶段提取，比 MallChat 的 WS AUTHORIZE 消息更干净 |
| MentionParser | @username/@all/@everyone 文本解析 |

---

### 汇总

| 类别 | 条目数 | 🔴 严重 | 🟡 中等 | ⚪ 轻微 |
|------|--------|---------|---------|---------|
| 重复代码 | 5 (CQ1-CQ5) | — | 5 | — |
| 硬编码 | 3 类 (CQ6-CQ9) | 1 | 1 | 1 |
| 异常处理 | 3 (CQ10-CQ12) | 1 | 2 | — |
| 性能 | 4 (CQ13-CQ16) | 1 | 3 | — |
| MallChat 差距 | 10 (CR1-CR10) | 1 | 4 | 5 |
| **合计** | **25** | **4** | **15** | **6** |

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-22 | 创建 2 号日志，续接 dev-log.md | `a08329a` |
| 2026-06-22 | 完整待办汇总（29 项）+ T7 纠错 | `0cfd4ea` |
| 2026-06-22 | **打通推送管线 6 项** — MessageSendListener → MQ → MsgSendConsumer → PushService → PushConsumer → pushToChannel/Thread | `36bc4be` |
| 2026-06-22 | **Phase 4.x 遗留 + Phase 5 未完成 6 项** — autoArchive + Reaction 残留 + Typing + Online/Offline + WSAdapter 全 18 类型 | `7af4244` |
| 2026-06-23 | **Backlog: 敏感词过滤系统 + EmojiMsgHandler** — DFAFilter + ACTrie + SensitiveWordBs + MyWordFactory + TextMsgHandler 集成 + MessageTypeEnum.EMOJI | `6ee4354` |
| 2026-06-23 | **Backlog: MentionParser + JsonUtils + FutureUtils** — @提及正则解析器 + Jackson 工具类 + CompletableFuture 工具集 | `6ee4354` |
| 2026-06-23 | **Phase 6: MinIOConfiguration + FileServiceImpl** — OssProperties 配置绑定 + MinioClient Bean + 预签名上传/确认/下载 三方法实现 | `6ee4354` |
| 2026-06-23 | **审计修复 (A1-A11)** — 端口同步 + SQL注入修复 + FileController.getFile + Server分页 + 文件上传校验 + WS端口外部化 + bind-wx返回值 + @Mapper/@TableId补完 | `6ee4354` |
| 2026-06-23 | **代码质量与可复用性审计** — 识别 25 项问题 (CQ1-CQ16 + CR1-CR10)：重复代码 5 处、硬编码 8+ 处、异常处理 3 项、性能隐患 4 项、MallChat 可复用模式 10 项 | `7fff1c6` |
| 2026-06-23 | **API 全量测试 + 运行时修复 (A19-A22)** — MinIO region+bucket 自动创建 + FileServiceImpl getById→lambdaQuery + FileVO.status + PENDING 文件 GET 修复 | `6ee4354` |
| 2026-06-23 | **Plan-vs-Code 全量审计** — API 46/46 端点比对通过 + 9 处结构差异 (D1-D9) + 计划文档 checkbox 同步 + 新增待办 30-32 (AOP切面/Redisson/Caffeine) | `6ee4354` |
| 2026-06-23 | **Round 1: Bug 修复 (A1-A3)** — ImageMsgHandler/FileMsgHandler getById→lambdaQuery + MessageAdapter attachments 组装 (4 路径) | `6ee4354` |
| 2026-06-23 | **Round 2: Redisson 分布式锁 (B1)** — RedisConfig + SpElUtils + RedissonLockAspect + LockService + spring-boot-starter-aop | `6ee4354` |
| 2026-06-23 | **Round 3: 频控 AOP (B2)** — FrequencyControlAspect + TotalCountWithInFixTime + 策略工厂 + RedisUtils.inc(TTL) | `6ee4354` |
| 2026-06-23 | **Round 4: 收尾 (B3-B5)** — AbstractLocalCache + CursorUtils @SuppressWarnings + WxMsgService @SuppressWarnings | `6ee4354` |
| 2026-06-23 | **Bug 修复: ExtraBody 序列化** — ImageMsgHandler/FileMsgHandler 中 Java `record` 改为 static class（Hutool JSONUtil 无法序列化 record 导致 extra 为 `{}`） | `f5f20cc` |
| 2026-06-23 | **Phase 0-6 功能缺口审计** — 对比计划+代办 vs 代码，新增 G1-G16 缺口清单（4 运行时 + 5 功能不完整 + 7 低优先级），确认 7 项已修复 | `f5f20cc` |
| 2026-06-23 | **G1: @Async 绑定自定义 Executor** — ThreadPoolConfig implements AsyncConfigurer + @EnableAsync 移至 Config 类，@Async 回退到 communityExecutor 代替 SimpleAsyncTaskExecutor | `6ee4354` |
| 2026-06-23 | **G3: WS handleSendMessage 错误通知** — WSAdapter.buildInvalidSendMsgResp() + handleSendMessage 推送 ERROR 响应，客户端不再静默失败 | `6ee4354` |
| 2026-06-23 | **G4: hasPower admin shortcut** — loginSuccess 改为 uid==1→hasPower=true，替代硬编码 false | `6ee4354` |
| 2026-06-23 | **G2: JSON TypeHandler for Message.extra** — MessageExtra POJO + @TableName(autoResultMap=true) + @TableField(typeHandler=JacksonTypeHandler.class)，移除 5 处手动 JSONUtil 序列化/反序列化 + 删除 ExtraBody inner class | `6ee4354` |
| 2026-06-23 | **G7 确认: 已修复** — SearchServiceImpl/ThreadServiceImpl 已在先前提及提交中完成 reaction 批量组装，本次确认无需改动 | `6ee4354` |
| 2026-06-23 | **二次审计: G8/G14 纠正 + 新增 N1-N4** — G8 端口确认一致、G14 游标分页确认存在（误报）；新增 N1(WSAdapter死代码)、N2(VIDEO缺失)、N3(零测试)、N4(ES uris残留) | `62c916f` |
| 2026-06-23 | **细小问题修复: WS 推送补全** — PushService.pushToServer() + MemberServiceImpl (join/leave/kick) + ChannelServiceImpl (create/update/delete) + ServerServiceImpl (update) + InviteServiceImpl (joinByInvite) WS 通知 | `04a5877` |
| 2026-06-23 | **细小问题修复: WSRespTypeEnum + WSAdapter** — 新增 MEMBER_KICK(32) + buildMemberKick() | `04a5877` |
| 2026-06-23 | **细小问题修复: 死配置清理** — application-local.properties 注释 community.es.uris | `04a5877` |
| 2026-06-23 | **细小问题修复: 计划文档同步** — 更新 P4-6 (reaction 残留) 为已修复 | `04a5877` |

---

## 参考

- 前篇开发日志: `docs/dev-log.md`
- 项目计划: `docs/plan/`
- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
