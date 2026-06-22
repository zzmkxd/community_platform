# 十、开发阶段划分 — Phase 0~4

> Phase 5-8 → [06-dev-phases-5-8.md](./06-dev-phases-5-8.md)

每个 Phase 结束条件：`./mvnw clean compile` 通过 + 本阶段 API 可 curl 验证。

---

### Phase 0: 起点准备

**目标**：空目录 → 可编译的空项目 + 基础设施全部就绪

#### 0.1 项目骨架
- [ ] 0.1.1 创建目录 `E:\Learn_zone\Code_zone\IDEA_code\community-platform\`
- [ ] 0.1.2 创建根 `pom.xml`：SB 3.3.5 parent + Spring Cloud BOM + 版本属性
- [ ] 0.1.3 创建 `community-common/` 模块（pom.xml + 目录结构）
- [ ] 0.1.4 创建 `community-transaction/` 模块（pom.xml + 目录结构）
- [ ] 0.1.5 创建 `community-server/` 模块（pom.xml + 启动类 + application.yml）
- [ ] 0.1.6 创建 `.gitignore` + `README.md`
- [ ] 0.1.7 初始化 Git 仓库，首次提交
- [ ] 0.1.8 验证：`./mvnw clean compile` → BUILD SUCCESS

#### 0.2 基础设施
- [ ] 0.2.1 创建 `docker-compose.yml`（MySQL 3307 + Redis 6380 + RocketMQ + MinIO + ES）
- [ ] 0.2.2 创建 `broker.conf`（RocketMQ Broker 配置）
- [ ] 0.2.3 创建 `docs/schema.sql`（13 张表完整 DDL，utf8mb4）
- [ ] 0.2.4 创建 `application-docker.yml`（Docker profile 配置）
- [ ] 0.2.5 验证：`docker compose up -d` → 基础设施全部 healthy

**MallChat 参考**：根 pom.xml 结构、docker-compose.yml 写法、broker.conf

---

### Phase 1: 基础设施层（共享库）

**目标**：从 MallChat 移植经过验证的工具类和配置到新项目的共享模块

#### 1.1 community-common — 配置类
- [ ] 1.1.1 `RedisConfig.java` — Redisson @Value 注入方式
- [ ] 1.1.2 `MybatisPlusConfig.java` — 分页插件 + 动态表名（预留）
- [ ] 1.1.3 `ThreadPoolConfig.java` — mallchatExecutor + websocketExecutor
- [ ] 1.1.4 `RestTemplateConfig.java` — HTTP 客户端（Phase 2 Feign 前使用）
- [ ] 1.1.5 验证：Spring 容器启动无 Bean 创建错误

#### 1.2 community-common — 工具类
- [ ] 1.2.1 `JwtUtils.java` — auth0 java-jwt，createToken/verifyToken
- [ ] 1.2.2 `RedisUtils.java` — String/Hash/Set/ZSet 封装
- [ ] 1.2.3 `CursorUtils.java` — 光标分页工具（从 MallChat 照搬）
- [ ] 1.2.4 `RequestHolder.java` — ThreadLocal 持有 uid + IP
- [ ] 1.2.5 验证：编译通过，工具类方法签名可用

#### 1.3 community-common — 拦截器 + 异常
- [ ] 1.3.1 `TokenInterceptor.java` — JWT 校验 + Redis Token 验证
- [ ] 1.3.2 `CollectorInterceptor.java` — 请求耗时统计
- [ ] 1.3.3 `GlobalExceptionHandler.java` — @RestControllerAdvice
- [ ] 1.3.4 `BusinessException.java` + `HttpErrorEnum.java`
- [ ] 1.3.5 `WebMvcConfig.java` — 注册拦截器 + CORS
- [ ] 1.3.6 验证：无认证请求返回 401

#### 1.4 community-common — 注解 + AOP
- [ ] 1.4.1 `@RedissonLock` — 分布式锁注解
- [ ] 1.4.2 `@FrequencyControl` — 频控注解（移植 MallChat 三层窗口策略）
- [ ] 1.4.3 验证：编译通过

#### 1.5 community-common — 基础类
- [ ] 1.5.1 `ApiResult<T>` — 统一返回体
- [ ] 1.5.2 `CursorPageBaseReq` / `CursorPageBaseResp` — 分页基类
- [ ] 1.5.3 `WSBaseReq` / `WSBaseResp<T>` — WebSocket 信封
- [ ] 1.5.4 `WSReqTypeEnum` / `WSRespTypeEnum` — WS 协议枚举（含社区扩展）
- [ ] 1.5.5 `BaseEntity` — (id, createTime, updateTime)
- [ ] 1.5.6 验证：全模块编译通过

#### 1.6 community-common — 缓存框架
- [ ] 1.6.1 `AbstractRedisStringCache.java` — 批量缓存抽象（移植）
- [ ] 1.6.2 `AbstractLocalCache.java` — Caffeine 本地缓存（可选）
- [ ] 1.6.3 验证：编译通过

#### 1.7 community-transaction
- [ ] 1.7.1 `@SecureInvoke` 注解 + `SecureInvokeAspect` 切面
- [ ] 1.7.2 `SecureInvokeRecord` 实体 + `SecureInvokeRecordDao`
- [ ] 1.7.3 `MQProducer.java` — 事务安全 RocketMQ 发送
- [ ] 1.7.4 验证：`./mvnw clean compile` 全模块通过

**MallChat 参考**：`mallchat-common-starter/` 下的所有 config、interceptor、utils、cache、exception 类，`mallchat-transaction/` 全部内容

---

### Phase 2: 用户模块（seed data 登录 + 微信扫码）

**目标**：跑通用户登录鉴权全链路。主路径为微信 OAuth 扫码；用户名/密码仅 seed data 测试通道（DDL 预置账号）。

> 设计决策 (2026-06-22)：删除注册端点。新用户只通过微信扫码自动创建。详见[[login-design-decision]]。

#### 2.1 数据库
- [ ] 2.1.1 执行 `user` 表 DDL（从 schema.sql 截取）
- [ ] 2.1.2 确认表结构：id, username(NULLABLE), password(NULLABLE), email(NULLABLE), open_id(UK NULLABLE), union_id(NULLABLE), nickname, avatar, sex, status, create_time, update_time
- [ ] 2.1.3 DDL 预置 seed data：3~5 个 BCrypt 测试账号

#### 2.2 DAO 层
- [ ] 2.2.1 `UserMapper.java` — extends BaseMapper<User>
- [ ] 2.2.2 `UserDao.java` — extends ServiceImpl<UserMapper, User>，按 username/openId 查询

#### 2.3 Entity
- [ ] 2.3.1 `User.java` — @TableName("user")，字段映射

#### 2.4 Service（seed data 登录 + 微信登录共用）
- [ ] 2.4.1 `AuthService.login(LoginReq)` — 查 User by username → BCrypt 验证 → JWT → Redis
- [ ] 2.4.2 `AuthService.login(Long uid)` — 微信扫码后直接按 uid 签发 token
- [ ] 2.4.3 `AuthService.refresh()` — 验证旧 Token → 签发新 Token
- [ ] 2.4.4 `AuthService.verify()` — JWT 验签 + Redis token 值比对
- [ ] 2.4.5 `AuthService.getValidUid()` — Token 有效返回 uid
- [ ] 2.4.6 `AuthService.renewalTokenIfNecessary()` — @Async 异步续期
- [ ] 2.4.7 `UserService.getMe()` — 查当前用户信息
- [ ] 2.4.8 `UserService.updateMe()` — 更新昵称/头像/邮箱
- [ ] 2.4.9 `UserService.getById()` — 公开查询用户

#### 2.5 VO/DTO
- [ ] 2.5.1 `LoginReq` / `LoginResp`
- [ ] 2.5.2 `UserVO` (id, username, nickname, avatar, email, openId, createTime)
- [ ] 2.5.3 `UserAdapter` — User entity ↔ UserVO 转换

#### 2.6 Controller
- [ ] 2.6.1 `AuthController` — /api/v1/auth/login (POST), /api/v1/auth/refresh (POST)
- [ ] 2.6.2 `UserController` — /api/v1/users/me (GET+PUT), /api/v1/users/{id} (GET)

#### 2.7 验证
- [ ] 2.7.1 `curl POST /api/v1/auth/login` (seed data 账号) → 返回 JWT + userId
- [ ] 2.7.2 无 Token 访问 /api/v1/users/me → 401
- [ ] 2.7.3 带 Token 访问 /api/v1/users/me → 200 + UserVO
- [ ] 2.7.4 `./mvnw clean compile` ✅

**MallChat 参考**：`LoginServiceImpl.java`（JWT 签发）、`TokenInterceptor.java`（鉴权链路）、`JwtUtils.java`（auth0 java-jwt）

---

### Phase 3: 社群模块（Server/Category/Channel/RBAC）

**目标**：实现 Server → Category → Channel 的完整嵌套 CRUD + 权限体系。这是整个项目最复杂的模块。

#### 3.1 数据库（9 张表）
- [ ] 3.1.1 `server` 表 DDL
- [ ] 3.1.2 `category` 表 DDL
- [ ] 3.1.3 `channel` 表 DDL
- [ ] 3.1.4 `server_member` 表 DDL
- [ ] 3.1.5 `role` 表 DDL
- [ ] 3.1.6 `member_role` 表 DDL
- [ ] 3.1.7 `channel_permission` 表 DDL
- [ ] 3.1.8 `emoji` 表 DDL
- [ ] 3.1.9 `invite` 表 DDL（code UK + server_id + inviter_id + max_uses + used_count + expire_time）

#### 3.2 Entity + Mapper + DAO（9 组）
- [ ] 3.2.1 Server Entity + Mapper + Dao
- [ ] 3.2.2 Category Entity + Mapper + Dao
- [ ] 3.2.3 Channel Entity + Mapper + Dao
- [ ] 3.2.4 ServerMember Entity + Mapper + Dao
- [ ] 3.2.5 Role Entity + Mapper + Dao
- [ ] 3.2.6 MemberRole Entity + Mapper + Dao
- [ ] 3.2.7 ChannelPermission Entity + Mapper + Dao
- [ ] 3.2.8 Emoji Entity + Mapper + Dao
- [ ] 3.2.9 Invite Entity + Mapper + Dao

#### 3.3 Server/Category/Channel CRUD
- [ ] 3.3.1 `ServerService.createServer()` — 建 server + 设 owner 角色 + 添加 @everyone 默认角色
- [ ] 3.3.2 `ServerService.getMyServers()` — 用户加入的服务器列表
- [ ] 3.3.3 `ServerService.getServerDetail()` — 服务详情含 categories + channels 嵌套
- [ ] 3.3.4 `ServerService.getDiscoverableServers()` — 公开可加入服务器（join_mode=FREE，光标分页）
- [ ] 3.3.5 `ServerService.updateServer()` — 仅 owner/admin
- [ ] 3.3.6 `ServerService.deleteServer()` — 仅 owner（软删除）
- [ ] 3.3.7 `CategoryService` — create/update/delete/list
- [ ] 3.3.8 `ChannelService` — create / update / delete / getSingle / getNestedList（按 category 分组）
- [ ] 3.3.9 `MemberService.joinServer()` — 加入服务器，分配 @everyone 角色
- [ ] 3.3.10 `MemberService.leaveServer()` — 离开（owner 不可离开，需先转让或删除）
- [ ] 3.3.11 `MemberService.kickMember()` — 踢出（需 KICK_MEMBERS 权限）
- [ ] 3.3.12 `MemberService.getMemberList()` — 光标分页
- [ ] 3.3.13 `MemberService.updateNickname()` — 服务器内昵称
- [ ] 3.3.14 `MemberService.transferOwnership()` — 转让 owner（仅当前 owner，目标必须是成员）

#### 3.3b 邀请管理
- [ ] 3.3.15 `InviteService.createInvite()` — 生成邀请链接（code + 过期时间 + 最大使用次数）
- [ ] 3.3.16 `InviteService.joinByInvite()` — 通过邀请码加入服务器（校验有效性 → 创建 member → 分配 @everyone）

#### 3.4 角色管理
- [ ] 3.4.1 `RoleService.createRole()` — 创建自定义角色（name + color + permissions + position）
- [ ] 3.4.2 `RoleService.getRoles()` — 服务器所有角色列表
- [ ] 3.4.3 `RoleService.updateRole()` — 编辑角色
- [ ] 3.4.4 `RoleService.deleteRole()` — 删除角色（须清理关联）
- [ ] 3.4.5 `RoleService.assignRole()` — 为用户分配角色
- [ ] 3.4.6 `RoleService.removeRole()` — 移除用户角色

#### 3.5 权限系统
- [ ] 3.5.1 `PermissionBit.java` 枚举 — 13 个权限位定义 + bitmask 常量
- [ ] 3.5.2 `PermissionService.resolvePermission()` — 核心算法：角色位 OR → 管理员短路 → 频道覆盖
- [ ] 3.5.3 `PermissionService.checkPermission()` — 单次鉴权
- [ ] 3.5.4 `ChannelPermissionService.setOverwrite()` — 设置频道权限覆盖
- [ ] 3.5.5 `ChannelPermissionService.getOverwrites()` — 查频道权限覆盖列表
- [ ] 3.5.6 `ChannelPermissionService.deleteOverwrite()` — 移除覆盖
- [ ] 3.5.7 单元测试：权限解析算法（角色 OR、deny 高于 allow、用户覆盖高于角色覆盖）

#### 3.6 表情管理
- [ ] 3.6.1 `EmojiService.uploadEmoji()` — 上传自定义表情至 MinIO
- [ ] 3.6.2 `EmojiService.listEmojis()` — 服务器表情列表
- [ ] 3.6.3 `EmojiService.deleteEmoji()` — 删除自定义表情

#### 3.7 VO/Adapter
- [ ] 3.7.1 ServerVO, CategoryVO, ChannelVO, MemberVO, RoleVO, EmojiVO, InviteVO, ChannelPermissionVO
- [ ] 3.7.2 ServerAdapter, CategoryAdapter, ChannelAdapter, MemberAdapter, RoleAdapter, InviteAdapter

#### 3.8 Controller（8 个）
- [ ] 3.8.1 `ServerController`
- [ ] 3.8.2 `CategoryController`
- [ ] 3.8.3 `ChannelController`
- [ ] 3.8.4 `MemberController`
- [ ] 3.8.5 `RoleController`
- [ ] 3.8.6 `ChannelPermissionController`
- [ ] 3.8.7 `EmojiController`
- [ ] 3.8.8 `InviteController`

#### 3.9 验证
- [ ] 3.9.1 注册用户 A → 创建服务器 → 验证 owner 角色自动分配
- [ ] 3.9.2 创建分类 → 创建频道 → GET 服务器详情 → 返回嵌套结构
- [ ] 3.9.3 用户 B 注册 → 加入服务器 → 验证得到 @everyone 角色
- [ ] 3.9.4 创建自定义角色 → 分配权限 → 赋给用户 B → 权限检查通过
- [ ] 3.9.5 频道级 deny 覆盖 → 用户在该频道发消息权限被拒绝
- [ ] 3.9.6 无权限用户尝试删除频道 → 403
- [ ] 3.9.7 浏览公开服务器 → `GET /api/v1/servers/discover` → CursorPage<ServerVO>
- [ ] 3.9.8 转让 owner → 原 owner 降级，新 owner 获得所有权限
- [ ] 3.9.9 创建邀请链接 → 新用户通过邀请加入 → 校验 code → 200 MemberVO
- [ ] 3.9.10 Git 提交：`feat(phase3): Server/Category/Channel CRUD + RBAC 权限系统 + 邀请`

#### 3.10 实现差异记录 (2026-06-22)

> 以下为方案文档与代码实现之间的差异，已逐一评审并给出决议。

##### 行为差异（已修复）

| # | 问题 | 修复 |
|---|------|------|
| 1 | `createServer` 只建 @everyone，未建 Owner 角色 | 新增 "Owner" 角色（ADMINISTRATOR 权限，position=999），创建者同时获得 Owner + @everyone |
| 2 | `updateServer` 只检查 owner，admin 不可操作 | 改为 `PermissionService.checkPermission(ADMINISTRATOR)` 放行 |
| 3 | 缺少 `entity/vo/ServerMemberApply.java` + 对应 DDL | **留待 Phase 7**（加入审批） |
| 4 | `leaveOrKick` 中 kick 只检查 owner 而非 KICK_MEMBERS | 已改为 RBAC `checkPermission(KICK_MEMBERS)` |
| 5 | `leaveOrKick` 中 owner 可自行离开 | 已阻止，owner 须先转让 |
| 6 | Emoji 3 个方法全是 stub | 已实现基础 CRUD（MinIO 上传留待 Phase 4+） |

##### API 路径对齐

| # | 方案路径 | 代码路径 | 决议 |
|---|---------|---------|------|
| 7 | `GET /api/v1/servers/discover` | `GET /api/v1/servers/discoverable` | 统一为 `/discover` |

##### 命名风格对齐（保持代码现状，功能等价）

| # | 方案方法名 | 代码方法名 | 决议 |
|---|-----------|----------|------|
| 8 | `CategoryService`（独立） | category 方法在 `ChannelService` | 与 04-migration.md 一致，保持现状 |
| 9 | `ChannelService.getSingle` | `getChannel` | 保持现状（mallchat 风格） |
| 10 | `ChannelService.getNestedList` | `getChannels` | 保持现状 |
| 11 | `MemberService.getMemberList` | `getMembers` | 保持现状 |
| 12 | `RoleService.assignRole`（单数） | `assignRoles`（批量） | 保持现状（支持批量） |
| 13 | `ChannelPermissionService.setOverwrite` | `upsertPermission` | 保持现状 |
| 14 | `ChannelPermissionService.getOverwrites` | `getPermissions` | 保持现状 |
| 15 | `ChannelPermissionService.deleteOverwrite` | `deletePermission` | 保持现状 |
| 16 | `EmojiService.listEmojis` | `getEmojis` | 保持现状 |
| 17 | `PermissionService.resolvePermission` (独立) | 内联在 `checkPermission` | 保持现状（功能等价） |

##### 结构简化（功能等价，后续按需补）

| # | 方案要求 | 代码现状 | 决议 |
|---|---------|---------|------|
| 18 | 独立 Adapter 类 6 个 | Service 内 private `toXxxVO()` | 保持现状，Phase 4+ 可提取 |
| 19 | 权限算法单元测试 (3.5.7) | 未写 | Phase 3.9 集成验证时补 |
| 20 | Server 实体 `join_mode` 字段 | 未加 | 当前仅 invite 加入，无需区分 |
| 21 | `getDiscoverableServers` 光标分页 | 全量列表 | 简化实现，数据量小时无差异 |

---

### Phase 4: 消息模块（Message/Thread/Reaction）

**目标**：实现消息发送 → 持久化 → 推送的完整链路，含 Thread 和 Reaction。复用 MallChat 策略模式。

#### 4.1 数据库（4 张表）
- [ ] 4.1.1 `message` 表 DDL（已建）
- [ ] 4.1.2 `thread` 表 DDL（已建）
- [ ] 4.1.3 `reaction` 表 DDL（已建）
- [ ] 4.1.4 `channel_read_state` 表 DDL（已建，Phase 3 DDL 已含）

#### 4.2 Entity + Mapper + DAO
- [ ] 4.2.1 Message Entity + Mapper + Dao — 含 JSON extra 字段 + JacksonTypeHandler
- [ ] 4.2.2 Thread Entity + Mapper + Dao（已建骨架）
- [ ] 4.2.3 Reaction Entity + Mapper + Dao（已建骨架）
- [ ] 4.2.4 ChannelReadState Entity + Mapper + Dao

#### 4.3 消息策略链（核心，复用 MallChat 策略模式）
- [ ] 4.3.1 `AbstractMsgHandler<T>` — 模板方法 checkAndSaveMsg() + @PostConstruct 自注册
- [ ] 4.3.2 `MsgHandlerFactory` — static Map<Integer, AbstractMsgHandler> + getStrategyNoNull()
- [ ] 4.3.3 `TextMsgHandler` — checkMsg() 校验文本长度 + saveMsg() 写 message.content
- [ ] 4.3.4 `ImageMsgHandler` — checkMsg() 校验文件关联 + saveMsg() 关联 file_attachment
- [ ] 4.3.5 `FileMsgHandler` — 同上
- [ ] 4.3.6 `SoundMsgHandler` — checkMsg() 校验 audioUrl 非空 + second > 0；saveMsg() 写 extra.soundMsgDTO
- [ ] 4.3.7 `SystemMsgHandler` — 系统消息（成员加入/离开/频道变更）

> **留待补充**：MentionParser（@提及解析）、CommonMark Markdown 渲染、VIDEO/EMOJI 消息类型

#### 4.4 消息 CRUD（v1 简化版）
- [ ] 4.4.1 `MessageService.sendMsg()` — 权限检查 → 策略链 → 持久化 → 发事件（**v1 暂不加入频控和敏感词过滤**）
- [ ] 4.4.2 `MessageService.getMessages()` — 频道内光标分页（`thread_id IS NULL`）
- [ ] 4.4.3 `MessageService.getMessage()` — 单条消息查询
- [ ] 4.4.4 `MessageService.editMessage()` — 仅作者，标记 status=2（已编辑）
- [ ] 4.4.5 `MessageService.deleteMessage()` — 作者或 MANAGE_MESSAGES 权限
- [ ] 4.4.6 `MessageAdapter` — Entity ↔ MessageVO（含 UserVO、reactions、attachments 组装）

> **留待补充**：频控（`@FrequencyControl` 注解 + AOP）、敏感词过滤（AC 自动机移植 MallChat `common/sensitive/`）

#### 4.4b 已读追踪
- [ ] 4.4.7 `ChannelReadStateService.updateReadState()` — 拉取消息时自动更新 last_read_msg_id（ON DUPLICATE KEY UPDATE）
- [ ] 4.4.8 `ChannelReadStateService.getUnreadCounts()` — 按 server 聚合各频道未读计数

#### 4.5 事件管道
- [ ] 4.5.1 `ChannelMessageSendEvent` — Spring ApplicationEvent
- [ ] 4.5.2 `MessageSendListener` — @TransactionalEventListener → RocketMQ SEND_MSG_TOPIC
- [ ] 4.5.3 `MsgSendConsumer` — RocketMQ 消费 → 构建 MessageVO → PushService.sendPushMsg()
- [ ] 4.5.4 `PushService.sendPushMsg()` → RocketMQ PUSH_TOPIC（含 channelId + threadId 路由信息）

#### 4.6 Thread 系统
- [ ] 4.6.1 `ThreadService.createThread()` — 从消息创建话题（需 CREATE_THREAD 权限）
- [ ] 4.6.2 `ThreadService.getThreads()` — 频道内话题列表（按 last_active 降序）
- [ ] 4.6.3 `ThreadService.getThreadMessages()` — 话题内消息分页
- [ ] 4.6.4 `ThreadService.updateThread()` — 改名/归档/重开
- [ ] 4.6.5 `ThreadService.autoArchiveThreads()` — @Scheduled 定时任务（每小时），将 24h 无活动的 Thread 自动归档
- [ ] 4.6.6 发消息时判断 thread_id：有值 → 只在 Thread 视图可见，不在频道主时间线

#### 4.7 Reaction 系统
- [ ] 4.7.1 `ReactionService.addReaction()` — 添加表情反应（需 ADD_REACTIONS 权限）
- [ ] 4.7.2 `ReactionService.removeReaction()` — 移除（再次添加同一 emoji = 移除）
- [ ] 4.7.3 `ReactionService.getMessageReactions()` — 按 emoji 聚合（COUNT + GROUP_CONCAT）
- [ ] 4.7.4 消息列表接口附带每个消息的 reactions 汇总

#### 4.8 Controller
- [ ] 4.8.1 `MessageController` — /api/v1/channels/{id}/messages (POST/GET/PUT/DELETE)
- [ ] 4.8.2 `ThreadController` — /api/v1/channels/{id}/threads + /api/v1/threads/{id}
- [ ] 4.8.3 `ReactionController` — /api/v1/messages/{id}/reactions
- [ ] 4.8.4 `SearchController` — /api/v1/servers/{id}/search

#### 4.9 VO/DTO
- [ ] 4.9.1 `SendMsgReq` — channelId, content, msgType, threadId?, replyMsgId?, fileIds[]?
- [ ] 4.9.1b `SoundMsgDTO` — url, size, second（语音消息元数据，复用 MallChat 字段）
- [ ] 4.9.2 `MessageVO` — id, channelId, threadId, fromUser, content, type, reactions[], attachments[], replyTo?, createTime, edited
- [ ] 4.9.3 `ThreadVO` — id, channelId, rootMessage, name, messageCount, lastActive, status
- [ ] 4.9.4 `ReactionVO` — emoji, count, users[], reacted(当前用户是否已添加)

#### 4.10 验证
- [ ] 4.10.1 用户在频道发文本消息 → 200 + MessageVO
- [ ] 4.10.2 光标分页取消息 → `?cursor=&pageSize=50` → CursorPage<MessageVO>
- [ ] 4.10.3 编辑自己的消息 → status=2，updatedAt 更新
- [ ] 4.10.4 删除自己的消息 → 204
- [ ] 4.10.5 创建 Thread → 在 Thread 内发消息 → 频道主时间线不出现 Thread 内消息
- [ ] 4.10.6 添加 Reaction → 返回 emoji + totalCount → 再次添加同一 emoji → 移除
- [ ] 4.10.7 无 SEND_MESSAGES 权限用户发消息 → 403
- [ ] 4.10.8 已读追踪：GET /api/v1/servers/{serverId}/unread → 返回各频道未读计数
- [ ] 4.10.9 Git 提交：`feat(phase4): 消息策略链 + Thread + Reaction + 已读追踪`

**MallChat 参考**：`MsgHandlerFactory` + `AbstractMsgHandler`（自注册策略）、`MessageSendEvent` → MQ → Consumer 管道、`CursorPageBaseResp` 分页、`MessageAdapter` DTO 组装
