# 八、微服务升级路径（Phase 7 之后）

> 本文档是"未来某天"的操作手册。Phase 0-7 期间不用碰。

---

## 前置条件

升级前必须满足：

1. **Phase 0-7 全部完成**，功能稳定且冒烟测试通过
2. **包级接口隔离已审计通过**（`plan/04-migration.md` 5.2 节定义的规则）
3. **Spring Cloud BOM 已在根 pom.xml 声明**（Phase 0 就已做，零成本）

---

## 一、模块物理拆分

### 1.1 拆分前后对照

```
拆分前 (单体):
  community-server/
    └── src/main/java/com/community/
        ├── CommunityApplication.java
        ├── common/          # 基础设施
        ├── user/            # 用户包
        ├── server/          # 社群包
        ├── message/         # 消息包
        ├── file/            # 文件包
        └── websocket/       # WebSocket

拆分后 (7 模块):
  community-common/            # JAR — 从 common/ 提取
  community-transaction/       # JAR — @SecureInvoke + MQProducer
  community-user-service/      # user/ 搬过来 + 启动类
  community-server-service/    # server/ 搬过来 + 启动类
  community-message-service/   # message/ 搬过来 + 启动类
  community-file-service/      # file/ 搬过来 + 启动类
  community-websocket/         # websocket/ + Netty 独立进程
```

### 1.2 操作步骤（以 user-service 为例，其余同理）

**Step 1**：创建模块目录
```
community-user-service/
  pom.xml
  src/main/java/com/community/user/
  src/main/resources/application.yml
```

**Step 2**：pom.xml 只引自己需要的依赖
```xml
<dependencies>
    <dependency>
        <groupId>com.community</groupId>
        <artifactId>community-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    <!-- 不需要 server/message/file 的依赖 -->
</dependencies>
```

**Step 3**：把 `user/` 包下代码原样搬过去。不改任何业务逻辑。

**Step 4**：创建启动类
```java
@SpringBootApplication
@ComponentScan("com.community")
@MapperScan("com.community.user.**.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**Step 5**：重复以上步骤，给 server / message / file / websocket 各创建模块。

---

## 二、Service 接口 → FeignClient

这是唯一需要改业务代码的地方。因为 Phase 0-7 严格遵循"跨包只调 Service 接口"，
所以每个跨服务依赖只需一行注入类型切换。

### 2.1 需要 Feign 化的接口（3 处）

| 调用方 | 被调方 | 接口方法 | 用途 |
|--------|--------|---------|------|
| message-service | server-service | `MemberService.isMember()` | 发消息前校验成员 |
| message-service | server-service | `PermissionService.checkPermission()` | 发消息前校验权限 |
| message-service | user-service | `UserService.getBatchUsers()` | 构建 MessageVO 时获取用户信息 |

### 2.2 操作步骤（以 MemberService 为例）

**被调方 (server-service)**——新增一个 Feign 实现：

```java
// server-service 新增文件：
@FeignClient(name = "server-service", path = "/internal/server")
public interface MemberServiceClient extends MemberService {
    // 继承现有 MemberService 接口的所有方法签名
    // Feign 自动生成 HTTP 代理实现
}
```

**被调方 (server-service)**——暴露 REST 端点：

```java
// 新增 InternalMemberController，实现同样的接口逻辑
@RestController
@RequestMapping("/internal/server")
public class InternalMemberController {
    @Autowired
    private MemberService memberService; // 真实的 ServiceImpl

    @GetMapping("/members/{serverId}/check/{userId}")
    public boolean isMember(@PathVariable Long serverId, @PathVariable Long userId) {
        return memberService.isMember(serverId, userId);
    }
}
```

**调用方 (message-service)**——注入类型切换：

```java
// ====== 单体时期 ======
@Autowired
private MemberService memberService;

// ====== 微服务时期 ======
@Autowired
private MemberServiceClient memberService;  // Feign 代理

// 其他代码不动！memberService.isMember() 调用方式完全相同
```

### 2.3 对另外两个接口重复同样操作

- `PermissionService` → `PermissionServiceClient` + rest 端点 `/internal/server/permissions/check`
- `UserService.getBatchUsers()` → `UserServiceClient` + rest 端点 `/internal/user/batch`

---

## 三、基础设施：Nacos + Gateway

### 3.1 Nacos

docker-compose 加一行：
```yaml
nacos:
  image: nacos/nacos-server:v2.3.2
  ports:
    - "8848:8848"
    - "9848:9848"
  environment:
    - MODE=standalone
```

每个服务加依赖：
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

每个服务 `application.yml` 加：
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

至此所有服务自动注册到 Nacos，互相发现。

### 3.2 Gateway

新建 `gateway-server` 模块：

```
gateway-server/
  pom.xml
  src/main/java/.../GatewayApplication.java
  src/main/resources/application.yml
```

路由配置（<50 行）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates: Path=/api/v1/auth/**,/api/v1/users/**
        - id: server-service
          uri: lb://server-service
          predicates: Path=/api/v1/servers/**,/api/v1/invites/**
        - id: message-service
          uri: lb://message-service
          predicates: Path=/api/v1/channels/**,/api/v1/messages/**,/api/v1/threads/**
        - id: file-service
          uri: lb://file-service
          predicates: Path=/api/v1/upload/**,/api/v1/files/**
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin

server:
  port: 8080
```

JWT 校验 → 在 Gateway 层做一个全局过滤器，提取 Header Token → 调 user-service 验证 → 放行/拦截。或继续在各自服务拦截器中做（与单体一致）。

### 3.3 WebSocket 不经过 Gateway

Netty 端口 8090 直连，不走 HTTP Gateway。WS 认证 Token 通过 URL 参数传递，与 MallChat 一致。

---

## 四、配置拆分

### 4.1 拆分策略

| 配置项 | 放置位置 |
|--------|---------|
| logging, serialization | 各服务 application.yml（独立） |
| MySQL, Redis, RocketMQ | Nacos 配置中心（共享） |
| wx.mp (appId/secret) | user-service 独享 |
| minio | file-service 独享 |
| elasticsearch | message-service 独享 |
| jwt.secret | Nacos 配置中心（共享） |

### 4.2 拆分后各服务端口

```
gateway-server         : 8080  (HTTP 统一入口)
user-service           : 8081
server-service         : 8082
message-service        : 8083
file-service           : 8084
community-websocket    : 8090  (Netty WS, 直连)
```

---

## 五、分库（可选，Phase 8+）

当前方案下所有服务连同一个 MySQL 实例，同一个 schema `community_platform`。

如果需要分库，按业务域拆：

```
community_platform_user    → user-service
community_platform_server  → server-service
community_platform_message → message-service
community_platform_file    → file-service
```

触发条件：单表数据量 > 500 万行 或 QPS > 2000。在此之前分库没有实际收益，只会增加分布式事务复杂度。

---

## 六、验证清单

- [ ] 所有服务注册到 Nacos 控制台
- [ ] Gateway 路由分发正确（curl Gateway 端口，各路径路由到对应服务）
- [ ] WebSocket 连接 8090 → 扫码登录 → LOGIN_SUCCESS 正常
- [ ] 发消息全链路：REST → Gateway → message-service → Feign → server-service/checkPermission → MQ → websocket → 前端
- [ ] Phase 7 冒烟测试脚本全部通过
- [ ] 断点调试：各服务单步跟踪，Feign 调用链正确

---

## 七、工作量估算

| 工作类别 | 估时 | 难度 |
|---------|------|------|
| 5 个模块创建 (pom.xml + 启动类 + 代码搬运) | 2.5h | 机械 |
| 3 个 Feign Client + REST 端点 | 3h | 中等 |
| Nacos + Gateway 搭建 | 3h | 低 |
| 配置拆分 + 端口分配 | 2h | 低 |
| 调试 + 修复 | 1 天 | 中等 |
| **合计** | **2~3 天** | |

对比无接口隔离的传统巨石拆分（需要先重构跨包 DAO 引用、修复循环依赖、逐个接口拆 Feign），预计节省 **50% 以上**工作量。
