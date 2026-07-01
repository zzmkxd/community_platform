# Community Platform（社群平台）

[![Build](https://github.com/zzmkxd/community_platform/actions/workflows/build.yml/badge.svg)](https://github.com/zzmkxd/community_platform/actions/workflows/build.yml)

> Discord-like 社群平台后端，供软件实训课程使用。

基于 Spring Boot 3.3.5 微服务架构（Spring Cloud Gateway + Nacos + OpenFeign），支持 **Server → Category → Channel → Thread** 嵌套社群结构 + **RBAC 13 位权限系统**。

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot + Spring Cloud + OpenFeign | 3.3.5 / 2023.0.3 |
| 网关 | Spring Cloud Gateway | 4.1.x |
| 服务发现 | Nacos | 2.3.2 |
| 数据库 | MySQL + MyBatis-Plus | 8.0 / 3.5.10.1 |
| 缓存 | Redis + Redisson | 7.x / 3.36.0 |
| 消息队列 | RocketMQ | 5.1.4 |
| 对象存储 | MinIO | latest |
| 实时通信 | Netty WebSocket | 4.1.114.Final |
| API 文档 | Springdoc OpenAPI (Swagger UI) | 2.6.0 |
| 容器化 | Docker Compose | v2 |

---

## 前置条件

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| **Docker Desktop** | 4.x | Docker Compose 一键启动（推荐），需 **8GB+ 内存** |
| **Git** | 2.x | 克隆仓库 |
| **Java JDK** | 21 | 仅本地 IDE 开发需要 |
| **Maven** | 3.9+ | 仅本地 IDE 开发需要（或使用 `./mvnw` wrapper） |

> Docker Compose 启动 13 个容器（MySQL + Redis + Nacos + MinIO + RocketMQ ×2 + Elasticsearch + Gateway + 5 微服务 + WebSocket），总内存上限 6.5 GB。Docker Desktop 需分配 **8 GB+**。

---

## 快速开始

```bash
# 1. 克隆仓库
git clone https://github.com/zzmkxd/community_platform.git
cd community_platform
```

### 方式 1: Docker Compose 一键启动（推荐）

```bash
# Windows
scripts\start-all.bat

# macOS / Linux
bash scripts/start-all.sh
```

> 脚本自动完成：`mvn package` 全量打包 → Docker Compose 构建镜像并启动。
> 首次启动 Docker 会弹出文件共享确认，点击 **"Share it"** 允许挂载 `docs/ddl.sql`（自动建表）。

**启动顺序**：MySQL → Redis → Nacos → **nacos-init 发布共享配置** → 6 个微服务 + MinIO + RocketMQ。

启动后访问：

| 服务 | 地址 | 端口 |
|------|------|------|
| **Gateway 统一入口** | http://localhost:8080 | 8080 |
| **Nacos 控制台** | http://localhost:8848/nacos | 8848 |
| **WebSocket** | ws://localhost:8091 | 8091 |
| **MinIO Console** | http://localhost:9005 | minioadmin / minioadmin |

### 方式 2: 本地开发（IDE 启动）

适合修改代码 + 断点调试。基础设施用 Docker，微服务在 IDE 中启动。

```bash
# Step 1: 安装父 POM 和共享库
mvn install -N -q                              # 父 POM
mvn install -pl community-common -q            # 共享库

# Step 2: 启动基础设施（不含微服务）
docker compose up -d mysql redis nacos minio rocketmq-namesrv rocketmq-broker

# Step 3: 等待 Nacos 共享配置发布完成
docker compose logs nacos-init | grep "successfully"
```

**Docker → Host 端口映射**（本地 `application-local.properties` 需用 host 地址）：

| 服务 | Docker 内部 | Host 端口 | 本地配置示例 |
|------|------------|-----------|-------------|
| MySQL | mysql:3306 | 127.0.0.1:**3308** | `community.mysql.port=3308` |
| Redis | redis:6379 | 127.0.0.1:**6381** | `community.redis.port=6381` |
| Nacos | nacos:8848 | 127.0.0.1:**8848** | `spring.cloud.nacos.server-addr=127.0.0.1:8848` |
| RocketMQ | namesrv:9876 | 127.0.0.1:**9878** | `rocketmq.name-server=127.0.0.1:9878` |
| MinIO | minio:9000 | 127.0.0.1:**9004** | `community.minio.endpoint=http://127.0.0.1:9004` |
| Elasticsearch | es:9200 | 127.0.0.1:**9200** | `community.es.uris=http://127.0.0.1:9200` |

> **配置方式二选一**：
> - **A. Nacos 集中配置**：在 [Nacos 控制台](http://localhost:8848/nacos) 将 `community-platform-common.yaml` 中 `${community.xxx}` 占位符替换为上表 host 地址（参考 `docs/nacos-shared-config.yaml` 顶部注释）
> - **B. 各服务独立配置**：为每个模块创建 `src/main/resources/application-local.yml`，覆盖 host 地址。Gateway 仅需 JWT secret + Nacos 地址，无需数据库配置

**Step 4: IDE 中按序启动**（右键 `main()` 或 Spring Boot Dashboard，Profile 选 `local`）：

| 顺序 | 模块 | 主类 | 端口 |
|------|------|------|------|
| 1 | `community-user-service` | `CommunityUserApplication` | 8081 |
| 2 | `community-server-service` | `CommunityServerApplication` | 8082 |
| 3 | `community-message-service` | `CommunityMessageApplication` | 8083 |
| 4 | `community-file-service` | `CommunityFileApplication` | 8084 |
| 5 | `community-websocket` | `CommunityWebSocketApplication` | 8091 |
| 6 | `community-gateway` | `CommunityGatewayApplication` | 8080 |

> Gateway 最后启动——它需要下游服务已注册到 Nacos 才能正确路由。

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

### 已实现 (Phase 1-6)

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
| **搜索** | 消息全文搜索 | Elasticsearch 替代 MySQL LIKE |
| **WebSocket** | Netty 8091 端口 | JWT 认证 + 持续连接 |
| **事件推送管道** | @SecureInvoke → RocketMQ → WebSocket | MQ 故障自动重试，at-least-once 保证 |
| **频控** | @FrequencyControl 注解 + AOP | 固定窗口 / 滑动窗口 / 令牌桶 |
| **敏感词过滤** | AC 自动机 + DFAFilter 双模式 | 移植自 MallChat |
| **Mention 解析** | @username / @all / @everyone | 文本解析 + 通知推送 |
| **微服务拆分** | Nacos + Gateway + Feign | 6 服务 + 1 共享库 (community-common) |
| **在线状态** | 在线/离线广播 | Redis Set 订阅制 |
| **Docker** | Dockerfile + docker-compose | 独立端口，含 MySQL+Redis+RocketMQ+MinIO+Nacos |
| **CI/CD** | GitHub Actions | push main/zjwSpringCloud/feature/fix 自动编译，push 自动打包推送 GHCR |
| **API 文档** | Swagger UI | 所有端点可在线调试 |

### 尚未实现 (Phase 7+)

| 项目 | 说明 | 计划 |
|------|------|------|
| **Thread 自动归档** | @Scheduled 每小时归档 24h 无活动 Thread | Phase 7 |
| **语音频道** | WebRTC 信令服务 | Phase 7 |
| **加入审批** | Server 加入申请 + 审批流程 | Phase 7 |
| **测试覆盖** | 单元测试 + 集成测试 | Phase 7.1 |

---

## 项目结构

```
community-platform/
├── Dockerfile                           # Docker 镜像（单体兼容）
├── docker-compose.yml                   # 全栈编排 (MySQL+Redis+RocketMQ+MinIO+Nacos)
├── pom.xml                              # 根 POM，依赖管理 (Spring Cloud BOM)
├── broker.conf                          # RocketMQ Broker 配置
├── community-gateway/                   # 🆕 API 网关 (8080)
│   └── filter/AuthGlobalFilter.java     # JWT 全局鉴权 + Nacos 路由
├── community-user-service/              # 🆕 用户服务 (8081)
│   ├── controller/ (Auth/User/WxPortal)
│   ├── service/ (AuthService/UserService)
│   └── interceptor/TokenInterceptor.java
├── community-server-service/            # 🆕 社群服务 (8082)
│   ├── controller/ (Server/Channel/Category/Member/Role/Emoji/Invite/Permission)
│   └── service/ (8 Service + PermissionService + MembershipValidator)
├── community-message-service/           # 🆕 消息服务 (8083)
│   ├── controller/ (Message/Thread/Reaction/Search)
│   ├── consumer/MsgSendConsumer.java    # RocketMQ → WS 推送
│   └── service/strategy/msg/            # 消息策略链 (6 handlers)
├── community-file-service/              # 🆕 文件服务 (8084)
│   ├── controller/FileController.java   # MinIO 预签名
│   └── config/MinIOConfiguration.java
├── community-websocket/                 # 🆕 WebSocket 服务 (Netty :8091)
│   ├── NettyWebSocketServer.java
│   ├── consumer/PushConsumer.java
│   └── service/ (PushService/WebSocketService)
├── community-common/                    # 🆕 共享库（无主类）
│   ├── utils/ (JWT/JsonUtils/CursorUtils/RedisUtils/RequestHolder)
│   ├── config/ (ThreadPool/MybatisPlus/Redis/Oss/SensitiveWord)
│   ├── transaction/ (@SecureInvoke 本地消息表 — 10 文件)
│   ├── annotation/ (@FrequencyControl/@RedissonLock)
│   ├── aspect/ (FrequencyControlAspect/SecureInvokeAspect)
│   ├── exception/ (GlobalExceptionHandler + 全局错误码)
│   └── domain/ (统一响应体 + 游标分页 + PushMessageDTO)
└── docs/
    ├── ddl.sql                          # 17 张表 DDL + 种子数据
    ├── nacos-shared-config.yaml         # Nacos 共享配置模板
    ├── dev-log.md / dev-log-2.md / dev-log-3.md   # 开发日志
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

### 消息发送链路（当前）

```
POST /api/v1/channels/{id}/messages (→ Gateway :8080)
  → AuthGlobalFilter JWT 校验
  → Route → community-message-service (:8083)
  → 权限检查 (Feign → server-service PermissionService)
  → 消息策略链 (MsgHandlerFactory → AbstractMsgHandler.checkAndSaveMsg)
  → MySQL 持久化
  → MQProducer.sendSecureMsg (@SecureInvoke at-least-once)
  → RocketMQ → community-websocket PushConsumer
  → PushService → WebSocket 频道广播
```

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

## 常见问题

### Docker 相关

| 问题 | 解决 |
|------|------|
| **Docker 内存不足导致崩溃** | Docker Desktop → Settings → Resources → Memory → **8 GB+**。13 个容器总上限 6.5 GB，详见下方内存配置表 |
| **Docker daemon not running** | 启动 Docker Desktop，等待鲸鱼图标变绿 |

**容器内存配置一览**（`docker-compose.yml` 中 `mem_limit`）：

| 容器 | mem_limit | JVM 堆 | 说明 |
|------|-----------|--------|------|
| RocketMQ Broker | 1 GB | `-Xms256m -Xmx512m` | 默认 8 GB，dev 环境降至 512 MB |
| Elasticsearch | 1 GB | `-Xms512m -Xmx512m` | ES 官方最低推荐值，不变 |
| MySQL | 512 MB | `--innodb-buffer-pool-size=128M` | dev 数据量极小，128 MB 为 MySQL 8.0 默认 |
| Nacos | 512 MB | `JVM_XMS=256m JVM_XMX=256m` | 独立模式，几个服务注册 |
| WebSocket | 512 MB | `-Xms128m -Xmx384m` | 热点服务，Netty 连接 + 全局 Map |
| Message | 512 MB | `-Xms128m -Xmx384m` | 热点服务，ES 索引 + MQ 消费 |
| RocketMQ NameServer | 384 MB | `-Xms128m -Xmx256m` | 路由表很小 |
| Gateway | 384 MB | `-Xms128m -Xmx256m` | 纯路由转发，无 DB/Redis |
| User / Server / File | 384 MB | `-Xms128m -Xmx256m` | 轻量 CRUD |
| Redis | 256 MB | — | Alpine 原生，极省内存 |
| MinIO | 256 MB | — | Go 原生，轻量 |
| nacos-init | 128 MB | — | 仅 curl 发布配置，用完即退 |
| **合计上限** | **≈ 6.5 GB** | | 实际用量 ≈ 4–5 GB |

| 问题 | 解决 |
|------|------|
| **端口冲突** (3308/6381/8848/9004/9200) | 修改 `docker-compose.yml` 中的 host 端口映射（冒号左边） |
| **`nacos-init` 容器退出 FAILED** | Nacos 启动慢，docker compose 重试即可：`docker compose up -d nacos-init` |
| **Windows 端口不可用** | Win11 Hyper-V 会占用 9090/9876 等随机高端口 → 修改 docker-compose 或停用 Hyper-V |
| **ES 启动失败 `max virtual memory`** | `wsl -d docker-desktop sysctl -w vm.max_map_count=262144` |
| **MinIO "Share it" 弹窗** | 首次启动 Docker 会弹出文件共享确认，点击 **Share it**（允许挂载 `docs/ddl.sql`） |

### 本地开发

| 问题 | 解决 |
|------|------|
| **`mvn: command not found`** | 使用项目自带的 Maven Wrapper：`./mvnw install -N` |
| **`cannot find symbol` 或依赖解析失败** | 先 `mvn install -N && mvn install -pl community-common` 再编译目标模块 |
| **启动报 `dataId not found`** | Nacos 共享配置未发布 → 检查 `docker compose logs nacos-init` |
| **Gateway 路由 503** | 下游服务未启动或未注册到 Nacos → 检查 Nacos 控制台「服务管理」 |
| **Feign 调用失败** | 确认目标服务在 Nacos 中已注册，服务名与 `@FeignClient(name=...)` 一致 |

### 如何切换 Java 版本

项目使用 Java 21。如果系统有多个 Java 版本：

```bash
# Windows (Git Bash / MSYS2)
source ~/.bashrc && switch-java 21

# macOS / Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo /path/to/jdk-21)
```

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
