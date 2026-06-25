# 交接文档：Spring Boot 单体 → Spring Cloud Alibaba 微服务

> 最后更新：2025-06-25  
> 分支：`zjwSpringCloud`  
> 状态：5 个微服务全部可启动，OpenFeign 跨服务调用已集成，待端到端验证

---

## 1. 项目概览

Discord-like 社群平台后端。Spring Boot 3.3.5 + Java 21 + Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.1.0。

```
community-platform/
├── community-common/          共享基础设施 JAR（DTO/VO/枚举/异常/工具/Feign接口/配置/切面）
├── community-user-service/    用户服务 :8081（登录/注册/JWT/用户资料）
├── community-server-service/  社群服务 :8082（Server/Category/Channel/成员/角色/权限）
├── community-message-service/ 消息服务 :8083（消息CRUD/Thread/Reaction/搜索/已读状态）
├── community-file-service/    文件服务 :8084（MinIO预签名上传/文件关联）
├── community-websocket/       WebSocket :8090（Netty连接管理/消息推送）
├── community-gateway/          API 网关 :8080（JWT 鉴权 + 路由转发）
└── docs/
    ├── plan.md                项目计划
    ├── dev-log.md             开发日志
    ├── ddl.md                 数据库设计
    └── handoff.md             本文档
```

## 2. 基础设施（Docker）

| 服务 | 端口 | 用途 |
|------|------|------|
| MySQL 8 | 3308 | 业务数据 |
| Redis 7 | 6381 | 缓存/Token/频道订阅 |
| Nacos 2.3.2 | 8848 | 服务注册 + 配置中心 |
| MinIO | 9004/9005 | 对象存储 |
| RocketMQ | 9878 | 消息推送异步解耦 |

启动：`docker compose up -d`

## 3. Nacos 配置

- 地址：`http://localhost:8848/nacos`
- 共享配置：`community-platform-common.yaml`（含 Redis、datasource、RocketMQ 等公共配置）
- **注意**：datasource 配置在共享配置里，所有服务都会自动创建 DataSource。websocket 除外（已排除）

## 4. 启动命令

```bash
# 首次或修改 common 源码后：
mvn install -DskipTests

# 推荐方式（-am 自动编译 common，不需要 mvn install）：
mvn -pl community-gateway -am spring-boot:run
mvn -pl community-user-service -am spring-boot:run
mvn -pl community-server-service -am spring-boot:run
mvn -pl community-message-service -am spring-boot:run
mvn -pl community-file-service -am spring-boot:run
mvn -pl community-websocket -am spring-boot:run
```

**注意**：Nacos 必须先启动。Gateway 启动后所有对外 API 统一走 `localhost:8080`。

## 5.1 Spring Cloud Gateway 网关

- **端口**：8080，统一入口
- **JWT 鉴权**：`AuthGlobalFilter` 解析 `Authorization: Bearer <token>`，注入 `X-Uid` header 给下游
- **白名单**：`/api/v1/auth/login`、`/api/v1/auth/register` 无需鉴权
- **Feign 内部调用**：`/internal/*` 直通，不鉴权
- **下游适配**：`TokenFilter`（common）、`TokenInterceptor`（user-service）优先读 `X-Uid`，Gateway 已鉴权则跳过 JWT 解析
- **路由**：精确 Path 匹配 + `lb://service-name`
  ```
  /api/v1/auth/**          → community-user-service
  /api/v1/users/**         → community-user-service
  /api/v1/servers/*/search/** → community-message-service  (必须在 /servers/** 前)
  /api/v1/channels/*/messages/** → community-message-service
  /api/v1/channels/*/threads/** → community-message-service
  /api/v1/messages/*/reactions/** → community-message-service
  /api/v1/upload/**        → community-file-service
  /api/v1/servers/**       → community-server-service
  /api/v1/channels/*/permissions/** → community-server-service
  ```

### 5.2 OpenFeign 跨服务调用

```
消费者服务                             提供者服务
┌──────────┐    Feign(HTTP)     ┌──────────────────┐
│ @Enable  │ ───→ UserService ──→│ @RestController  │
│ Feign    │    (Feign接口在     │ @RequestMapping  │
│ Clients  │     common中)       │ ("/internal/...")  │
└──────────┘                    └──────────────────┘
```

- **Feign 接口**：定义在 `community-common`，用 `@FeignClient` + `@GetMapping/@PostMapping` + `@RequestBody/@RequestParam/@PathVariable`
- **实现类**：各服务模块中，`@RestController` + `@RequestMapping("/internal/xxx")` 实现接口
- **`contextId`**：同一服务有多个 Feign 接口时必须用 `contextId` 区分（如 `ChannelService` 和 `MemberService` 都指向 `community-server-service`，分别设 `contextId = "channelService"` / `"memberService"`）
- **LoadBalancer**：`spring-cloud-starter-loadbalancer` 必须**显式**声明在 `community-common/pom.xml`。Spring Cloud 2023.0.3 的 OpenFeign 4.1.3 不再传递此依赖
- **`lazy-attributes-resolution: true`**：每个消费端 application.yml 都加了这个配置，防止 LoadBalancer 解析过早

### 5.3 消费者 Feign 客户端声明

```java
// 按需声明，不列出本地实现的接口
@EnableFeignClients(clients = {UserService.class, FileService.class, PushService.class, ...})
```

| 服务 | 依赖的 Feign 客户端 |
|------|-------------------|
| server-service | `UserService`, `PushService`, `ChannelReadStateService` |
| message-service | `UserService`, `FileService`, `PushService`, `ChannelService`, `PermissionService` |
| websocket | `UserService`, `MemberService` |
| user-service | 无（纯提供者） |
| file-service | 无（纯提供者） |

### 5.4 `@MapperScan` 配置

所有有 DB 的服务都需要扫两个包：
```java
@MapperScan({"com.community.<own>.dao.mapper", "com.community.common.sensitive.dao.mapper"})
```

websocket 无 DB，不需要任何 `@MapperScan`。

## 6. 关键踩坑记录

### 6.1 ⚠️ 参数注解不随 @Override 继承（最重要！）

```java
// ❌ 错误：Feign 接口上有 @RequestBody，但实现类参数上没有
@Override
public UserVO login(LoginReq req) { ... }  // req 永远是空对象

// ✅ 正确：实现类参数上也必须写 @RequestBody
@Override
@PostMapping("/login")
public UserVO login(@RequestBody LoginReq req) { ... }
```

**Java 不继承方法参数上的注解**。`@RequestBody`、`@RequestParam`、`@PathVariable` 必须在 `@RestController` 实现类的方法参数上**显式再写一遍**。方法级别的 `@GetMapping`/`@PostMapping` 也要写（Spring 会从接口继承，但显式写更清晰）。

### 6.2 `@ConditionalOnBean` 不能用在 `@Configuration` 类上

用户 `@Configuration` 类处理时机**早于**自动配置类（如 `DataSourceAutoConfiguration`），此时 DataSource bean 还未注册，条件永远为 false。

```java
// ❌ 错误：@Configuration 上不能用 @ConditionalOnBean
@Configuration
@ConditionalOnBean(DataSource.class)  // 时序问题，总是 false

// ✅ 正确：用 @ConditionalOnClass（检查 classpath）
@Configuration
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")  // classpath 检查，无时序问题
```

### 6.3 websocket 服务排除 DataSource

```java
// websocket 无 MySQL，必须排除，否则 Nacos 共享配置中的 datasource 会触发自动创建
@SpringBootApplication(
    scanBasePackages = {"com.community.websocket", "com.community.common"},
    exclude = {DataSourceAutoConfiguration.class}
)
```

### 6.4 common 中依赖 DB 的 bean 统一条件注解

| 类 | 条件 |
|----|------|
| `SensitiveWordDao` | `@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")` |
| `SensitiveWordConfig` | `@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")` |
| `TransactionAutoConfiguration` | `@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")` |
| `SecureInvokeAspect` | 无 `@Component`，由 `TransactionAutoConfiguration` 按需 `@Import` |
| `SecureInvokeRecordDao` | 无 `@Component`，同上 |

### 6.5 Maven 本地仓库路径

`D:\workplace\maven\.m2\repository`（不在默认 `~/.m2`）

## 7. 调用链路参考

```
客户端请求
  │
  ├── POST /api/v1/channels/{id}/messages
  │   └── message-service(8083)
  │       ├── [Feign] UserService     → user-service(8081)
  │       ├── [Feign] ChannelService  → server-service(8082)
  │       ├── [Feign] PermissionService → server-service(8082)
  │       ├── [Feign] FileService     → file-service(8084)
  │       └── [Feign] PushService     → websocket(8090)
  │           └── RocketMQ → PushConsumer → WebSocket 推送
  │
  ├── POST /api/v1/servers
  │   └── server-service(8082)
  │       ├── [Feign] UserService → user-service(8081)
  │       └── [Feign] PushService → websocket(8090)
  │
  └── GET /api/v1/servers/{id}/members
      └── server-service(8082)
          └── [Feign] UserService → user-service(8081)
```

## 8. 待完成事项

| # | 任务 | 优先级 |
|---|------|--------|
| 1 | 端到端验证：登录 alice → 创建 Server → 发消息 → WebSocket 推送 | 🔴 高 |
| 2 | 升级 Spring Cloud Alibaba → 2023.0.3.4 | 🟡 中 |
| 3 | Spring Cloud Gateway 网关模块（统一入口、鉴权、限流） | ✅ 已完成 |
| 4 | Nacos 共享配置拆分：datasource 移出共享配置，各服务自管 | 🟢 低 |
| 5 | `mvn -pl <service> -am spring-boot:run` 替代 `mvn install` 的 CI 脚本化 | 🟢 低 |

## 9. 新对话快速启动

```
我在做 Spring Boot → Spring Cloud Alibaba 微服务迁移项目。
项目路径：D:\GitHubRepository\community_platform-1
分支：zjwSpringCloud
请先阅读 docs/handoff.md 了解当前状态，然后继续工作。
```
