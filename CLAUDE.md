# Community Platform (社群平台)

Discord-like 社群平台后端。Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Nacos + Gateway + Feign + Netty WebSocket + MyBatis-Plus + Redis + RocketMQ。

## 常用命令

```bash
# 编译全量模块
mvn clean compile -B

# 启动基础设施（独立端口，与 MallChat 不冲突）
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
MySQL:3308 · Redis:6381 · RocketMQ:9878/10911 · MinIO:9004 · Nacos:8848

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

## 文档

- 项目计划: `docs/plan.md`
- 开发日志: `docs/dev-log.md` / `docs/dev-log-2.md`
- 数据库设计: `docs/ddl.sql`
- Nacos 共享配置: `docs/nacos-shared-config.yaml`

## 参考仓库

MallChat: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
