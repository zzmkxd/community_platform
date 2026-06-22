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

## Phase 3: Server/Category/Channel CRUD + RBAC 权限系统 + 邀请 (2026-06-22)

**Commit**: `943ebfa` feat(phase3): Server/Category/Channel CRUD + RBAC 权限系统 + 邀请

### 目标

实现社群平台核心一：Server > Category > Channel 三级嵌套结构 + 13-bit BIGINT 位掩码 RBAC + 邀请制加入。

### 新增文件（34 个）

#### Server 模块 (`community/server/`)
| 文件 | 说明 |
|------|------|
| `domain/entity/Server.java` | Server 实体 (id, name, icon, ownerId, ...) |
| `domain/entity/ServerMember.java` | 成员实体 (serverId, userId, roleId, ...) |
| `domain/entity/Category.java` | 分类实体 (serverId, name, sortOrder, ...) |
| `domain/entity/Channel.java` | 频道实体 (categoryId, name, type, permission, ...) |
| `domain/entity/Role.java` | 角色实体 (serverId, name, permissions BIGINT, ...) |
| `domain/entity/Invite.java` | 邀请码实体 (serverId, code, expiresAt, maxUses, ...) |
| `domain/vo/*.java` | 8 个 VO (ServerVO, CategoryVO, ChannelVO, RoleVO, MemberVO, InviteVO, ...) |
| `domain/mapper/*.java` | 6 个 MyBatis-Plus Mapper |
| `domain/dao/*.java` | 6 个 DAO (封装 Mapper) |
| `service/ServerService.java` | Server CRUD + 业务逻辑 |
| `service/ServerServiceImpl.java` | 实现：创建默认角色 + 默认分类 + 默认频道 |
| `service/ChannelService.java` | Channel CRUD + 权限校验 |
| `service/ChannelServiceImpl.java` | 实现：创建默认 TEXT 频道 |
| `service/RoleService.java` | 角色权限管理 |
| `service/RoleServiceImpl.java` | 实现：13-bit 位掩码组合 |
| `service/MemberService.java` | 成员/邀请管理 |
| `service/MemberServiceImpl.java` | 实现：邀请码生成/验证/加入 |
| `controller/ServerController.java` | REST API 端点 |
| `controller/CategoryController.java` | 分类 CRUD 端点 |
| `controller/ChannelController.java` | 频道 CRUD 端点 |
| `controller/MemberController.java` | 成员/角色管理端点 |
| `controller/InviteController.java` | 邀请码创建/验证/加入端点 |

#### 权限系统 (`community/permission/`)
| 文件 | 说明 |
|------|------|
| `PermissionBit.java` | 13 个权限位枚举 (MANAGE_SERVER, MANAGE_ROLES, MANAGE_CHANNELS, KICK_MEMBERS, BAN_MEMBERS, CREATE_INVITE, MANAGE_INVITES, SEND_MESSAGES, EMBED_LINKS, ATTACH_FILES, ADD_REACTIONS, USE_EXTERNAL_STICKERS, MENTION_EVERYONE) |
| `PermissionParser.java` | 权限位解析工具类 |

### RBAC 权限模型

```
最终权限 = (用户角色权限 | 全部角色权限)  // OR 合并
         & 角色级别权限                       // 过滤角色范围
         + ADMINISTRATOR 覆盖              // 拥有任意角色 OR 含 ADMINISTRATOR → 全局管理员,全部位 on
         + ChannelPermission 覆盖          // 优先级最高: user > role, deny > allow
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/servers` | 创建 Server（自动创建默认角色/分类/频道） |
| GET | `/servers` | 获取我加入的 Server 列表 |
| PUT | `/servers/{id}` | 更新 Server 信息 |
| DELETE | `/servers/{id}` | 删除 Server（仅 owner） |
| POST | `/servers/{id}/categories` | 创建分类 |
| GET | `/servers/{id}/categories` | 获取分类列表 |
| POST | `/categories/{id}/channels` | 创建频道 |
| GET | `/channels/{id}` | 获取频道详情 |
| PUT | `/channels/{id}` | 更新频道 |
| DELETE | `/channels/{id}` | 删除频道 |
| POST | `/servers/{id}/invites` | 创建邀请码 |
| POST | `/invites/{code}/join` | 通过邀请码加入 |
| GET | `/servers/{id}/members` | 获取成员列表 |
| PUT | `/servers/{id}/members/{uid}/role` | 修改成员角色 |

### 编译验证
```bash
mvn clean compile -pl community-server -am  # BUILD SUCCESS
```

---

## Phase 3.1: 代码审查修复 + Phase 3-4 方案细化 (2026-06-22)

**Commit**: `49db1c7` fix: 5 项代码审查修复 + Phase 3-4 方案细化

### 5 项修复

| # | 问题 | 文件 | 修复 |
|---|------|------|------|
| 1 | `permission` 包缺少 `Permission` 接口 | 新建 `Permission.java` | 接口抽象，`PermissionBit` + `ChannelPermission` 双实现 |
| 2 | `PermissionParser` 未泛化 | `PermissionParser.java` | 改为 `Permission` 接口级别工具 |
| 3 | Server 创建事务边界不完整 | `ServerServiceImpl.java` | `@Transactional` 统一包裹创建 Server + 默认角色 + 默认分类 + 默认频道 |
| 4 | Token 续期逻辑缺失 | `TokenInterceptor.java` | 响应头自动追加新 Token |
| 5 | 全局异常处理器未覆盖 MethodArgumentNotValidException | `GlobalExceptionHandler.java` | 追加 `@ExceptionHandler` |

### 方案细化
- `05-dev-phases-0-4.md` Phase 3 和 Phase 4 章节大幅扩写：checkbox 从 ~20 项增至 ~120 项，补充每种消息类型的处理策略、Reaction 分组查询、Thread 嵌套关联、已读追踪表结构等细节
- 与 MallChat plan 再次比对，确认 Phase 3-4 覆盖无遗漏

---

## Phase 4: 消息策略链 + Thread + Reaction + 已读追踪 (2026-06-22)

**Commit**: `632e093` feat(phase4): 消息策略链 + Thread + Reaction + 已读追踪

### 目标

实现社群平台核心二：消息策略模式（8 种消息类型）、Thread（频道内子话题）、Reaction（快捷表情反馈）、已读状态追踪。

### 新增/修改文件（28 个）

#### 消息策略链 (`community/message/`)
| 文件 | 说明 |
|------|------|
| `AbstractMsgHandler.java` | 抽象消息处理器，定义 `process(msg)` 模板 |
| `MsgHandlerFactory.java` | 策略工厂，`@PostConstruct` 自动注册所有 Handler |
| `TextMsgHandler.java` | 文本消息处理 |
| `ImgMsgHandler.java` | 图片消息处理 |
| `FileMsgHandler.java` | 文件消息处理 |
| `VideoMsgHandler.java` | 视频消息处理 |
| `RecallMsgHandler.java` | 撤回消息处理 |
| `SystemMsgHandler.java` | 系统消息处理（成员加入/退出/角色变更等） |
| `Message.java` | 消息聚合根 Entity |
| `MessageExtra.java` | 消息扩展信息 (reactions, mentions, attachments JSON) |
| `MessageDao.java` | 消息 DAO |
| `MessageDaoImpl.java` | 实现：游标分页查询 |
| `MessageServiceImpl.java` | 消息发送全链路：权限校验 → 频控 → 持久化 → MQ → WS 推送 |
| `MessageController.java` | REST API 端点 |
| `MsgTypeEnum.java` | 消息类型枚举 (TEXT=1, IMAGE=2, FILE=3, VIDEO=4, SOUND=5, EMOJI=6, RECALL=7, SYSTEM=8) |

#### Thread (`community/thread/`)
| 文件 | 说明 |
|------|------|
| `Thread.java` | Thread Entity (channelId, title, creatorId, lastActive, messageCount, ...) |
| `ThreadDao.java` | Thread DAO |
| `ThreadServiceImpl.java` | Thread CRUD + 游标分页 |
| `ThreadController.java` | REST API 端点 |

#### Reaction (`community/reaction/`)
| 文件 | 说明 |
|------|------|
| `Reaction.java` | Reaction Entity (messageId, userId, emoji, ...) |
| `ReactionDao.java` | Reaction DAO |
| `ReactionServiceImpl.java` | 添加/移除/批量查询 Reaction |
| `ReactionController.java` | REST API 端点 |

#### 已读追踪
| 文件 | 说明 |
|------|------|
| `ChannelReadState.java` | 已读状态 Entity (userId, channelId, lastReadTime, ...) |
| `ChannelReadStateDao.java` | DAO |
| `ChannelReadStateService.java` | 更新/查询未读计数 |

### 消息发送全链路

```
POST /channels/{id}/messages
  → TokenInterceptor (JWT 验证)
    → PermissionInterceptor (SEND_MESSAGES 权限检查)
      → FrequencyControlService (3 层频控: 5s/30s/60s)
        → MsgHandlerFactory.get(type).process(msg)
          → AC 自动机敏感词过滤（预留扩展点）
            → messageDao.save(msg)
              → RocketMQ → PushService → WebSocket 推送在线用户
```

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/channels/{id}/messages` | 发送消息 |
| GET | `/channels/{id}/messages` | 游标分页获取消息列表 |
| DELETE | `/messages/{id}` | 删除消息 |
| PUT | `/messages/{id}` | 编辑消息 |
| POST | `/messages/{id}/threads` | 从消息创建 Thread |
| GET | `/channels/{id}/threads` | 获取频道下 Thread 列表（按 last_active DESC） |
| DELETE | `/threads/{id}` | 删除 Thread |
| POST | `/messages/{id}/reactions` | 添加 Reaction (emoji) |
| DELETE | `/messages/{id}/reactions/{emoji}` | 移除 Reaction |
| POST | `/channels/{id}/read` | 标记频道已读 |

### 编译验证
```bash
mvn clean compile -pl community-server -am  # BUILD SUCCESS, 184 source files
```

---

## Phase 4.1: 方案-代码差异比对 (2026-06-22)

**Commit**: `1889ee8` docs: 记录 Phase 4 方案-代码 9 项差异

### 比对结论

逐文件比对 `05-dev-phases-0-4.md` Phase 4 章节 vs 实际代码，发现 9 项差异：

| # | 类别 | 差异 | 说明 |
|---|------|------|------|
| 1 | 缺失 | SoundMsgHandler 未实现 | 语音消息处理器，P1 优先级 |
| 2 | 缺失 | EmojiMsgHandler 未实现 | 自定义表情消息处理器 |
| 3 | 缺失 | MentionParser 未实现 | @提及正则提取 + uid 解析 |
| 4 | 行为 | getThreads 按 id 排序 | 计划要求按 last_active DESC |
| 5 | 行为 | 消息列表不附带 reactions | 计划要求 MessageVO 含 ReactionVO 列表 |
| 6 | 行为 | ThreadVO 不含 creator 信息 | 计划要求含 creator UserVO |
| 7 | 行为 | 消息编辑未实现 | 计划有 PUT /messages/{id} 端点 |
| 8 | 行为 | 频道已读仅手动标记 | 计划含自动标记（收到消息时） |
| 9 | 命名 | CursorPageBaseReq vs 计划命名 | 与 MallChat 保持一致 |

差异 #1-3 为计划内未实现的 P1-P2 功能，已记录在待办。差异 #4-8 为行为偏差，在下一 commit 修复。差异 #9 为命名选择，保留现状。

### 记录位置
所有差异已写入 `05-dev-phases-0-4.md` §4.0.1 实现差异记录表。

---

## Phase 4.2: 3 项行为修复 (2026-06-22)

**Commit**: `b71004f` fix: Phase 4 3 项行为修复 — getThreads 排序/消息列表 reactions/ThreadVO creator

### 修复详情

| # | 修复 | 文件 | 变更 |
|---|------|------|------|
| 1 | getThreads 按 last_active DESC 排序 | `ThreadServiceImpl.java` | `wrapper.orderByDesc(Thread::getLastActive)` |
| 2 | 消息列表附带 reactions | `MessageServiceImpl.java` | 新增 `buildReactionMap(List<Long> msgIds)` 方法，批量查询 + 按 emoji 分组 + 设置 reacted 标记 |
| 3 | ThreadVO 含 creator 信息 | `ThreadServiceImpl.java` | `toThreadVO()` 接受 `User creator` 参数，`getThreads()` 批量查找所有 creator |

### 修复 #2 设计细节

```java
// buildReactionMap 逻辑
1. reactionDao.lambdaQuery().in(Reaction::getMessageId, msgIds).list()
2. 按 messageId 分组 → 每组内按 emoji 分组
3. 构建 Map<Long, List<ReactionVO>>:
   - count = 每个 emoji 下的 userId 数量
   - userIds = 前 5 个 userId 用于前端预览
   - reacted = 当前用户是否在 userIds 中
```

### 编译验证
```bash
mvn clean compile -pl community-server -am  # BUILD SUCCESS
```

---

## Phase 4.3: 启动调试 — 8 项修复 (2026-06-22)

**Commits**: `96f0496` fix: 启动调试修复, `d1ab4ae` docs: 记录 Phase 4 启动调试 8 项修复

### 目标

首次启动应用并验证 API 可用。遇到 8 项阻碍并逐一修复。

### 8 项启动问题

| # | 问题 | 根因 | 修复 |
|---|------|------|------|
| 1 | RocketMQTemplate Bean 缺失，WxMsgService 启动失败 | `spring.autoconfigure.exclude` 排除了 RocketMQAutoConfiguration | 移除 exclude 项 |
| 2 | Redis WRONGPASS | 配置文件密码 `redis123` ≠ Docker 实际密码 `123456` | 配置改为 `123456` |
| 3 | Redis 端口连接失败 | 配置 `6379`，Docker 映射到 `6380` | 配置改为 `6380` |
| 4 | User 表 `open_id` 列缺失 | Java Entity 有 `openId` 字段，DB 未建该列 | `ALTER TABLE user ADD COLUMN open_id VARCHAR(64), ADD COLUMN union_id VARCHAR(64)` |
| 5 | `channel_read_state` 表不存在 | Phase 4 DDL 未执行 | 手动建表 |
| 6 | Netty WebSocket `Address already in use: 8090` | MallChat 占用 8090 | `NettyWebSocketServer.WEB_SOCKET_PORT` 改为 `8091` |
| 7 | `GET /servers/{id}/unread` 返回 404 | 未实现未读端点 | 在 `ServerController` 添加 `/unread` 端点 |
| 8 | Thread 创建请求体中文乱码 | Windows curl 默认 GBK 编码 | 用英文测试；后续应确保 UTF-8 |

### 调试成功验证

使用 curl 验证了 12 个端点，确认 Phase 1-4 全部核心 API 可用：

```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"alice","password":"123456"}'

# Server CRUD
curl -X POST http://localhost:8080/api/v1/servers -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"name":"Test Server"}'
curl http://localhost:8080/api/v1/servers -H "Authorization: Bearer <token>"

# 消息发送
curl -X POST http://localhost:8080/api/v1/channels/{id}/messages -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"type":1,"content":"Hello"}'

# ... 共 12 个端点
```

---

## Phase 4.4: Docker 部署 (2026-06-22)

**Commit**: `1f7d513` feat: Docker 部署 — Dockerfile + docker-compose + 独立端口

### 目标

让其他人无需本地安装 Java 21 + Maven 即可运行调试后端，一键 `docker compose up -d`。

### 新增文件

| 文件 | 说明 |
|------|------|
| `Dockerfile` | 基于 `eclipse-temurin:21-jre`，复制 JAR → `java -jar app.jar` |
| `docker-compose.yml` | 6 服务编排：mysql + redis + minio + rocketmq-namesrv + rocketmq-broker + app |
| `application-docker.yml` | Docker 环境配置，所有 host 使用 Docker 服务名 |
| `broker.conf` | RocketMQ Broker 配置（autoCreateTopic=true） |
| `.dockerignore` | 排除 target/build 目录 + IDE 文件 |

### 端口规划（避免与 MallChat 冲突）

| 服务 | MallChat Docker | 社区平台 Docker | 
|------|----------------|-----------------|
| API (Spring Boot) | 8080:8080 | 8081:8080 |
| WebSocket (Netty) | 8090:8090 | 8092:8091 |
| MySQL | 3307:3306 | 3308:3306 |
| Redis | 6379:6379 | 6381:6379 |
| MinIO API | 9002:9000 | 9004:9000 |
| MinIO Console | 9003:9001 | 9005:9001 |
| RocketMQ NameServer | 9876:9876 | 9878:9876 |
| RocketMQ Broker | 10909+10911 | 10919+11911 |

### 启动方式

```bash
mvn clean package -pl community-server -am -DskipTests
docker compose up -d
# API: http://localhost:8081/swagger-ui/index.html
# WebSocket: ws://localhost:8092/ws?token=xxx
```

### Docker Compose 关键设计
- MySQL 自动建表：`./docs/ddl.sql` 挂载到 `/docker-entrypoint-initdb.d/01-init.sql`
- 健康检查：MySQL (`mysqladmin ping`)、Redis (`redis-cli ping`)、MinIO (`curl /minio/health/live`)
- App 容器 `depends_on` mysql + redis 的健康检查条件

### 构建验证
```bash
docker compose up -d  # 6 服务全部启动成功
```

---

## Phase 4.5: README 重写 (2026-06-22)

**Commit**: `25e7dbe` docs: 重写 README — 启动指南 + 功能清单

### 变更前

旧 README 严重过时（240 行）：
- 引用了不存在的 Nacos / Spring Cloud
- `./mvnw`（项目无 Maven Wrapper）
- WebSocket 端口标 8090（实际 8091）
- 接口文档用 Knife4j doc.html（实际 Swagger UI）
- DDL 标"待创建"（已存在）
- "下一步"清单全部完成但未更新

### 变更后

全重写为 290 行精准指南：

| 章节 | 内容 |
|------|------|
| 项目简介 | 一句话定位：Discord-like 社群平台 |
| 技术栈 | 准确版本号（Spring Boot 3.3.5, Java 21, MyBatis-Plus 3.5.10.1...） |
| 快速开始 | Docker（推荐）+ 本地开发 两种方式，含完整命令 |
| 测试账号 | alice/bob/charlie，密码 123456 |
| 已实现功能 | Phase 1-4 功能清单表（4 表，60+ 项） |
| 尚未实现 | Phase 4.x+ 待补项清单 |
| API 概览 | 50+ 个端点分类一览 |
| 项目结构 | 包目录树 |
| 核心设计 | RBAC 权限模型 + 消息链路 + 游标分页 |
| 与 MallChat 的关系 | 复用/重写对照表 |

### 关键修正

| 旧 | 新 | 原因 |
|----|-----|------|
| Nacos / Spring Cloud | 单体应用 | Phase 1-7 单体，Phase 8 微服务 |
| `./mvnw` | `mvn` | 无 Maven Wrapper |
| WS 8090 | 8091 | 避免与 MallChat 冲突 |
| Knife4j doc.html | swagger-ui/index.html | 实际集成 Swagger UI |
| DDL "待创建" | Docker 自动建表 / 本地手动执行 | 已存在 |
| 14 位权限含 MANAGE_EMOJI | 13 位 | MANAGE_EMOJI 枚举中不存在 |

---

## Phase 4.6: CI/CD — GitHub Actions 编译 + 发布 GHCR (2026-06-22)

**Commit**: `0de68cf` feat: CI/CD — GitHub Actions 编译 + 打包推送 GHCR

### 目标

PR 自动编译检查 + push main 自动打包 Docker 镜像并推送到 GitHub Container Registry (ghcr.io)，让其他人无需本地 JDK/Maven 即可 `docker compose pull && up` 运行。

### 新增/修改文件

| 文件 | 说明 |
|------|------|
| `.github/workflows/build.yml` | CI 流水线：编译 → 打包 JAR → docker build → push GHCR |
| `docker-compose.yml` | app 镜像改为 `ghcr.io/zzmkxd/community-platform:latest` |
| `README.md` | 新增 `docker compose pull` 无 JDK 启动方式 + CI/CD 条目 |

### 流水线设计

```
Pull Request → main     compile (mvn compile, JDK 21)

Push to main            compile → package (mvn package -DskipTests)
                              → docker/build-push-action → ghcr.io
```

### 关键设计决策

- **无 Maven Wrapper**：直接用 `mvn` 命令（`actions/setup-java@v4` 内置 Maven）
- **GHCR 认证**：`${{ secrets.GITHUB_TOKEN }}` 自动注入，无需额外配置
- **push 权限**：`packages: write` 允许推送容器镜像
- **两阶段**：compile 阶段 PR/push 都跑，build-and-push 仅 push main 时执行

### 使用方式

其他人克隆后无需 JDK/Maven：
```bash
docker compose pull app   # 拉取 CI 构建的最新镜像
docker compose up -d      # 启动全栈
```

---

## 待办：后续升级清单

> 以下条目在 Phase 0-2 中因优先级、复杂度或外部依赖而被推迟，Phase 3+ 或重构时重新评估。

### 阻塞性待办（启动前必须解决）

| # | 条目 | 阻塞场景 | 说明 |
|---|------|---------|------|
| ~~T1~~ | ~~BCrypt seed data 哈希~~ | ✅ 已完成 (2026-06-22) | `ddl.sql` 中占位符已替换为真实 BCrypt hash |
| ~~T2~~ | ~~应用配置 微信凭据~~ | ✅ 已完成 (2026-06-22) | 微信凭据已填入，复用 MallChat 公众号 |

### 功能缺口（按 Phase 推进时补齐）

| # | 条目 | Phase | 说明 |
|---|------|-------|------|
| ~~T3~~ | ~~Invite 模块（P0）~~ | ✅ Phase 3 | 7 个文件：Entity + Mapper + Dao + VO + Service + Controller |
| T4 | SoundMsgHandler + EmojiMsgHandler（P1） | Phase 5 | 语音/自定义表情消息处理 |
| T5 | MentionParser（P1） | Phase 5 | @提及正则提取 + uid 解析 |
| ~~T6~~ | ~~ChannelReadState 全套（P1）~~ | ✅ Phase 4 | Entity + Mapper + Dao + Service + 未读计数 API |
| T7 | PushService（P1） | Phase 4.x | MQ 推送路由，RocketMQTemplate 封装。**2026-06-22 审计发现：误标 ✅，实际未实现** |
| T8 | AC 自动机敏感词（P2） | Phase 5 | `common/sensitive/` 移植 MallChat ACTrie |
| T9 | WxMsg 持久化表（P2） | 后续按需 | 微信原始消息审计（MallChat 有，社区平台当前跳过） |
| ~~T10~~ | ~~docker-compose.yml（P2）~~ | ✅ Phase 4.4 | MySQL 3308 + Redis 6381 + RocketMQ 9878 + MinIO 9004-5 |
| ~~T11~~ | ~~broker.conf（P2）~~ | ✅ Phase 4.4 | RocketMQ Broker 配置 |

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
