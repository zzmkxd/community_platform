# 社群平台 开发日志 (2)

> **续篇** — 前篇 `dev-log.md` 覆盖 Phase 0 ~ Phase 4.6（项目骨架 → CI/CD）。本文档记录 Phase 5 起的工作。
>
> 前篇结尾的待办清单（T1~T18）中，T1~T11 已完成，T12~T18 仍待处理。

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前 Phase**: Phase 5 待开始（WebSocket 实时通信）
- **最新提交**: `f940ec5` — 计划文档审计同步
- **日期**: 2026-06-22

---

## Phase 4.x: 消息推送管线缺口（待补）

> 以下为计划审计中确认的剩余缺口。补全后消息全链路才完整。

### 缺口清单

| # | 缺失内容 | 说明 |
|---|---------|------|
| P4-1 | **MsgSendConsumer** | `MessageSendListener` 发事件后，需 RocketMQ Consumer 消费并调 PushService |
| P4-2 | **PushService / PushConsumer 实现** | `PushConsumer` 骨架存在（仅 log），`pushToChannel`/`pushToThread` 空壳 |
| P4-3 | **autoArchiveThreads** | `@Scheduled` 每小时归档 24h 无活动 Thread |
| P4-6r | **Reaction 残留** | `SearchServiceImpl:38` 和 `ThreadServiceImpl:129` 仍传 `Collections.emptyList()` |

### 当前消息发送链路断裂点

```
MessageServiceImpl.sendMessage()
  → AbstractMsgHandler.checkAndSaveMsg() ✅
  → eventPublisher.publishEvent(ChannelMessageSendEvent) ✅
  → MessageSendListener.onMessageSend()  ⚠️ 仅 log，未发 MQ
  → MsgSendConsumer                     ❌ 不存在
  → PushService.sendPushMsg()           ❌ 不存在
  → PushConsumer.onMessage()             ⚠️ 仅 log
  → WebSocketService.pushToChannel()     ❌ TODO 空壳
  → WebSocketService.sendToUid()         ✅ 可用（ONLINE_UID_MAP）
```

---

## Phase 5: 实时通信（WebSocket）— 待开始

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 现有基础设施（已就绪）

| 组件 | 状态 | 说明 |
|------|------|------|
| `NettyWebSocketServer` | ✅ | 端口 8091，Pipeline 完整（IdleStateHandler → HttpServerCodec → ... → WebSocketServerProtocolHandler） |
| `HttpHeadersHandler` | ✅ | 提取 Token + IP |
| `NettyUtil` | ✅ | Channel Attribute 管理 |
| `NettyWebSocketServerHandler` | ✅ | 请求分发（AUTHORIZE/LOGIN/SUBSCRIBE/SEND_MESSAGE 等） |
| `WebSocketService` (接口) | ✅ | 55 行，完整定义 |
| `WebSocketServiceImpl` | ✅ | connect/authorize/removed/subscribeChannel/subscribeThread/sendToUid 已实现 |
| `WSAdapter` | ✅ | 构建 WS 响应（LOGIN_SUCCESS/INVALID_TOKEN/SCAN_SUCCESS 等） |
| `PushConsumer` | ⚠️ | 骨架存在，仅 log |
| `WSRespTypeEnum` / `WSReqTypeEnum` | ✅ | 10 种请求 + 8 种响应类型 |

### 待实现

| 条目 | 说明 |
|------|------|
| pushToChannel 实现 | 从 Redis Set 读取订阅者 → 逐个 sendToUid |
| pushToThread 实现 | 同上，按 threadId 维度 |
| handleSendMessage 实现 | 解析 WS 消息请求 → 调 MessageService.sendMessage() |
| TYPING_START/STOP | 广播给同频道/Thread 其他订阅者（防抖） |
| MEMBER_ONLINE/OFFLINE | 上线/离线广播给同服务器在线成员 |

---

## Phase 6: 文件上传 + 搜索 — 待开始

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| `FileController` | ✅ | 4 个端点 |
| `FileService` (接口) | ✅ | 3 个方法定义 |
| `FileServiceImpl` | ❌ | 全部 `throw UnsupportedOperationException("TODO")` |
| `FileAttachment` Entity + Mapper + Dao | ✅ | 完整 |
| `SearchController` | ✅ | 1 个端点 |
| `SearchService` (接口) | ✅ | 1 个方法定义 |
| `SearchServiceImpl` | ⚠️ | MySQL LIKE 占位，非计划中的 ES 8.11 |
| MinIO 容器 | ✅ | docker-compose 已配置（端口 9004-5） |
| ES 容器 | ❌ | 未在 docker-compose 中 |

---

## 待办：后续升级清单（延续自 dev-log.md）

### 未完成项

| # | 条目 | 说明 |
|---|------|------|
| T12 | `@SecureInvoke` + `MQProducer` | MallChat 事务安全投递模式，当前用 RocketMQTemplate 裸调 |
| T13 | `mallchat-flows-visual.html` 未复制 | MallChat plan 有第二个 HTML 可视化 |
| T14 | `CursorUtils.java` unchecked 警告 | 泛型强制转换，不影响编译 |
| T15 | `WxMsgService.java` deprecated API | weixin-java-mp 4.4.0 API 变更 |

### 架构升级（Phase 8 硬性要求）

| # | 条目 | 说明 |
|---|------|------|
| T16 | 包级接口隔离审计 | Phase 7+ 检查所有跨包引用 |
| T17 | Nacos + Gateway + Feign | Phase 8 微服务拆分 |
| T18 | 配置中心迁移 | MySQL/Redis/RocketMQ 连接串抽取到 Nacos |

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-22 | 创建 2 号日志，续接 dev-log.md | — |

---

## 参考

- 前篇开发日志: `docs/dev-log.md`
- 项目计划: `docs/plan/`
- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
