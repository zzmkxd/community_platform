# Community Platform (社群平台)

Discord-like 社群平台后端。Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Nacos + Gateway + Feign + Netty WebSocket + MyBatis-Plus + Redis + RocketMQ。

## 常用命令

```bash
# 一键启动（Windows / Linux）
scripts\start-all.bat          # Windows: Maven 打包 → Docker 构建 → 3 波分段启动
bash scripts/start-all.sh      # macOS / Linux 同上

# 手动启动基础设施（独立端口，与 MallChat 不冲突）
docker compose up -d

# 首次启动需发布 Nacos 共享配置（有 2 种方式）
# 方式 A: docker compose up -d nacos-init 会自动发布模板（含占位符，Docker 模式用）
# 方式 B: 手动 POST 到 Nacos（本地开发用，需替换为 localhost 硬编码值）
#   配置模板: docs/nacos-shared-config.yaml
#   注意: 本地运行时需在 Nacos 中配置 community.redis.host / community.jwt.secret 等自定义属性

# 本地启动单个微服务（需 6 个终端，从 Gateway 统一入口）
# 每个服务 POM 已配 <skip>false</skip> 覆盖根 POM 的 <skip>true</skip>
mvn -pl community-gateway spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl community-user-service -am spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl community-server-service -am spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl community-message-service -am spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl community-file-service -am spring-boot:run -Dspring-boot.run.profiles=local
mvn -pl community-websocket -am spring-boot:run -Dspring-boot.run.profiles=local

# 冒烟测试（17 个断言覆盖全链路: 鉴权 → Server → Channel → Message → Reaction → Member → Role → Invite）
bash scripts/smoke-test.sh
```

## 架构

```
客户端 → Gateway(:8080) → Nacos 服务发现
  ├── /api/v1/auth/**, /api/v1/users/**        → community-user-service    (:8081)
  ├── /api/v1/servers/**, /api/v1/channels/*/permissions/**
  │                                              → community-server-service  (:8082)
  ├── /api/v1/channels/*/messages/**, /api/v1/servers/*/search/**
  │   /api/v1/messages/*/reactions/**, /api/v1/channels/*/threads/**
  │                                              → community-message-service (:8083)
  └── /api/v1/upload/**                         → community-file-service    (:8084)

WebSocket: community-websocket (Netty :8091)，独立部署，不经过 Gateway

消息推送链路:
  message-service → MQProducer.sendSecureMsg (@SecureInvoke at-least-once)
  → RocketMQ → websocket PushConsumer → PushService → WebSocket 频道广播

Nacos 共享配置: community-platform-common.yaml（数据源、Redis、RocketMQ、MyBatis-Plus）
GatewayFilter: TokenFilter（JWT 校验）→ Gateway Route → Feign → 微服务
```

基础设施 (Docker Compose):
MySQL:3308 · Redis:6381 · RocketMQ:9878/10911 · MinIO:9004 · Nacos:8848 · ES:9200 · WebSocket:8091
13 个容器总内存上限 6.5 GB → Docker Desktop 需 **8 GB+**（Settings → Resources → Memory）
各容器 mem_limit + JVM 参数详见 `README.md` → "容器内存配置一览"

## 模块导航

| Maven 模块 `community-` | 端口 | 职责 |
|-------------------------|------|------|
| `gateway` | 8080 | Spring Cloud Gateway 路由 + TokenFilter + Nacos 发现 |
| `user-service` | 8081 | 鉴权 (login/refresh) + 用户 CRUD + 微信 OAuth |
| `server-service` | 8082 | Server/Category/Channel/Member/Role/Emoji/Invite CRUD + RBAC 权限 |
| `message-service` | 8083 | 消息策略链 (5 handlers) + Thread + Reaction + Search + 已读 |
| `file-service` | 8084 | MinIO 预签名上传 + 文件关联 |
| `websocket` | :8091 | Netty WS 服务端 + 频道订阅制推送 + PushConsumer |
| `common` | — | 共享库：utils / config / transaction (@SecureInvoke) / annotation / aspect |

旧单体 `community-server/` 已删除。

## 设计模式

- **策略模式**: MsgHandlerFactory → AbstractMsgHandler (Text/Image/File/Sound/System/Emoji)
- **事件驱动**: Spring Event → @SecureInvoke → RocketMQ → PushConsumer → WS
- **游标分页**: CursorPageBaseResp (cursor + isLast + list)
- **RBAC**: 13 位权限位图 + 频道级覆盖 (allow/deny)
- **微服务**: Gateway 路由 + Nacos 注册发现 + OpenFeign 远程调用

## 与 MallChat 的关键差异

| 维度 | MallChat | 社区平台 |
|------|----------|---------|
| 架构 | 单体 | 微服务 (Gateway + 5 services + WS) |
| 导航模型 | 扁平群聊 (roomId) | 嵌套社群 (Server > Category > Channel > Thread) |
| 成员管理 | 群成员 + 简单角色 | RBAC 13 位权限 + 频道覆盖 |
| 登录方式 | 微信 OAuth | 用户名+密码+JWT |
| 消息类型 | 8 种 | 6 种 (Text/Image/File/Sound/System/Emoji) |
| WS 推送 | 房间全推 | 频道订阅制 (Redis Set) |
| 好友系统 | 有 | 无 |

## Nacos 踩坑记录

以下问题已在 docker-compose.yml 中修复，迁移或重构时注意这些点：

| 坑 | 症状 | 修复 |
|----|------|------|
| **健康检查大小写** | Nacos healthcheck 一直 unhealthy | `grep -q ok` → `grep -qi ok`（Nacos v2.3.2 返回 `OK` 大写） |
| **Compose 变量插值吃掉 `$`** | nacos-init 中 `$i`/`$HTTP_CODE`/`$(curl)` 全变空字符串 | 用 `$$` 转义：`$$i`、`$$HTTP_CODE`、`$$(curl)` |
| **应用连 Nacos 用 `localhost`** | 容器内 `localhost:8848` 不可达 → DataSource URL 解析失败 | 在 `x-common-env` 设置 `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=nacos:8848`（env var 优先级 > application.yml 的 `localhost` 默认值） |
| **NACOS_SERVER_IP 必须 IP** | 设为 `nacos` 导致 Nacos 启动崩溃 | `NACOS_SERVER_IP` 校验必须是 IP 格式，不能用 hostname。Docker 环境不设此变量，客户端用 env var 覆盖 server-addr |

## Swagger UI

Gateway 是 WebFlux/Netty 无 springdoc，需直接访问各微服务端口：

| 服务 | Swagger UI |
|------|-----------|
| User Service (8081) | http://localhost:8081/swagger-ui/index.html |
| Server Service (8082) | http://localhost:8082/swagger-ui/index.html |
| Message Service (8083) | http://localhost:8083/swagger-ui/index.html |
| File Service (8084) | http://localhost:8084/swagger-ui/index.html |

右上角 **Authorize** 填入 `Bearer <token>` 即可在线调试所有需要鉴权的端点。

## 文档

- 前端接入指南: `docs/integration-guide.md`（鉴权/错误码/文件上传/WebSocket 协议）
- 项目计划: `docs/plan.md`
- 开发日志: `docs/dev-log.md` / `docs/dev-log-2.md` / `docs/dev-log-3.md`
- 数据库设计: `docs/ddl.sql`
- Nacos 共享配置: `docs/nacos-shared-config.yaml`

## 参考仓库

MallChat: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
