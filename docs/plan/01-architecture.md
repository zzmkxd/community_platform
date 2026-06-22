# 一、功能模块全景 + 技术栈 + 复用速查

## 模块地图

```
┌─────────────────────────────────────────────────────────┐
│                    community-platform                    │
├──────────┬──────────┬──────────┬──────────┬────────────┤
│  用户模块  │  社群模块  │  消息模块  │  文件模块  │  WebSocket │
│  user/   │ server/  │ message/ │  file/   │ websocket/ │
├──────────┼──────────┼──────────┼──────────┼────────────┤
│ · 注册    │ · Server │ · 消息    │ · 上传    │ · 连接管理  │
│ · 登录    │ · Category│ · Thread │ · 下载    │ · 频道订阅  │
│ · JWT    │ · Channel│ · Reaction│ · 预览    │ · 实时推送  │
│ · 个人资料 │ · 成员    │ · 搜索    │ · 缩略图   │ · 输入状态  │
│ · 好友(延) │ · 角色    │ · Markdown│          │ · 在线状态  │
│          │ · 权限    │ · 表情    │          │            │
└──────────┴──────────┴──────────┴──────────┴────────────┘

基础设施 (docker-compose):
  MySQL 8.0  |  Redis 7  |  RocketMQ 5.1.4  |  MinIO  |  Elasticsearch 8
```

## 1.1 用户模块

| 功能 | 说明 | MallChat 参考 |
|------|------|--------------|
| 微信扫码登录 | 主路径。公众号 OAuth 扫码 → 创建/查找 User → JWT | `WxPortalController` + `WxMsgService` + `WebSocketServiceImpl` |
| 用户名/密码登录 | seed data 测试通道。DDL 预置账号，无注册端点 | `JwtUtils` + `AuthService.login()` |
| Token 校验 | 拦截器从 Header 提取，查 Redis 验证 | `TokenInterceptor` 模式 |
| 个人资料 | 微信同步 + 后续可修改 | 新增 |
| RequestHolder | ThreadLocal 持有当前用户 uid + IP | 完全复用 |

> 设计决策 (2026-06-22)：主路径为微信 OAuth，用户名/密码仅 seed data 测试用。新用户只通过微信扫码创建。

## 1.2 社群模块

这是整个项目的核心，也是和 MallChat 差异最大的部分。

```
Server (服务器/社群)
├── Category A (分类，如"学习区")
│   ├── Channel 1: #闲聊 (TEXT，公开)
│   ├── Channel 2: #问答 (TEXT，仅 Thread 模式)
│   └── Channel 3: #资源 (TEXT，可发文件)
├── Category B (分类，如"管理区")
│   ├── Channel 4: #公告 (TEXT，仅管理员发消息)
│   └── Channel 5: #管理员讨论 (TEXT，仅特定角色可见)
└── (无分类的频道也可以存在)
```

| 功能 | 说明 |
|------|------|
| Server CRUD | 创建/编辑/删除（仅 owner），查看列表 |
| Category CRUD | 创建/编辑/删除/拖拽排序 |
| Channel CRUD | 创建/编辑/删除/排序，type=TEXT 或 VOICE |
| 成员管理 | 加入（自由/审核/邀请）、离开、踢出、成员列表（光标分页） |
| 服务器内昵称 | 成员可在不同服务器设不同昵称 |
| 角色系统 | 每个服务器有默认 @everyone 角色 + 自定义角色 |
| 权限系统 | 角色级权限位图 + 频道级覆盖（allow/deny） |
| 表情管理 | 服务器内上传自定义表情（静态/动态） |

## 1.3 消息模块

| 功能 | 说明 | MallChat 参考 |
|------|------|--------------|
| 发消息 | Text/Image/File/System/Sound 五种类型 | `MsgHandlerFactory` 策略模式 |
| 消息列表 | 频道内光标分页（`?cursor=&pageSize=50`） | `CursorPageBaseResp` |
| 编辑消息 | 仅作者，标记 `edited` | 新增 |
| 删除消息 | 作者或管理员 | 新增 |
| Thread | 从消息创建话题分支，话题内独立消息分页 | 新增 |
| Reaction | 添加/移除 Emoji，按消息聚合展示 | 扩展 MallChat Like/Dislike |
| 消息搜索 | ES 全文搜索（频道维度 + 服务器全局） | 新增 |
| Markdown | 消息内容存 Markdown，前端渲染 | 新增 |
| @提及 | `@username` 解析为 uid 存入 extra JSON | 新增 |
| 语音消息 | 录音上传后发 SoundMsgDTO，时长+URL 存入 extra | 复用 MallChat `SoundMsgHandler` |

## 1.4 文件模块

| 功能 | 说明 | MallChat 参考 |
|------|------|--------------|
| 预签名上传 | 前端直接传 MinIO，后端只给 URL | `MinIOTemplate` |
| 上传确认 | 上传完成后回调确认，保存元数据 | 新增 |
| 下载/预览 | 返回签名 URL，图片可缩略 | 新增 |
| 文件关联 | file_attachment 关联 message_id | 新增 |

## 1.5 WebSocket 模块

| 功能 | 说明 | MallChat 参考 |
|------|------|--------------|
| 连接管理 | Netty on 8091，JWT 认证 | `NettyWebSocketServer` 完整复用 |
| 频道订阅 | 客户端声明订阅哪些频道，仅收到相关推送 | 新增（MallChat 是房间级全推） |
| 消息推送 | RocketMQ → PushConsumer → WS 帧 | `PushService` 模式复用 |
| 输入状态 | TYPING_START/STOP，广播给同频道其他人 | 新增 |
| 在线状态 | 上线/下线通知 | `WSOnlineOfflineNotify` 复用 |

---

## 项目目录

```
E:\Learn_zone\Code_zone\IDEA_code\community-platform\
```

与 MallChat 平级，避免互相污染。

---

## 技术栈版本锁定

```
JDK:             21
Spring Boot:     3.3.5
Spring Cloud:    2023.0.3
  ├── Gateway:   4.1.x
  ├── OpenFeign: 4.1.x
  └── LoadBalancer: 4.1.x
Spring Cloud Alibaba: 2023.0.1.0
  └── Nacos:     2.3.x
MySQL:           8.0
Redis:           7.x (Alpine)
Redisson:        3.36.0
RocketMQ:        5.1.4
Elasticsearch:   8.11.x
MinIO:           latest
MyBatis-Plus:    3.5.10.1 (mybatis-plus-spring-boot3-starter)
Netty:           4.1.114.Final
Springdoc:       2.6.0
Knife4j:         4.4.0
Hutool:          5.8.34
Lombok:          1.18.36
auth0 java-jwt:  3.19.0
CommonMark:      0.21.0
Docker Compose:  v2
```

---

## MallChat 模式复用速查

| MallChat 源文件（参考） | 新项目对应位置 | 复用方式 |
|------------------------|---------------|---------|
| `chat/service/strategy/msg/MsgHandlerFactory.java` | `message/service/strategy/msg/` | 照搬自注册模式 |
| `chat/service/strategy/msg/AbstractMsgHandler.java` | `message/service/strategy/msg/` | 照搬模板方法骨架 |
| `chat/service/strategy/msg/SoundMsgHandler.java` | `message/service/strategy/msg/` | 照搬（语音消息） |
| `chat/service/strategy/mark/MsgMarkFactory.java` | `message/service/strategy/reaction/` | 改为 Reaction 策略 |
| `common/service/cache/AbstractRedisStringCache.java` | `common/cache/` | 直接移植 |
| `common/common/event/MessageSendEvent.java` | `message/event/` | 改为 ChannelMessageSendEvent |
| `websocket/NettyWebSocketServer.java` | `websocket/` | 90% 照搬 |
| `websocket/NettyWebSocketServerHandler.java` | `websocket/` | 照搬 + 订阅逻辑 |
| `websocket/HttpHeadersHandler.java` | `websocket/` | 完全照搬 |
| `user/service/WebSocketService.java` | `websocket/service/` | 照搬 + subscribe API |
| `user/service/adapter/WSAdapter.java` | `websocket/service/adapter/` | 扩展新 WS 类型 |
| `common/utils/CursorUtils.java` | `common/utils/` | 完全照搬 |
| `common/interceptor/TokenInterceptor.java` | `common/interceptor/` | 照搬 |
| `common/exception/GlobalExceptionHandler.java` | `common/exception/` | 照搬 |
| `transaction/annotation/SecureInvoke.java` | `transaction/annotation/` | 直接移植 |
| `transaction/aspect/SecureInvokeAspect.java` | `transaction/aspect/` | 直接移植 |
| `mallchat-oss-starter/MinIOTemplate.java` | `file/config/MinIOConfiguration.java` | 模式复用 |
| `common/config/RedissonConfig.java` | `common/config/` | 照搬 @Value 注入方式 |
| `mallchat-frequency-control/` | `common/annotation/@FrequencyControl` | 策略模式移植 |
