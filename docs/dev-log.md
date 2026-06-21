# 社群平台 开发日志

## 项目信息

- **项目名称**: community-platform (社群平台)
- **目标**: Discord-like 社群平台，供软件实训课程使用
- **技术栈**: Spring Boot 3.3.5 + Java 21 + Netty + MyBatis-Plus + Redis + RocketMQ + MinIO
- **项目管理**: Git + Maven (多模块)
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

### 待实现 (TODO)

所有 Service 实现方法目前标记为 `throw new UnsupportedOperationException("TODO")`，
下一步按以下顺序实现：

1. **用户模块**: 注册/登录/JWT
2. **Server 模块**: Server/Channel/Category CRUD
3. **Member + Role + Permission**: RBAC 权限系统
4. **Message**: 消息发送/分页/编辑/删除
5. **Thread + Reaction**: 话题 + 反应
6. **File**: MinIO 预签名上传
7. **WebSocket**: 频道订阅推送完善
8. **DDL**: 数据库建表脚本

### Git

```bash
git init
git remote add origin <url>
```

---

## 参考

- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
- 项目计划: `docs/plan.md` (来自 MallChat/plan/)
- 前端计划: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\plan_front\`
