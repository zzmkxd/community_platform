# Community Platform（社群平台）

> Discord-like 社群平台后端，供软件实训课程使用。

基于 **Spring Cloud** 微服务架构，支持 **Server → Category → Channel → Thread** 嵌套社群结构 + **RBAC 14 位权限系统**。

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot + Spring Cloud | 3.3.5 / 2023.0.3 |
| 注册中心 | Nacos (Spring Cloud Alibaba) | 2023.0.1.0 |
| 数据库 | MySQL + MyBatis-Plus | 8.0 / 3.5.10.1 |
| 缓存 | Redis + Redisson | 7.x / 3.36.0 |
| 消息队列 | RocketMQ | 5.1.4 |
| 搜索引擎 | Elasticsearch | 8.11.x |
| 对象存储 | MinIO | latest |
| 实时通信 | Netty WebSocket | 4.1.114.Final |
| API 文档 | Springdoc + Knife4j | 2.6.0 / 4.4.0 |
| 容器化 | Docker Compose | v2 |

---

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

启动 MySQL + Redis + RocketMQ + MinIO + Elasticsearch + Nacos。

### 2. 初始化数据库

```sql
CREATE DATABASE community_platform DEFAULT CHARACTER SET utf8mb4;
```

建表脚本见 `docs/ddl.sql`（待创建）。

### 3. 配置本地环境

复制并编辑 `application-local.properties`：

```properties
community.mysql.password=your_password
community.jwt.secret=your_jwt_secret
```

### 4. 启动应用

```bash
./mvnw clean compile -pl community-server
./mvnw spring-boot:run -pl community-server
```

服务启动后访问：
- **API 文档 (Knife4j)**: http://localhost:8080/doc.html
- **WebSocket**: ws://localhost:8090/ws

---

## 项目结构

```
community-platform/
├── pom.xml                          # 根 POM，依赖管理
├── community-server/                # 单体服务（Phase 1）
│   └── src/main/java/com/community/
│       ├── CommunityApplication.java
│       ├── common/                  # 基础设施
│       │   ├── config/              # 线程池、MyBatis-Plus、拦截器
│       │   ├── interceptor/         # TokenInterceptor（JWT 验证）
│       │   ├── exception/           # 全局异常处理 + 错误码
│       │   ├── annotation/          # @FrequencyControl、@RedissonLock
│       │   ├── cache/               # Redis 批量缓存框架
│       │   ├── utils/               # JWT、游标分页、RequestHolder
│       │   ├── websocket/           # WS 协议类型定义
│       │   └── domain/vo/           # ApiResult、CursorPageBaseResp
│       ├── user/                    # 用户模块（注册/登录/JWT）
│       ├── server/                  # 社群模块（Server 核心 + RBAC）
│       ├── message/                 # 消息模块（消息/Thread/Reaction/搜索）
│       ├── file/                    # 文件模块（MinIO 预签名上传）
│       └── websocket/               # Netty WS 服务端（端口 8090）
└── docs/                            # 文档
```

**Phase 2** 计划拆分为：gateway-server + user-service + server-service + message-service + file-service + websocket-server。

---

## 核心设计

### 社群嵌套模型

```
Server (服务器)
├── Category A (分类："学习区")
│   ├── Channel #闲聊 (TEXT, 公开)
│   ├── Channel #问答 (TEXT, Thread-only)
│   └── Channel #资源 (TEXT, 可发文件)
├── Category B (分类："管理区")
│   ├── Channel #公告 (TEXT, 仅管理员)
│   └── Channel #管理讨论 (TEXT, 仅特定角色)
└── (无分类的频道也可直接挂在 Server 下)
```

### RBAC 权限位图（14 位）

| 权限位 | 值 | 说明 |
|--------|-----|------|
| CREATE_INVITE | 0x0001 | 创建邀请 |
| KICK_MEMBERS | 0x0002 | 踢出成员 |
| BAN_MEMBERS | 0x0004 | 封禁成员 |
| ADMINISTRATOR | 0x0008 | 管理员（全部权限，短路判断） |
| MANAGE_CHANNELS | 0x0010 | 管理频道 |
| MANAGE_SERVER | 0x0020 | 管理服务器 |
| ADD_REACTIONS | 0x0040 | 添加反应 |
| SEND_MESSAGES | 0x0080 | 发送消息 |
| USE_THREADS | 0x0100 | 使用话题 |
| EMBED_LINKS | 0x0200 | 嵌入链接 |
| ATTACH_FILES | 0x0400 | 上传文件 |
| MENTION_EVERYONE | 0x0800 | @全体成员 |
| MANAGE_ROLES | 0x1000 | 管理角色 |
| MANAGE_EMOJI | 0x2000 | 管理表情 |

权限计算：**effectivePermissions = (所有角色 permissions 的 OR) ⊕ 频道级覆盖 (allow/deny)**

含 ADMINISTRATOR 位则直接放行，无需逐位检查。

### 消息发送全链路

```
POST /api/v1/channels/{id}/messages
  → Token 校验 (JWT)
  → 权限检查 (Feign → server-service)
  → 频控 (@FrequencyControl)
  → 敏感词过滤 (AC 自动机)
  → MySQL 持久化
  → MessageSendEvent → RocketMQ SendMsgConsumer
  → PushConsumer → WS 帧 → 仅推送给订阅了该频道的在线用户
```

**与群聊的区别**：不再是"房间内全推"，而是**按频道订阅推送**——客户端通过 `SUBSCRIBE_CHANNEL` WS 消息声明订阅哪些频道，只收到相关频道的实时消息。

### WebSocket 协议

```
C → S 请求 (9 种):
  LOGIN(1), HEARTBEAT(2), SEND_MESSAGE(4),
  SUBSCRIBE_CHANNEL(5), UNSUBSCRIBE_CHANNEL(6),
  SUBSCRIBE_THREAD(7), UNSUBSCRIBE_THREAD(8),
  TYPING_START(9), TYPING_STOP(10)

S → C 推送 (18 种):
  LOGIN_SUCCESS(3), MESSAGE_CREATE(20), MESSAGE_UPDATE(21),
  MESSAGE_DELETE(22), REACTION_ADD(23), REACTION_REMOVE(24),
  TYPING_START(25), TYPING_STOP(26), THREAD_CREATE(34),
  MEMBER_JOIN(30), MEMBER_LEAVE(31), USER_ONLINE(40),
  USER_OFFLINE(41), CHANNEL_CREATE(50), CHANNEL_UPDATE(51),
  CHANNEL_DELETE(52), SERVER_UPDATE(53), ERROR(99)
```

### 游标分页

所有列表接口统一使用游标分页：

```json
// 请求
GET /api/v1/channels/1/messages?cursor=&pageSize=50

// 响应
{
  "cursor": "1700000000000",
  "isLast": false,
  "list": [ ... ]
}
```

---

## API 概览

> 完整接口文档启动后端后访问 http://localhost:8080/doc.html

| 模块 | 端点前缀 | 说明 |
|------|---------|------|
| Auth | `POST /api/v1/auth/*` | 注册、登录、Token 刷新 |
| User | `GET/PUT /api/v1/users/*` | 个人资料 |
| Server | `CRUD /api/v1/servers/*` | 服务器管理 |
| Category | `CRUD /api/v1/servers/{id}/categories/*` | 分类管理 |
| Channel | `CRUD /api/v1/servers/{id}/channels/*` | 频道管理 |
| Member | `CRUD /api/v1/servers/{id}/members/*` | 成员管理 |
| Role | `CRUD /api/v1/servers/{id}/roles/*` | 角色管理 |
| Emoji | `CRUD /api/v1/servers/{id}/emojis/*` | 表情管理 |
| Message | `CRUD /api/v1/channels/{id}/messages/*` | 消息收发 |
| Thread | `CRUD /api/v1/channels/{id}/threads/*` | 话题管理 |
| Reaction | `POST/DELETE/GET /api/v1/messages/{id}/reactions` | 反应 |
| Search | `GET /api/v1/servers/{id}/search` | 消息搜索 |
| File | `POST /api/v1/upload/*` | MinIO 预签名上传 |

---

## 与 MallChat 的关系

本项目借鉴了 [MallChat](https://github.com/zongzibinbin/MallChat) 的设计模式和基础设施代码，但业务逻辑完全重写：

| 复用 | 重写 |
|------|------|
| Netty WS 管道 + JWT 认证 | 社群嵌套模型 (Server/Category/Channel/Thread) |
| 游标分页 (CursorUtils) | RBAC 14 位权限系统 |
| 消息策略模式 (MsgHandlerFactory) | 频道订阅推送（替代房间全推） |
| Redis 批量缓存框架 | MinIO 预签名上传 |
| @FrequencyControl 频控 | 用户注册/登录（替代微信 OAuth） |
| 全局异常处理 + 统一响应体 | 5 种消息类型（替代 8 种） |

---

## 下一步

- [ ] 实现用户注册/登录（AuthService）
- [ ] 编写 DDL 建表脚本（13 张表）
- [ ] 实现 Server/Channel CRUD
- [ ] 实现 RBAC 权限检查（PermissionService）
- [ ] 实现消息发送全链路（含 RocketMQ → WS 推送）
- [ ] 实现 Thread + Reaction
- [ ] 实现 MinIO 预签名上传
- [ ] 实现 ES 消息搜索
- [ ] Phase 2: 微服务拆分（Nacos + Gateway + Feign）

---

## License

MIT
