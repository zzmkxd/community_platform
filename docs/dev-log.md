# 社群平台 开发日志

## 项目信息

- **项目名称**: community-platform (社群平台)
- **目标**: Discord-like 社群平台，供软件实训课程使用
- **技术栈**: Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Java 21 + Netty + MyBatis-Plus + Redis + RocketMQ + MinIO
- **架构目标**: Phase 0-7 单体开发（包级接口隔离），Phase 8 拆分为 Spring Cloud 微服务（Nacos + Gateway + Feign）—— **硬性要求**
- **项目管理**: Git + Maven
- **目录**: `E:\Learn_zone\Code_zone\IDEA_code\community-platform\`

---

## Phase 0: 项目骨架搭建 (2026-06-22)

### 创建的文件清单

#### 项目构建
- `pom.xml` — 根 POM (Spring Cloud + Alibaba BOM)
- `community-server/pom.xml` — 模块 POM (含所有依赖)
- `.gitignore`

#### 启动与配置
- `CommunityApplication.java` — Spring Boot 入口类
- `application.yml` — 主配置 (占位符模式)
- `application-local.properties` — 本地开发配置 (gitignored)
- `ThreadPoolConfig.java` — 线程池配置
- `MybatisPlusConfig.java` — MyBatis-Plus 配置
- `InterceptorConfig.java` — 拦截器注册
- `OpenApiConfig.java` — Knife4j/Springdoc 配置

#### Common 基础设施 (13 个文件)
- `RequestHolder.java` — ThreadLocal 请求上下文
- `JwtUtils.java` — JWT 生成/验证/解析
- `CursorUtils.java` — 游标分页工具
- `RedisUtils.java` — Redis 操作工具
- `BusinessException.java` — 业务异常
- `FrequencyControlException.java` — 限流异常
- `BusinessErrorEnum.java` — 业务错误码枚举
- `CommonErrorEnum.java` — 通用错误码枚举
- `ErrorEnum.java` — 错误码接口
- `GlobalExceptionHandler.java` — 全局异常处理
- `TokenInterceptor.java` — JWT Token 拦截器
- `ApiResult.java` — 统一响应体
- `CursorPageBaseReq.java` / `CursorPageBaseResp.java` — 游标分页请求/响应
- `AbstractRedisStringCache.java` + `BatchCache.java` — 批量缓存框架
- `RequestInfo.java` — 请求信息 DTO

#### WS 协议定义 (4 个文件)
- `WSReqTypeEnum.java` — C→S 请求类型 (9 种)
- `WSRespTypeEnum.java` — S→C 推送类型 (18 种)
- `WSBaseReq.java` / `WSBaseResp.java` — WS 协议帧

#### 注解 (3 个文件)
- `@FrequencyControl` + `@FrequencyControlContainer` — 频控注解
- `@RedissonLock` — 分布式锁注解

#### 常量 (2 个文件)
- `RedisKey.java` — Redis Key 常量
- `MQConstant.java` — RocketMQ Topic 常量

#### Domain Entities (13 张表)
- `User` — 用户
- `Server` — 服务器
- `Category` — 分类
- `Channel` — 频道
- `ServerMember` — 服务器成员
- `Role` — 角色 (14位权限位图)
- `MemberRole` — 成员-角色关联
- `ChannelPermission` — 频道权限覆盖
- `Emoji` — 服务器表情
- `Message` — 消息 (含 extra JSON)
- `Thread` — 话题
- `Reaction` — 反应
- `FileAttachment` — 文件附件

#### Enums (6 个)
- `PermissionBit.java` — 14位权限定义
- `MessageTypeEnum.java` — 消息类型 (TEXT/IMAGE/FILE/SYSTEM/SOUND)
- `ChannelTypeEnum.java` — 频道类型 (TEXT/VOICE)
- `MemberStatusEnum.java` — 成员状态 (ACTIVE/KICKED/LEFT)
- `ThreadStatusEnum.java` — 话题状态 (ACTIVE/ARCHIVED)
- `FileStatusEnum.java` — 文件状态 (PENDING/UPLOADED)

#### VOs (12 个)
- `UserVO`, `ServerVO`, `CategoryVO`, `ChannelVO`, `MemberVO`, `RoleVO`
- `ServerDetailVO` (嵌套 server + categories + channels + myRoles)
- `MessageVO`, `ThreadVO`, `ReactionVO`, `FileVO`, `EmojiVO`

#### DAO 层 (13 对 Mapper + Dao)
- 每个实体对应一个 `Mapper` 接口 + 一个 `Dao` (extends ServiceImpl)

#### Service 层 (11 个接口 + 11 个 Impl)
- **User**: AuthService, UserService
- **Server**: ServerService, ChannelService, MemberService, RoleService, PermissionService, EmojiService
- **Message**: MessageService, ThreadService, ReactionService, SearchService
- **File**: FileService

#### Controller 层 (11 个)
- **User**: AuthController, UserController
- **Server**: ServerController, CategoryController, ChannelController, MemberController, RoleController, EmojiController
- **Message**: MessageController, ThreadController, ReactionController, SearchController
- **File**: FileController

#### WebSocket Netty 模块 (8 个文件)
- `NettyWebSocketServer.java` — Netty 启动类 (端口 8090)
- `NettyWebSocketServerHandler.java` — 业务 Handler (连接/认证/订阅/分发)
- `HttpHeadersHandler.java` — Token/IP 提取
- `NettyUtil.java` — Channel 属性工具
- `WebSocketService.java` + `WebSocketServiceImpl.java` — 连接管理 + 频道/话题订阅
- `PushConsumer.java` — RocketMQ 推送消费者
- `WSAdapter.java` — WS 响应构建器

### 统计

| 类别 | 数量 |
|------|------|
| 总 Java 文件 | ~100+ |
| 实体 (Entities) | 13 |
| 枚举 (Enums) | 6 |
| VO 类 | 12 |
| DTO 类 | 3 |
| Mapper 接口 | 13 |
| Dao 类 | 13 |
| Service 接口 | 11 |
| Service 实现 | 11 |
| Controller | 11 |
| WS 模块 | 8 |
| Common 基础设施 | ~15 |

---

## Phase 0.1: 代码审计与修复 (2026-06-22)

对照计划文档 (`docs/plan/`) 进行全量代码审计，发现并修复以下问题：

### 致命缺陷 (1 项)
| # | 问题 | 文件 | 修复 |
|---|------|------|------|
| 1 | FileController 类级 `@RequestMapping("/api/v1/upload")` + 方法级 `/api/v1/files/` 导致路径叠加为 `/api/v1/upload/api/v1/files/` | FileController.java | 类级改为 `@RequestMapping("/api/v1")`，方法路径改为 `/upload/*` 和 `/files/*` |

### API 契约违规 (5 项)
| # | 问题 | 修复 |
|---|------|------|
| 2 | CategoryController.update 返回 void，计划要求 `→ CategoryVO` | `ChannelService.updateCategory` 返回值改为 `CategoryVO` |
| 3 | ReactionController POST/DELETE 都调用 `toggleReaction`，语义错误 | 拆分为 `addReaction` 和 `removeReaction` 两个独立方法 |
| 4 | DELETE 端点返回 200 + body，计划要求 `→ 204` | 8 个 DELETE 端点全部改为 `@ResponseStatus(HttpStatus.NO_CONTENT)` + void |
| 5 | Reaction DELETE 返回 200 + body | 改为 204 + void |

### 缺失端点 (5 项)
| # | 问题 | 修复 |
|---|------|------|
| 6 | ChannelPermissionController 完全缺失 (PUT/GET/DELETE 3 个端点) | 新建 Controller + Service + VO 共 4 个文件 |
| 7 | RoleController 缺失成员-角色分配端点 (POST/DELETE) | RoleController 新增 2 个端点 |

### 代码质量 (4 项)
| # | 问题 | 文件 | 修复 |
|---|------|------|------|
| 8 | 未使用的 import `com.alanpoi.analysis.lambda.LambdaUtils` | CursorUtils.java | 删除 |
| 9 | `@MapperScan("com.community.**.dao")` 无法匹配 `*.dao.mapper` 子包 | CommunityApplication.java | 改为 `com.community.**.dao.mapper` |
| 10 | ThreadController 无类级 `@RequestMapping`，方法使用硬编码全路径 | ThreadController.java | 添加 `@RequestMapping("/api/v1")`，路径相对化 |
| 11 | `mybatis-plus-jsqlparser` 缺失依赖导致 `PaginationInnerInterceptor` 编译失败 | community-server/pom.xml | 添加依赖 |

### 通过验证的项目
- 6 个枚举值全部与计划一致 (PermissionBit 14 bits, MessageTypeEnum 5 types, ChannelTypeEnum, MemberStatusEnum, ThreadStatusEnum, FileStatusEnum)
- 13 个实体类全部存在且字段匹配
- WSReqTypeEnum 10 个类型码正确
- WSRespTypeEnum 18 个类型码正确
- 11 个 API 路径模式正确（除 FileController 外）
- 项目结构符合计划 Phase 1 单体架构

### 新增文件
- `server/domain/vo/ChannelPermissionVO.java`
- `server/service/ChannelPermissionService.java`
- `server/service/impl/ChannelPermissionServiceImpl.java`
- `server/controller/ChannelPermissionController.java`

### 编译验证
```bash
mvn clean compile -pl community-server  # BUILD SUCCESS, 141 source files
```

---

## Phase 1: 最小可运行闭环 — User 模块 (2026-06-22)

### 目标
实现注册/登录/JWT 认证链路，使项目可启动并通过 curl 测试。

### DDL
- `docs/ddl.sql` — 全 13 张表 DDL，utf8mb4 字符集，含索引与注释

### 依赖变更
| 依赖 | 说明 |
|------|------|
| `spring-security-crypto` | BCrypt 密码加密 |
| `mybatis-plus-jsqlparser` | PaginationInnerInterceptor（Phase 0.1 补加） |

### 实现的功能

**AuthServiceImpl** — 注册 / 登录 / Token 刷新 / Redis Session 管理
- `register`: 用户名唯一性校验 → BCrypt 加密 → 持久化 → JWT 签发 → Redis 存储 token (5 天 TTL)
- `login`: 用户名查用户 → BCrypt 密码比对 → JWT 签发 → Redis 存储
- `refreshToken`: 校验旧 token → 签发新 token (原子 SET 覆盖，无竞态)
- `getValidUid`: JWT 验签 + Redis Token 值比对 (真正的单设备登录)

**UserServiceImpl** — 个人信息 / 编辑 / 公开查询
- `getMe`: RequestHolder 获取 uid → 查 DB → 返回 UserVO
- `updateMe`: RequestHolder → 查 DB → 更新昵称/头像/邮箱 → 返回 UserVO
- `getUserById`: 直接查 DB 返回公开信息

**TokenInterceptor 升级**
- 从 `JwtUtils` 直接校验改为注入 `AuthService.getValidUid()`
- 多了 Redis 层校验，实现单设备登录控制

**UserVO 扩展**
- 新增 `token` 字段，注册/登录响应携带 JWT

### 修改/新增文件

| 文件 | 操作 |
|------|------|
| `docs/ddl.sql` | 新增 — 13 张表完整 DDL |
| `user/domain/vo/UserVO.java` | 修改 — 新增 token 字段 |
| `user/service/impl/AuthServiceImpl.java` | 重写 — 完整实现 |
| `user/service/impl/UserServiceImpl.java` | 重写 — 完整实现 |
| `common/interceptor/TokenInterceptor.java` | 修改 — JwtUtils → AuthService |
| `community-server/pom.xml` | 修改 — 新增 spring-security-crypto + mybatis-plus-jsqlparser |

### 编译验证
```bash
mvn clean compile -pl community-server  # BUILD SUCCESS, 141 source files
```

---

## Phase 1.1: 登录安全加固 (2026-06-22)

### 修复的安全问题

#### #1 getValidUid Token 值比对 (单设备登录)

**位置**: `AuthServiceImpl.java:101-105`

**问题**: `getValidUid()` 只检查 Redis key 是否存在 (`stored != null`)，不比较 Token 值。
设备 B 登录后 SET 新 Token → Redis key 仍存在 → 设备 A 的旧 Token 仍可通过验证。

**修复**: `stored != null` → `token.equals(stored)`
新登录覆盖 Redis 中的 Token 值 → 旧 Token 立即失效 → 真正的单设备登录。

#### #2 refreshToken 竞态条件

**位置**: `AuthServiceImpl.java:87-95`

**问题**: DEL 旧 Token 和 SET 新 Token 之间存在窗口期，并发请求会因 Redis key 不存在而被拒绝。

**修复**: 移除 `RedisUtils.del()` 调用，直接 SET 覆盖。
Redis SET 是原子操作，新值覆盖后旧 Token 在比对校验中自然失效。

### 设计决策记录

- **JWT 不加 `withExpiresAt()`**: 有意设计，Redis TTL 控制生命周期，JWT 仅做 uid 载体。MallChat 同理。
- **双 Token (access + refresh)**: 性能优化项，当前单 Token 方案足够。详细方案记录在 [08-optimizations.md](plan/08-optimizations.md)。
- **微信登录**: 可行但独立模块，优先级低于核心功能链路。Phase 2+ 可选。

### 参考
- 优化方案: `docs/plan/08-optimizations.md`

---

## Phase 1.2: Plan 对齐 MallChat + Spring Cloud 修正 (2026-06-22)

### 目标
将 `docs/plan/` 与 MallChat plan 全面对齐，补全缺失内容，同时修正 Spring Cloud 相关表述以反映微服务为最终目标。

### Plan 文件变更

#### 新增 3 个文件（从 MallChat 复制）
| 文件 | 内容 |
|------|------|
| `docs/plan/mallchat-business-flows.md` | MallChat 现有业务流程精准描述（参考基线） |
| `docs/plan/community-flows-visual.html` | 4 Tab 可视化总览（业务流程·ER 图·接口映射·WS 协议） |
| `docs/plan/08-microservice-upgrade-path.md` | 微服务升级操作手册（模块拆分 + Feign + Nacos/Gateway + 工作量估算） |

#### 更新 8 个已有文件
| 文件 | 主要补充 |
|------|---------|
| `README.md` | 新增 4 个文件索引；Context 对齐；架构策略明确 Phase 8 为硬性要求；阶段总览增加 Phase 7+ |
| `01-architecture.md` | 消息类型补 Sound；用户模块增加微信扫码登录行；复用表增 SoundMsgHandler |
| `02-business-flows.md` | 新增：登录鉴权双模式、用户加入服务器（三种方式+邀请）、已读/未读追踪、成员管理（角色分配/踢出/Owner转让）、完整 WS 协议枚举 (27 种) |
| `03-api-spec.md` | 新增：微信回调端点、discover/transfer/invite/unread 端点；注册/登录标记 Phase 1 已实现 |
| `04-migration.md` | DDL 设计 7→14 条；补 invite/channel_read_state/sensitive_word 说明；新增"待加入组件"速查表 |
| `05-dev-phases-0-4.md` | Phase 4 补全：MentionParser、SoundMsgHandler、SoundMsgDTO、ChannelReadState(DDL+Entity+Service)、SearchController、Thread 自动归档 |
| `06-dev-phases-5-8.md` | 新增 Phase 7+（接口边界审计 + WebSocket 独立进程） |
| `07-dev-rules.md` | 补充包级接口隔离规则 + Spring Cloud 目标说明 |

### Spring Cloud 表述修正（3 处）
| 文件 | 旧表述 | 新表述 |
|------|--------|--------|
| `README.md` | "单体编码…Spring Cloud BOM 零成本预留，Phase 0-7 不做物理拆分" | "Phase 0-7 单体编码。Phase 8 按计划拆分为 Spring Cloud 微服务，此为**硬性要求**" |
| `07-dev-rules.md` | "Spring Cloud BOM 仅声明版本号，不引入实际组件" | "Phase 0-7 单模块内按业务边界分包，Phase 8 拆分为独立微服务。包级接口隔离确保拆分时 Service→FeignClient 改动量最小" |
| `01-architecture.md` | 技术栈已含 Spring Cloud 2023.0.3 + Alibaba 2023.0.1.0 + Nacos 2.3.x（无需修改） | — |

### Plan 一致性确认
| 维度 | 状态 |
|------|------|
| 登录方式 | 用户名/密码 (Phase 1 已实现) + 微信 OAuth (Phase 2+ 目标) — 双模式 |
| 表数量 | 15 张（当前 DDL 13 张，缺 invite + channel_read_state） |
| 消息类型 | 5 种（TEXT/IMAGE/FILE/SYSTEM/SOUND） |
| WS 协议 | 9 请求 + 18 推送 = 27 种 |
| 架构路径 | 单体 → Phase 8 微服务 |

---

## Phase 1.3: 代码差距分析 (2026-06-22)

对照 MallChat plan 全量 checklist 与实际代码，识别出以下差距：

### 缺口总览

| 优先级 | 数量 | 内容 | 阻塞 |
|--------|------|------|------|
| **P0** | 5 项 | Invite 表 + Entity + Mapper + Dao + VO + Service + Controller | Phase 3 Server CRUD |
| **P1** | 8 项 | SoundMsgHandler、MentionParser、SoundMsgDTO、channel_read_state 全套 (5 文件)、PushService、MessageTypeEnum.SOUND | Phase 4 Message 模块 |
| **P2** | 5 项 | User.open_id 字段、敏感词 AC 自动机、docker-compose.yml、broker.conf、微信登录全套 | Phase 2+/5+ 可选 |
| **P3** | 2 项 | WS 协议枚举补全、ServerDetailVO.myRoles | 后续微调 |

### P0 详情 — Phase 3 前必须补齐（Invite 模块）
```
新增文件 (7 个):
  server/domain/entity/Invite.java
  server/dao/mapper/InviteMapper.java
  server/dao/InviteDao.java
  server/domain/vo/InviteVO.java
  server/service/InviteService.java
  server/service/impl/InviteServiceImpl.java
  server/controller/InviteController.java

DDL 补充 (1 张表):
  invite — code(UK), server_id(FK), inviter_id(FK), max_uses, times_used, expire_at
```

### P1 详情 — Phase 4 补齐（消息模块增强）
```
新增文件 (12 个):
  message/service/strategy/msg/SoundMsgHandler.java
  message/service/parser/MentionParser.java
  message/domain/dto/SoundMsgDTO.java
  message/domain/entity/ChannelReadState.java
  message/dao/mapper/ChannelReadStateMapper.java
  message/dao/ChannelReadStateDao.java
  message/service/ChannelReadStateService.java
  message/service/impl/ChannelReadStateServiceImpl.java
  message/service/PushService.java
  message/service/SearchService.java (已有接口, 待实现)

DDL 补充 (1 张表):
  channel_read_state — user_id, channel_id(UK together), last_read_msg_id, last_read_time

修改 (1 项):
  MessageTypeEnum — 新增 SOUND 枚举值
```

### P2/P3 — 延后处理
详见 `docs/plan/04-migration.md` 底部"待加入组件"速查表。

### 推进策略

```
Phase 3 (本次): Server/Category/Channel CRUD + Member + Role + Permission + Invite (含 P0)
    ↓
Phase 4 (下次): Message/Thread/Reaction + SoundMsg + MentionParser + ChannelReadState + PushService (含 P1)
    ↓
Phase 5 (后续): WebSocket 实时推送（依赖 Phase 4 消息链路就绪）
    ↓
Phase 6-8: 文件/搜索/收尾/微服务拆分
```

---

## Phase 1.4: 用户模块完善 — 统一用户名/微信登录 (2026-06-22)

### 目标
为 Phase 2+ 微信 OAuth 扫碼登录做准备，统一用户实体和鉴权模式。用户名/密码和微信登录两种方式共存。

### User 实体扩展
- 新增 `open_id VARCHAR(64) UNIQUE NULL` — 微信 openId，用户名用户为 NULL
- 新增 `union_id VARCHAR(64) NULL` — 微信 unionId，预留
- MySQL UNIQUE 索引允许多个 NULL，两种用户形态共存

### AuthService 统一 (新增 2 方法)

**`login(Long uid)`** — 直接按 uid 签发 token
- 先查 Redis 是否已有有效 token，有则返回（避免频繁签发）
- 无则创建 JWT + 写 Redis（5 天 TTL）
- 用于微信回调：用户已由微信侧认证，只需签发 token

**`renewalTokenIfNecessary(String token)`** — 异步续期
- `@Async` 异步执行，对用户透明
- 查 Redis key 剩余 TTL → 不足 2 天时重置为 5 天
- 只改 Redis TTL，不重新签发 JWT
- TokenInterceptor 中每次请求调用

### TokenInterceptor 增强
- `preHandle` 验证通过后调 `authService.renewalTokenIfNecessary(token)` 实现无感续期

### 账号绑定端点
- `POST /api/v1/users/me/bind-wx { code?, openId? }` — 绑定微信到当前账户
- 校验 openId 不重复绑定 → 更新 user.open_id
- `code` 路径留到 Phase 2+（需 weixin-java-mp）

### RedisUtils 扩展
- 新增 `getExpire(key, TimeUnit)` 方法，支持 TTL 查询

### 修改/新增文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `user/domain/entity/User.java` | 修改 | 加 openId, unionId 字段 |
| `user/domain/vo/UserVO.java` | 修改 | 加 openId, unionId 字段 |
| `user/service/AuthService.java` | 修改 | 加 login(Long), renewalTokenIfNecessary |
| `user/service/impl/AuthServiceImpl.java` | 修改 | 实现 2 个新方法 |
| `user/service/UserService.java` | 修改 | 加 bindWeChat 方法签名 |
| `user/service/impl/UserServiceImpl.java` | 修改 | 实现 bindWeChat + toVO 补充新字段 |
| `user/controller/UserController.java` | 修改 | 加 POST /me/bind-wx |
| `user/domain/dto/AccountBindReq.java` | 新增 | { code, openId } |
| `common/utils/RedisUtils.java` | 修改 | 加 getExpire 方法 |
| `common/exception/BusinessErrorEnum.java` | 修改 | 加 WECHAT_NOT_CONFIGURED, OPEN_ID_ALREADY_BOUND |
| `common/interceptor/TokenInterceptor.java` | 修改 | 加 renewalTokenIfNecessary 调用 |
| `CommunityApplication.java` | 修改 | 加 @EnableAsync |
| `docs/ddl.sql` | 修改 | user 表加 open_id, union_id |

### 设计决策
- **JWT 不加 `withExpiresAt()`**: 延续 Phase 1.1 决策，Redis TTL 控制生命周期
- **异步续期不重签 JWT**: 只刷新 Redis TTL，不改 JWT 内容，性能最优
- **`login(uid)` 复用已有 token**: 如果 Redis 中已有有效 token 直接返回，避免重复签发
- **绑定接口同时接受 code + openId**: code 留到 Phase 2+（微信 OAuth），openId 供当前测试

### 编译验证
```bash
mvn clean compile -pl community-server  # BUILD SUCCESS
```

---

## Phase 2: 微信公众平台 OAuth 扫码登录 (2026-06-22)

### 目标
从 MallChat 移植完整 3 阶段 WebSocket 登录流程（QR 码生成 → 扫码 → 授权），全部配置复用 MallChat 同一公众号（同一 appId/secret/token/aesKey）。

### 新建文件（17 个）

| 文件 | 说明 |
|------|------|
| `common/config/WxMpProperties.java` | @ConfigurationProperties(prefix = "wx.mp") — configs + useRedis + redisConfig |
| `common/config/WxMpConfiguration.java` | WxMpService Bean（setMultiConfigStorages）+ WxMpMessageRouter Bean |
| `user/service/handler/AbstractHandler.java` | Implements WxMpMessageHandler，提供 protected Logger logger |
| `user/service/handler/LogHandler.java` | @Component，日志记录所有微信事件 |
| `user/service/handler/ScanHandler.java` | @Component，委托 WxMsgService.scan() |
| `user/service/handler/SubscribeHandler.java` | @Component，关注 + 扫码关注 → WxMsgService.scan() |
| `user/service/handler/MsgHandler.java` | @Component，默认消息 Handler（简化，无 WxMsgDao 持久化） |
| `user/service/adapter/TextBuilder.java` | 构建 WxMpXmlOutTextMessage |
| `user/service/adapter/UserAdapter.java` | buildAuthorizeUser() — WxOAuth2UserInfo → User |
| `user/service/WxMsgService.java` | 核心业务：scan() / authorize() / fillUserInfo() |
| `user/controller/WxPortalController.java` | 3 端点：GET server verify / POST event push / OAuth callback |
| `common/domain/dto/LoginMessageDTO.java` | { Long uid, Integer code } |
| `common/domain/dto/ScanSuccessMessageDTO.java` | { Integer code } |
| `common/domain/dto/WSChannelExtraDTO.java` | { Long uid } |
| `user/domain/vo/response/ws/WSLoginUrl.java` | { String loginUrl } |
| `user/domain/vo/response/ws/WSLoginSuccess.java` | { Long uid, String avatar, String name, String token, Integer power } |
| `user/consumer/MsgLoginConsumer.java` | @RocketMQMessageListener LOGIN_MSG_TOPIC (BROADCASTING) |
| `user/consumer/ScanSuccessConsumer.java` | @RocketMQMessageListener SCAN_MSG_TOPIC (BROADCASTING) |

### 修改文件（12 个）

| 文件 | 改动 |
|------|------|
| `community-server/pom.xml` | 加 weixin-java-mp:4.4.0 + caffeine 依赖 |
| `application.yml` | 加 wx.mp.callback + configs 占位符配置 |
| `application-local.properties` | 加 MallChat 微信配置值 |
| `constant/MQConstant.java` | 加 LOGIN_MSG_TOPIC / SCAN_MSG_TOPIC + 4 consumer groups |
| `constant/RedisKey.java` | 加 OPEN_ID_STRING ("openId:") / LOGIN_CODE ("loginCode") |
| `common/interceptor/TokenInterceptor.java` | `/wx/portal/` 加入 isPublicURI 白名单 |
| `common/websocket/dto/WSBaseResp.java` | 泛型化：`WSBaseResp<T>` + `of(Integer, T)` |
| `common/websocket/WSRespTypeEnum.java` | 加 LOGIN_URL(1), LOGIN_SCAN_SUCCESS(2), INVALIDATE_TOKEN(6) |
| `websocket/service/WebSocketService.java` | 加 scanLoginSuccess / scanSuccess / sendToAllOnline / sendToUid |
| `websocket/service/impl/WebSocketServiceImpl.java` | 完整重写：WAIT_LOGIN_MAP (Caffeine)、ONLINE_WS_MAP、ONLINE_UID_MAP、handleLogin(QR)、scanLoginSuccess、scanSuccess |
| `websocket/service/adapter/WSAdapter.java` | 加 buildLoginResp / buildScanSuccessResp / buildLoginSuccessResp / buildInvalidateTokenResp |
| `user/service/AuthService.java` | 加 verify(String token) 方法 |
| `user/service/impl/AuthServiceImpl.java` | 实现 verify() — JWT decode + Redis value 比对 |
| `user/dao/UserDao.java` | 加 getByOpenId(String openId) |
| `user/domain/entity/User.java` | 新增 openId / unionId 字段（Phase 1.4 遗留） |

### 方案变更（相对原计划）

| # | 变更点 | 原计划 | 实际情况 | 原因 |
|---|--------|--------|----------|------|
| 1 | `setMultiConfigStorages` API | 双参数 (defaultConfig, Map) | 单参数 (Map) — 对齐 MallChat | weixin-java-mp 4.4.0 API 签名与旧版不同 |
| 2 | `WxPortalController.route()` | 直接调 messageRouter.route() 捕获 WxErrorException | private helper route() 捕获 Exception | route() 在 4.4.0 不声明 WxErrorException，Java 编译报错 |
| 3 | `MsgHandler.log` | `log.info(...)` | `logger.info(...)` | 继承自 AbstractHandler 的字段名是 `logger` |
| 4 | MQ 发消息方式 | 计划用 MQProducer 包装 | 直接注入 RocketMQTemplate | 社区平台无 MQProducer 包装，改用底层 API |
| 5 | MsgHandler 持久化 | 保存到 WxMsg 表 | 简化，仅日志记录 | WxMsg 表非必需，后续按需添加 |
| 6 | MallChat WxPortalController POST 路径 | 无前导 `/` | 加前导 `/`（`/wx/portal/public`） | 社区平台路由规范要求 |

### WebSocket 登录状态机

```
Client                          Server                          WeChat
  |                                |                                |
  |--- WS connect + token -------->|                                |
  |                                |                                |
  |--- type=LOGIN(1) ------------->|  handleLogin(channel)          |
  |                                |  1. INCR loginCode (Redis)     |
  |                                |  2. WxMpService.getQrcodeService()
  |                                |     .createQrCodeTicket(loginCode)
  |                                |  3. WAIT_LOGIN_MAP.put(code, channel)
  |<-- type=LOGIN_URL(1) ---------|  4. return QR loginUrl          |
  |                                |                                |
  |                          User scans QR                          |
  |                          WeChat → WxPortalController.post()     |
  |                                |  ScanHandler → WxMsgService.scan()
  |                                |  1. openId → User query/create |
  |                                |  2. Redis openId:code 映射     |
  |                                |  3. MQ SCAN_MSG_TOPIC          |
  |                                |  4. return OAuth 授权链接      |
  |                                |                                |
  |<-- type=SCAN_SUCCESS(2) -------|  ScanSuccessConsumer →         |
  |                                |  scanSuccess(code) →           |
  |                                |  通知前端扫码成功              |
  |                          User 点击授权                          |
  |                          WeChat → WxPortalController.callBack() |
  |                                |  authorize(userInfo)            |
  |                                |  1. fillUserInfo(uid, userInfo) |
  |                                |  2. Redis openId → code        |
  |                                |  3. MQ LOGIN_MSG_TOPIC         |
  |                                |                                |
  |<-- type=LOGIN_SUCCESS(3) ------|  MsgLoginConsumer →            |
  |                                |  scanLoginSuccess(code, uid)   |
  |                                |  1. WAIT_LOGIN_MAP.get(code)   |
  |                                |  2. login(uid) → JWT           |
  |                                |  3. channel 绑定 uid           |
```

### 编译排查记录

| # | 错误 | 文件 | 根因 | 修复 |
|---|------|------|------|------|
| 1 | WSBaseResp 不兼容泛型 | WSAdapter / WebSocketService | WSBaseResp 是非泛型类 | 改为 `WSBaseResp<T>` + 泛型 `of()` |
| 2 | Caffeine 类找不到 | pom.xml | 缺 caffeine 依赖 | 添加 `com.github.benmanes.caffeine:caffeine` |
| 3 | setMultiConfigStorages 类型不兼容 | WxMpConfiguration.java:50 | 旧版双参数 API | 改为单参数 `Map<String, WxMpConfigStorage>` |
| 4 | 找不到符号 `log` | MsgHandler.java:18 | 字段名应为 `logger` | `log.info()` → `logger.info()` |
| 5 | WxErrorException 无法抛出 | WxPortalController.java:69,81 | route() 不声明该异常 | private helper route() 捕获 Exception |
| 6 | 语法错误（pattern matching 等） | HttpHeadersHandler / NettyWebSocketServerHandler | JAVA_HOME 指向 JDK 8 | JAVA_HOME 改为 JDK 21 |

### 编译验证
```bash
mvn clean compile -pl community-server -am  # BUILD SUCCESS, 161 source files
```

---

## Phase 2.1: 登录方案精简 (2026-06-22)

### 决策
双模式登录（用户名/密码 + 微信 OAuth）保留，但用户名路径从"独立注册系统"降级为"seed data 测试通道"。

### 变更内容

| # | 变更 | 说明 |
|---|------|------|
| 1 | 删除 `POST /api/v1/auth/register` | 不允许自由注册 |
| 2 | 删除 `RegisterReq.java` | 注册请求 DTO |
| 3 | 删除 `AuthService.register()` / `AuthServiceImpl.register()` | 注册方法 |
| 4 | User 表 `username`/`password` 改为 NULLABLE | 微信用户这些字段为 NULL |
| 5 | User 表新增 `uk_open_id` 唯一索引 | 微信 openId 唯一约束 |
| 6 | DDL 预置 3 个 seed data 账号 | alice/bob/charlie，密码 "123456" |
| 7 | Plan 6 个文档更新 | README/01/02/03/04/05 全部反映决策 |
| 8 | dev-log 记录 `[[login-design-decision]]` | 设计决策内存 |

### 设计理由
- 纯微信 OAuth 阻塞性太强（公众号过期、网络不通直接无法演示）
- 纯用户名登录丢失 Netty/RocketMQ/OAuth2 学习价值
- 双模式覆盖技术栈最广
- 注册端点删除消除 30% 冗余代码，核心学习价值不减

### 编译验证
```bash
mvn clean compile -pl community-server -am  # BUILD SUCCESS
```

---

## Phase 2.2: Plan 双向比对 + 路线修正 (2026-06-22)

### 目标
逐个文件比对 MallChat `plan/` 与社区平台 `docs/plan/`，识别所有出入并确认社区平台路线偏离是否合理。

### 比对结论

两个 plan 目录结构高度一致（同源），核心分歧仅 2 条：

| # | 分歧 | MallChat plan | 社区平台 plan | 判定 |
|---|------|--------------|-------------|------|
| 1 | 登录方式 | 纯微信 OAuth | seed data + 微信 OAuth 双模式 | ✅ 保留双模式（学习广度优先） |
| 2 | 微服务 | 不做，单体 + Nginx | Phase 8 硬性要求 | ✅ 保留硬性要求（课程评分项） |

其余 90% 内容一致：API 规格、DDL 设计、Phase 3-7 checkbox、WebSocket 协议枚举。

### 发现并修复的社区平台文档偏差

| # | 文件 | 偏差 | 修复 |
|---|------|------|------|
| 1 | `README.md` | "Phase 1 先实现用户名/密码注册登录" | 改为 "seed data 测试通道 + 微信 OAuth 主路径" |
| 2 | `01-architecture.md` | 用户模块含"注册"行 | 改为微信扫码为主，"用户名/密码登录"标为 seed data 通道 |
| 3 | `02-business-flows.md` | 2.2 节"当前登录流程（用户名/密码）"在"目标登录流程（微信扫码）"之前 | 重排：先主路径（微信扫码），seed data 作为辅助说明 |
| 4 | `03-api-spec.md` | 含 `POST /register` 端点 | 删除 register 端点，仅保留 login |
| 5 | `04-migration.md` | DDL 决策 #14 "User 表支持双模式"措辞模糊 | 改为 "username/password 仅 seed data，open_id 用于微信用户" |
| 6 | `05-dev-phases-0-4.md` | Phase 2 含 register 相关 checkbox | 删除注册相关项，重写为 seed data 登录 + 微信扫码 |
| 7 | `06-dev-phases-5-7.md` | 缺失 Phase 8 | 拆为 `06-dev-phases-5-8.md`，新增完整 Phase 8 微服务章节 |

### 参考
- MallChat plan: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\plan\`
- 社区平台 plan: `E:\Learn_zone\Code_zone\IDEA_code\community-platform\docs\plan\`
- 设计决策: `memory/login-design-decision.md`

---

## 待办：后续升级清单

> 以下条目在 Phase 0-2 中因优先级、复杂度或外部依赖而被推迟，Phase 3+ 或重构时重新评估。

### 阻塞性待办（启动前必须解决）

| # | 条目 | 阻塞场景 | 说明 |
|---|------|---------|------|
| T1 | BCrypt seed data 哈希 | docker compose 首次启动 | `ddl.sql` 中 `<BCRYPT_HASH_OF_123456>` 占位符需替换为真实 BCrypt hash，否则 seed data 账号无法登录 |
| T2 | `application-local.properties` 微信凭据 | 微信扫码登录 | 当前使用 MallChat 公众号凭据，新环境需更新 `community.wx.*` |

### 功能缺口（按 Phase 推进时补齐）

| # | 条目 | Phase | 说明 |
|---|------|-------|------|
| T3 | Invite 模块（P0） | Phase 3 | 7 个文件：Entity + Mapper + Dao + VO + Service + Controller |
| T4 | SoundMsgHandler + SoundMsgDTO（P1） | Phase 4 | 语音消息处理 |
| T5 | MentionParser（P1） | Phase 4 | @提及正则提取 + uid 解析 |
| T6 | ChannelReadState 全套（P1） | Phase 4 | Entity + Mapper + Dao + Service + 未读计数 API |
| T7 | PushService（P1） | Phase 4 | MQ 推送路由封装 |
| T8 | AC 自动机敏感词（P2） | Phase 4 | `common/sensitive/` 移植 MallChat ACTrie |
| T9 | WxMsg 持久化表（P2） | 后续按需 | 微信原始消息审计（MallChat 有，社区平台当前跳过） |
| T10 | docker-compose.yml（P2） | Phase 3 前 | MySQL 3307 + Redis 6380 + RocketMQ + MinIO + ES |
| T11 | broker.conf（P2） | Phase 3 前 | RocketMQ Broker 配置 |

### 技术债（最后收尾）

| # | 条目 | 说明 |
|---|------|------|
| T12 | `@SecureInvoke` + `MQProducer` | MallChat 事务安全投递模式，社区平台当前用 RocketMQTemplate 裸调 |
| T13 | `mallchat-flows-visual.html` 未复制 | MallChat plan 有第二个 HTML 可视化，社区平台未移植 |
| T14 | `CursorUtils.java` unchecked 警告 | 泛型强制转换，不影响编译但需关注 |
| T15 | `WxMsgService.java` deprecated API | weixin-java-mp 4.4.0 API 变更，后续升级需适配 |

### 架构升级（Phase 8 硬性要求）

| # | 条目 | 说明 |
|---|------|------|
| T16 | 包级接口隔离审计 | Phase 7+ 检查所有跨包引用，确保无 DAO/Mapper/Entity 泄漏 |
| T17 | Nacos + Gateway + Feign | Phase 8 微服务拆分，参考 `08-microservice-upgrade-path.md` |
| T18 | 配置中心迁移 | MySQL/Redis/RocketMQ 连接串从各服务抽取到 Nacos |

---

## 参考

- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
- 项目计划: `docs/plan/`
- 优化方案: `docs/plan/08-optimizations.md`
- 微服务升级: `docs/plan/08-microservice-upgrade-path.md`
- 前端计划: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\plan_front\`
