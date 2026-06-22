# Community Platform（社群平台）

> Discord-like 社群平台后端，供软件实训课程使用。

基于 Spring Boot 3.3.5 单体架构，支持 **Server → Category → Channel → Thread** 嵌套社群结构 + **RBAC 13 位权限系统**。

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.3.5 |
| 数据库 | MySQL + MyBatis-Plus | 8.0 / 3.5.10.1 |
| 缓存 | Redis + Redisson | 7.x / 3.36.0 |
| 消息队列 | RocketMQ | 5.1.4 |
| 对象存储 | MinIO | latest |
| 实时通信 | Netty WebSocket | 4.1.114.Final |
| API 文档 | Springdoc OpenAPI (Swagger UI) | 2.6.0 |
| 容器化 | Docker Compose | v2 |

---

## 快速开始

### 方式 1: Docker Compose（推荐，一键启动）

```bash
# 1. 打包
export JAVA_HOME="/path/to/jdk-21"
mvn package -pl community-server -am -DskipTests

# 2. 构建镜像
docker build -t community-platform:latest .

# 3. 启动全栈（MySQL + Redis + MinIO + RocketMQ + App）
docker compose up -d
```

首次启动时 Docker Desktop 会弹出文件共享确认窗口，点击 **"Share it"** 允许挂载 `docs/ddl.sql`（数据库自动建表 + 种子数据）。

启动后访问：

| 服务 | 地址 |
|------|------|
| **API 文档 (Swagger UI)** | http://localhost:8081/swagger-ui/index.html |
| **WebSocket** | ws://localhost:8092 |
| **MinIO Console** | http://localhost:9005 (minioadmin / minioadmin) |

停止并清理数据：

```bash
docker compose down -v
```

### 方式 2: 本地开发（依赖 MallChat Docker 基础设施）

复用 MallChat 的 Docker 容器，无需额外启动数据库：

```bash
# 1. 确保 MallChat 基础设施已运行
cd MallChat && docker compose up -d

# 2. 初始化数据库（仅首次）
mysql -u root -p123456 -h 127.0.0.1 -P 3307 < docs/ddl.sql

# 3. 启动应用
cd community-platform
export JAVA_HOME="/path/to/jdk-21"
mvn spring-boot:run -pl community-server -Dspring-boot.run.profiles=local
```

启动后访问：

| 服务 | 地址 |
|------|------|
| **API 文档 (Swagger UI)** | http://localhost:8080/swagger-ui/index.html |
| **WebSocket** | ws://localhost:8091 |

### 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `alice` | `123456` | 种子用户 1 |
| `bob` | `123456` | 种子用户 2 |
| `charlie` | `123456` | 种子用户 3 |

> 设计决策：username/password 仅用于 seed data 开发通道，无注册端点。正式用户通过微信 OAuth 扫码登录。

### API 调试流程

```bash
# 1. 登录获取 token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'

# 2. 创建服务器
curl -X POST http://localhost:8080/api/v1/servers \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"学习区","description":"技术讨论","icon":"📚"}'

# 3. 创建频道
curl -X POST http://localhost:8080/api/v1/servers/1/channels \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"general","type":0}'

# 4. 发送消息
curl -X POST http://localhost:8080/api/v1/channels/1/messages \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello World!","msgType":1}'
```

---

## 功能清单

### 已实现 (Phase 1-4)

| 模块 | 功能 | 说明 |
|------|------|------|
| **Auth** | 登录 / Token 刷新 / JWT 验证 | 种子用户 BCrypt 校验，5 天 Token + Redis 单设备登录 |
| **Server** | 创建/列表/详情/更新/删除 | 创建时自动生成 @everyone + Owner 角色，owner/admin 可管理 |
| **Category** | 创建/更新/删除 | 删除时频道移入未分类 |
| **Channel** | 创建/列表/详情/更新/删除 | 嵌套在 Category 下返回，TEXT(0)/VOICE(1) 两种类型 |
| **Member** | 加入/列表/昵称/踢出/离开/转让 | KICK_MEMBERS 权限控制，Owner 不可自行离开 |
| **Role** | 创建/更新/删除/分配 | Owner-only 操作，assignRoles 原子批量替换 |
| **RBAC 权限** | 13 位位掩码权限系统 | OR 角色权限 → ADMINISTRATOR 短路 → ChannelPermission 覆盖 |
| **ChannelPermission** | upsert/列表/删除 | user > role，deny > allow |
| **Invite** | 创建/加入 | UUID 短码，支持最大使用次数 + 过期时间 |
| **Emoji** | 上传/列表/删除 | MinIO 预签名上传 |
| **Message** | 发送/列表/详情/编辑/删除 | 5 种消息类型策略链 (Text/Image/File/Sound/System) |
| **Thread** | 创建/列表/详情/更新 | 按 last_active DESC 排序，Thread 内消息独立于频道时间线 |
| **Reaction** | 添加/移除/列表 | 同 emoji 切换（再点移除），按 emoji 聚合 + reacted 标记 |
| **已读追踪** | 更新已读/未读计数 | ChannelReadState upsert，按 Server 聚合各频道未读数 |
| **文件上传** | MinIO 预签名 URL | PENDING → UPLOADED 状态流转 |
| **搜索** | 消息全文搜索 | MySQL LIKE + LIMIT 50 |
| **WebSocket** | Netty 8091 端口 | JWT 认证 + 基础 WS 握手 |
| **API 文档** | Swagger UI | 所有端点可在线调试 |
| **Docker** | Dockerfile + docker-compose | 一键启动全栈，独立端口不冲突 |

### 尚未实现 (Phase 4.x+)

| 项目 | 说明 | 计划 |
|------|------|------|
| **事件推送管道** | MsgSendConsumer → PushService → RocketMQ → WebSocket 实时推送 | Phase 4.x |
| **消息实时推送** | 发送消息后通过 WebSocket 推送给频道内在线用户 | Phase 4.x |
| **Thread 自动归档** | @Scheduled 每小时归档 24h 无活动 Thread | Phase 4.x |
| **频控** | @FrequencyControl 注解 + AOP | Phase 5 |
| **敏感词过滤** | AC 自动机算法（移植 MallChat common/sensitive） | Phase 5 |
| **Mention 解析** | @用户解析 + 通知 | Phase 5 |
| **WebSocket 完整协议** | 订阅/取消订阅频道/Thread、Typing 状态、系统通知 | Phase 5 |
| **ES 全文搜索** | Elasticsearch 替代 MySQL LIKE | Phase 6 |
| **微服务拆分** | Nacos + Gateway + Feign 拆为 5 个微服务 | Phase 6 |
| **在线状态** | 用户在线/离线 + 状态广播 | Phase 6 |
| **语音频道** | WebRTC 信令服务 | Phase 7 |
| **加入审批** | Server 加入申请 + 审批流程 | Phase 7 |

---

## 项目结构

```
community-platform/
├── pom.xml                              # 根 POM，依赖管理
├── Dockerfile                           # Docker 镜像
├── docker-compose.yml                   # 全栈编排
├── broker.conf                          # RocketMQ Broker 配置
├── community-server/                    # 单体服务
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml          # 主配置，profile=local
│       │   ├── application-local.properties  # 本地开发（gitignored）
│       │   └── application-docker.yml   # Docker 环境配置
│       └── java/com/community/
│           ├── CommunityApplication.java
│           ├── common/                  # 基础设施
│           │   ├── config/              # OpenApi/Swagger 配置
│           │   ├── constant/            # MQConstant / RedisKey
│           │   ├── exception/           # 全局异常处理 + 错误码枚举
│           │   ├── utils/               # JWT / CursorUtils / RequestHolder
│           │   └── domain/              # ApiResult / CursorPageBaseResp / DTO
│           ├── user/                    # 用户模块
│           │   ├── consumer/            # MQ 消费者 (MsgLogin/ScanSuccess)
│           │   ├── controller/          # Auth / User / WxPortal
│           │   ├── dao/                 # UserDao + Mapper
│           │   ├── domain/              # User Entity / VO / DTO
│           │   └── service/             # AuthService / UserService / WxMsgService
│           │       ├── adapter/         # TextBuilder / UserAdapter
│           │       └── handler/         # 微信消息 Handler (Scan/Subscribe/Log)
│           ├── server/                  # 社群模块
│           │   ├── controller/          # Server / Channel / Category / Member / Role / Emoji / Invite
│           │   ├── dao/                 # 7 个 DAO + Mapper
│           │   ├── domain/              # Entity (7) + VO (10) + enums (PermissionBit)
│           │   └── service/             # 7 个 Service + PermissionService (RBAC)
│           ├── message/                 # 消息模块
│           │   ├── controller/          # Message / Thread / Reaction / Search
│           │   ├── dao/                 # MessageDao / ThreadDao / ReactionDao / ChannelReadStateDao
│           │   ├── domain/              # Entity (4) + VO (5) + DTO (SendMsgReq)
│           │   ├── event/               # ChannelMessageSendEvent + MessageSendListener
│           │   └── service/             # Message / Thread / Reaction / Search / ChannelReadState
│           │       ├── adapter/         # MessageAdapter
│           │       └── strategy/msg/    # 消息策略链 (Abstract + Factory + 5 handlers)
│           ├── file/                    # 文件模块
│           │   └── controller/          # FileController (MinIO 预签名)
│           └── websocket/               # Netty WebSocket (端口 8091)
│               ├── NettyWebSocketServer.java
│               └── consumer/            # PushConsumer
└── docs/
    ├── ddl.sql                          # 15 张表 DDL + 种子数据
    ├── README.md                        # 文档索引
    └── plan/                            # 开发方案文档
```

---

## 核心设计

### 社群嵌套模型

```
Server (服务器)
├── Category A (分类："学习区")
│   ├── Channel #闲聊 (TEXT)
│   ├── Channel #问答 (TEXT)
│   └── Channel #资源 (TEXT)
├── Category B (分类："管理区")
│   ├── Channel #公告 (TEXT)
│   └── Channel #管理讨论 (TEXT)
└── (无分类的频道可直接挂在 Server 下)
```

### RBAC 权限位图（13 位）

| 权限位 | 位值 | 说明 |
|--------|------|------|
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

权限计算：**effectivePermissions = (所有角色 permissions 的 OR) → ChannelPermission 覆盖 (user > role, deny > allow)**

含 ADMINISTRATOR 位则直接放行，无需逐位检查。

### 创建 Server 默认角色

| 角色 | 权限 | 说明 |
|------|------|------|
| @everyone | CREATE_INVITE + SEND_MESSAGES + ADD_REACTIONS + USE_THREADS + EMBED_LINKS + ATTACH_FILES | 所有成员自动获得 |
| Owner | ADMINISTRATOR | 仅创建者，position=999，金色 #FFD700 |

### 消息发送链路（当前 v1）

```
POST /api/v1/channels/{id}/messages
  → Token 校验 (TokenInterceptor)
  → 权限检查 (SEND_MESSAGES)
  → 消息策略链 (MsgHandlerFactory → AbstractMsgHandler.checkAndSaveMsg)
  → MySQL 持久化
  → ChannelMessageSendEvent → MessageSendListener (@Async log)
  → (Phase 4.x: RocketMQ → MsgSendConsumer → PushService → WebSocket push)
```

> 留待：频控、敏感词过滤、实时 WS 推送

### 游标分页

所有列表接口统一使用游标分页：

```json
GET /api/v1/channels/1/messages?cursor=&pageSize=50

{
  "cursor": "1",
  "isLast": true,
  "list": [...]
}
```

---

## API 概览

> 启动后访问 Swagger UI 可在线调试所有接口。

| 模块 | 端点 | 说明 |
|------|------|------|
| Auth | `POST /api/v1/auth/login` | 登录（种子用户密码） |
| Auth | `POST /api/v1/auth/refresh` | 刷新 Token |
| User | `GET /api/v1/users/me` | 当前用户信息 |
| User | `PUT /api/v1/users/me` | 更新个人信息 |
| User | `POST /api/v1/users/me/bind-wx` | 绑定微信 openId |
| User | `GET /api/v1/users/{id}` | 查看用户 |
| Server | `POST /api/v1/servers` | 创建服务器 |
| Server | `GET /api/v1/servers` | 我的服务器列表 |
| Server | `GET /api/v1/servers/discover` | 可发现的服务器 |
| Server | `GET /api/v1/servers/{id}` | 服务器详情（含角色） |
| Server | `GET /api/v1/servers/{id}/unread` | 各频道未读计数 |
| Server | `PUT /api/v1/servers/{id}` | 更新服务器 |
| Server | `DELETE /api/v1/servers/{id}` | 删除服务器 |
| Channel | `POST /api/v1/servers/{id}/channels` | 创建频道 |
| Channel | `GET /api/v1/servers/{id}/channels` | 频道列表（嵌套分类） |
| Channel | `GET /api/v1/channels/{id}` | 频道详情 |
| Channel | `PUT /api/v1/channels/{id}` | 更新频道 |
| Channel | `DELETE /api/v1/channels/{id}` | 删除频道 |
| Category | `POST /api/v1/servers/{id}/categories` | 创建分类 |
| Category | `PUT /api/v1/servers/{id}/categories/{id}` | 更新分类 |
| Category | `DELETE /api/v1/servers/{id}/categories/{id}` | 删除分类 |
| Member | `GET /api/v1/servers/{id}/members` | 成员列表 |
| Member | `POST /api/v1/servers/{id}/members/join` | 加入服务器 |
| Member | `PUT /api/v1/servers/{id}/members/{uid}/nickname` | 更新昵称 |
| Member | `DELETE /api/v1/servers/{id}/members/{uid}` | 踢出/离开 |
| Member | `PUT /api/v1/servers/{id}/members/transfer-ownership` | 转让 Owner |
| Role | `POST /api/v1/servers/{id}/roles` | 创建角色 |
| Role | `GET /api/v1/servers/{id}/roles` | 角色列表 |
| Role | `PUT /api/v1/servers/{id}/roles/{id}` | 更新角色 |
| Role | `DELETE /api/v1/servers/{id}/roles/{id}` | 删除角色 |
| Role | `PUT /api/v1/servers/{id}/members/{uid}/roles` | 分配角色 |
| ChannelPermission | `POST /api/v1/channels/{id}/permissions` | 设置权限覆盖 |
| ChannelPermission | `GET /api/v1/channels/{id}/permissions` | 权限覆盖列表 |
| ChannelPermission | `DELETE /api/v1/channels/{id}/permissions/{id}` | 删除权限覆盖 |
| Emoji | `POST /api/v1/servers/{id}/emojis` | 上传表情 |
| Emoji | `GET /api/v1/servers/{id}/emojis` | 表情列表 |
| Emoji | `DELETE /api/v1/servers/{id}/emojis/{id}` | 删除表情 |
| Invite | `POST /api/v1/servers/{id}/invites` | 创建邀请 |
| Invite | `POST /api/v1/invites/{code}/join` | 通过邀请加入 |
| Message | `POST /api/v1/channels/{id}/messages` | 发送消息 |
| Message | `GET /api/v1/channels/{id}/messages` | 消息列表（游标分页） |
| Message | `GET /api/v1/channels/{id}/messages/{msgId}` | 消息详情 |
| Message | `PUT /api/v1/channels/{id}/messages/{msgId}` | 编辑消息 |
| Message | `DELETE /api/v1/channels/{id}/messages/{msgId}` | 删除消息 |
| Thread | `POST /api/v1/channels/{id}/threads` | 创建话题 |
| Thread | `GET /api/v1/channels/{id}/threads` | 话题列表 |
| Thread | `GET /api/v1/threads/{id}` | 话题详情 |
| Thread | `GET /api/v1/threads/{id}/messages` | 话题内消息 |
| Thread | `PUT /api/v1/threads/{id}` | 更新话题 |
| Reaction | `POST /api/v1/messages/{id}/reactions?emoji=` | 添加反应 |
| Reaction | `GET /api/v1/messages/{id}/reactions` | 反应列表 |
| Reaction | `DELETE /api/v1/messages/{id}/reactions?emoji=` | 移除反应 |
| Search | `GET /api/v1/servers/{id}/search?q=` | 消息搜索 |
| File | `POST /api/v1/upload/presign` | MinIO 预签名上传 URL |
| WxPortal | `GET /wx/portal/public` | 微信服务器验证 |
| WxPortal | `GET /wx/portal/public/callBack` | 微信 OAuth 回调 |
| WxPortal | `POST /wx/portal/public` | 微信事件推送 |

---

## 与 MallChat 的关系

本项目借鉴了 [MallChat](https://github.com/zongzibinbin/MallChat) 的设计模式和基础设施代码，但业务逻辑完全重写：

| 复用自 MallChat | 本项目重写 |
|----------------|-----------|
| Netty WS 管道 + JWT 认证 | 社群嵌套模型 (Server/Category/Channel/Thread) |
| 游标分页 (CursorUtils) | RBAC 13 位权限系统 + ChannelPermission 覆盖 |
| 消息策略模式 (MsgHandlerFactory + AbstractMsgHandler) | 频道订阅推送（替代房间全推） |
| Redis 批量缓存框架 | 用户名密码登录（替代微信 OAuth 为主） |
| @FrequencyControl 频控注解 | MinIO 预签名上传 |
| 全局异常处理 + 统一响应体 | 5 种消息类型（Text/Image/File/Sound/System） |
| JWT (java-jwt) + RequestHolder (ThreadLocal) | MyBatis-Plus ServiceImpl 模式替代定制 DAO |
| Spring Event → 事务外异步 | 种子数据开发通道（无注册端点） |

---

## License

MIT
