# Community Platform (社群平台)

Discord-like 社群平台后端。Spring Boot 3.3.5 + Java 21 + Netty WebSocket + MyBatis-Plus + Redis + RocketMQ。

## 常用命令

```bash
./mvnw clean compile -pl community-server    # 编译主模块
./mvnw clean package -DskipTests              # 全量打包
docker compose up -d                          # 启动基础设施
```

## 架构

```
请求链路:
Filter(TraceId) → TokenInterceptor → Controller → Service → Dao(MyBatis-Plus)

WebSocket:
NettyWebSocketServer(:8091) → HttpHeadersHandler(Token提取)
  → WebSocketServerProtocolHandler(握手升级)
  → NettyWebSocketServerHandler(业务分发)

消息推送链路:
POST /api/v1/channels/{id}/messages → MessageService.sendMsg()
  → 权限检查(PermissionService) → 持久化(MySQL)
  → MessageSendEvent → RocketMQ MsgSendConsumer
  → PushService → pushToChannel/Thread → WebSocket推送
  → @SecureInvoke 保障 MQ 发送 at-least-once (transaction/ service 包)
```

基础设施 (Docker Compose 独立端口):
MySQL:3308 · Redis:6381 · RocketMQ:9878/10911 · MinIO:9004 · Nacos:8848

## 模块导航

| 包 `com.community.` | 职责 |
|---------------------|------|
| `common/` | 基础设施：配置、拦截器、异常、JWT、缓存、频控注解、WS协议类型、事务 (@SecureInvoke 本地消息表) |
| `user/` | 用户模块：注册、登录、JWT、个人资料 |
| `server/` | 社群模块：Server/Category/Channel CRUD、成员、角色、权限、表情 |
| `message/` | 消息模块：消息CRUD、Thread、Reaction、搜索 |
| `file/` | 文件模块：MinIO预签名上传、文件关联 |
| `websocket/` | Netty WebSocket：连接管理、频道订阅、消息推送 |

## 设计模式（沿用MallChat）

- **策略模式**: MsgHandlerFactory → AbstractMsgHandler (Text/Image/File/System/Sound)
- **事件驱动**: Spring Event (MessageSendEvent → RocketMQ → PushConsumer → WS)
- **游标分页**: CursorPageBaseResp (cursor + isLast + list)
- **RBAC**: 14位权限位图 + 频道级覆盖 (allow/deny)

## 与 MallChat 的关键差异

| 维度 | MallChat | 社区平台 |
|------|----------|---------|
| 导航模型 | 扁平群聊 (roomId) | 嵌套社群 (Server > Category > Channel > Thread) |
| 成员管理 | 群成员 + 简单角色 | RBAC 14位权限 + 频道覆盖 |
| 登录方式 | 微信 OAuth | 用户名+密码+JWT |
| 消息类型 | 8种 | 5种 (Text/Image/File/Sound/System) |
| WS推送 | 房间全推 | 频道订阅制 (Redis Set) |
| 好友系统 | 有 | 无 |

## 文档

- 项目计划: `docs/plan.md`
- 开发日志: `docs/dev-log.md` / `docs/dev-log-2.md`
- 数据库设计: `docs/ddl.sql`

## 参考仓库

MallChat: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
