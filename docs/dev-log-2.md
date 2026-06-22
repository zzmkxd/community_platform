# 社群平台 开发日志 (2)

> **续篇** — 前篇 `dev-log.md` 覆盖 Phase 0 ~ Phase 4.6（项目骨架 → CI/CD）。本文档记录 Phase 5 起的工作。
>
> 前篇待办 T1~T11 已完成。**T7 (PushService) 2026-06-22 审计纠正：原误标 ✅，实际未实现。**

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前 Phase**: Phase 5 待开始（WebSocket 实时通信）
- **最新提交**: `36bc4be` — 打通消息推送管线（6 项阻断性缺口）
- **日期**: 2026-06-22

---

## 完整待办汇总（28 项，2026-06-22 审计生成）

### 一、🔴 阻断性：消息推送管线（Phase 4.x — 阻断 Phase 5.6 验证）

```
① MessageSendListener  →  发 RocketMQ SEND_MSG_TOPIC     (P4-4, 3 行代码)
② PushMessageDTO        →  新建 data class                 (新增)
③ MsgSendConsumer       →  消费 SEND_MSG → 转 PushDTO     (P4-1, 新类)
④ PushService           →  RocketMQTemplate 封装发 PUSH   (T7)
⑤ PushConsumer          →  解析 PushDTO → 分派            (5.3.1, 替换 TODO)
⑥ pushToChannel/Thread  →  Redis SMEMBERS → sendToUid     (5.3.3, 替换 TODO)
```

| # | 来源 | 条目 | 现状 | 文件 |
|---|------|------|------|------|
| ~~1~~ | ~~P4-4~~ | ~~MessageSendListener 发 MQ~~ | ✅ 已修复 (`36bc4be`) | 注入 RocketMQTemplate → SEND_MSG_TOPIC |
| ~~2~~ | ~~新增~~ | ~~PushMessageDTO~~ | ✅ 已创建 (`36bc4be`) | common/domain/dto/PushMessageDTO.java |
| ~~3~~ | ~~P4-1~~ | ~~MsgSendConsumer~~ | ✅ 已创建 (`36bc4be`) | message/consumer/MsgSendConsumer.java |
| ~~4~~ | ~~T7~~ | ~~PushService~~ | ✅ 已创建 (`36bc4be`) | message/service/PushService.java + impl |
| ~~5~~ | ~~5.3.1~~ | ~~PushConsumer 实现~~ | ✅ 已修复 (`36bc4be`) | 解析 PushMessageDTO → 分派 |
| ~~6~~ | ~~5.3.3~~ | ~~pushToChannel / pushToThread~~ | ✅ 已修复 (`36bc4be`) | Redis SMEMBERS → sendToUid |

### 二、🟡 Phase 4.x 其他遗留

| # | 来源 | 条目 | 说明 |
|---|------|------|------|
| 7 | P4-3 | autoArchiveThreads | `@Scheduled` 每小时归档 24h 无活动 Thread |
| 8 | P4-6r | Reaction 残留 | SearchServiceImpl:38 和 ThreadServiceImpl:129 仍传 `Collections.emptyList()` |

### 三、🟡 Phase 5 未完成（推送管线打通后）

| # | 来源 | 条目 | 说明 |
|---|------|------|------|
| 9 | 5.4.1-2 | Handler 路由 TYPING_START / TYPING_STOP | 枚举已定义 (type=9,10)，switch 未路由 |
| 10 | 新增 | WebSocketService.handleTypingStart/Stop | 新方法 + 广播给频道/Thread 其他订阅者 |
| 11 | 5.5.1-2 | MEMBER_ONLINE / MEMBER_OFFLINE | 登录/断连时广播给同服务器成员 |
| 12 | 5.3.4 | WSAdapter 缺 10 个 build 方法 | buildMessageDelete/Update, buildReactionRemove, buildTypingStop, buildMemberJoin/Leave, buildUserOnline/Offline, buildThreadCreate, buildChannelCreate/Update/Delete |

### 四、🔵 Backlog 功能缺口（来自 dev-log T4-T9）

| # | 来源 | 条目 | 现状 |
|---|------|------|------|
| 13 | T4 | EmojiMsgHandler | ❌ 不存在（SoundMsgHandler ✅ 已实现） |
| 14 | T5 | MentionParser（@提及正则 + uid 解析） | ❌ 不存在 |
| 15 | T8 | AC 自动机敏感词 | ❌ 未移植 MallChat `common/sensitive/` |
| 16 | T9 | WxMsg 持久化表 | 后续按需（微信原始消息审计） |

### 五、🔵 File 模块预检（Phase 6 前置）

| # | 条目 | 说明 |
|---|------|------|
| 17 | FileService 3 方法 | 全部 `throw UnsupportedOperationException("TODO")` |
| 18 | MinIOConfiguration | 不存在（MinIO 容器已就绪，缺 Spring Bean） |
| 19 | MessageAdapter 文件关联 | ImageMsgHandler/FileMsgHandler 需校验 fileId 已确认上传 |
| 20 | docker-compose 验证 MinIO | 已配置 9004-5，需联调 |

### 六、🔵 Search 模块预检（Phase 6 前置）

| # | 条目 | 说明 |
|---|------|------|
| 21 | SearchService ES 实现 | 当前 MySQL LIKE 占位，需替换为 ES Client |
| 22 | docker-compose 缺 ES 容器 | 需添加 Elasticsearch 8.11 服务 |

### 七、⚪ 技术债

| # | 来源 | 条目 |
|---|------|------|
| 23 | T12 | @SecureInvoke + MQProducer 事务安全投递 |
| 24 | T14 | CursorUtils.java unchecked 泛型警告 |
| 25 | T15 | WxMsgService.java deprecated API (weixin-java-mp 4.4.0) |

### 八、⚪ 文档

| # | 来源 | 条目 |
|---|------|------|
| 26 | T13 | mallchat-flows-visual.html 未复制（MallChat plan 有，社区平台未移植） |

### 九、⏳ Phase 7-8 架构升级

| # | 来源 | 条目 |
|---|------|------|
| 27 | T16 | 包级接口隔离审计（Phase 7+） |
| 28 | T17 | Nacos + Gateway + Feign 微服务拆分（Phase 8） |
| 29 | T18 | 配置中心迁移（MySQL/Redis/RocketMQ → Nacos） |

---

## Phase 4.x: 消息推送管线缺口

> 即上表 #1~#6。补全后消息全链路才完整。

### 当前链路断裂点

```
MessageServiceImpl.sendMessage()
  → AbstractMsgHandler.checkAndSaveMsg() ✅
  → eventPublisher.publishEvent(ChannelMessageSendEvent) ✅
  → MessageSendListener.onMessageSend()  ⚠️ 仅 log，未发 MQ        ← 缺口 #1
  → MsgSendConsumer                     ❌ 不存在                  ← 缺口 #3
  → PushService.sendPushMsg()           ❌ 不存在                  ← 缺口 #4
  → PushConsumer.onMessage()             ⚠️ 仅 log                 ← 缺口 #5
  → WebSocketService.pushToChannel()     ❌ TODO 空壳              ← 缺口 #6
  → WebSocketService.sendToUid()         ✅ 可用
```

---

## Phase 5: 基础设施现状

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 已就绪（13/26 checkbox）

| 组件 | 状态 | 说明 |
|------|------|------|
| `NettyWebSocketServer` | ✅ | 端口 8091，Pipeline 完整 |
| `HttpHeadersHandler` | ✅ | 提取 Token + IP |
| `NettyUtil` | ✅ | Channel Attribute 管理 |
| `NettyWebSocketServerHandler` | ✅ | switch 分发 7 种请求（LOGIN/HEARTBEAT/SEND_MESSAGE/SUBSCRIBE×3/UNSUBSCRIBE×2） |
| `WebSocketService` 接口 | ✅ | 55 行，完整定义 |
| `WebSocketServiceImpl` | ✅ | connect/authorize/removed/subscribe*/sendToUid/sendToAllOnline |
| `WSAdapter` | ⚠️ | 8 个 build 方法，缺 10 个 |
| `PushConsumer` | ⚠️ | 骨架存在，仅 log |
| `WSReqTypeEnum` | ✅ | 10 种请求（含 TYPING_START/STOP） |
| `WSRespTypeEnum` | ✅ | 18 种响应（含 TYPING/MEMBER/THREAD/CHANNEL/SERVER 推送） |

### 计划 checkbox 逐项核对

| 章节 | 完成 | 总数 | 缺口 |
|------|------|------|------|
| 5.1 Netty 服务器 | 4 | 4 | — |
| 5.2 连接管理 | 7 | 7 | — |
| 5.3 消息推送 | 1 | 4 | pushToChannel/Thread, PushConsumer, WSAdapter |
| 5.4 输入状态 | 0 | 3 | 全部未实现 |
| 5.5 在线状态 | 0 | 2 | 全部未实现 |
| 5.6 验证 | 2 | 7 | 推送/输入/Thread 场景不可验证 |
| **合计** | **14** | **27** | **13 条待补** |

---

## Phase 6: 文件 + 搜索 — 待开始

> 详细计划见 `docs/plan/06-dev-phases-5-8.md`

### 文件模块

| 组件 | 状态 |
|------|------|
| `FileController` (4 端点) | ✅ |
| `FileService` 接口 | ✅ |
| `FileServiceImpl` | ❌ 全部 throw TODO |
| `FileAttachment` Entity + Mapper + Dao | ✅ |
| MinIO 容器 (docker-compose) | ✅ 9004-5 |
| MinIOConfiguration (`@Configuration` Bean) | ❌ |

### 搜索模块

| 组件 | 状态 |
|------|------|
| `SearchController` | ✅ |
| `SearchService` 接口 | ✅ |
| `SearchServiceImpl` | ⚠️ MySQL LIKE 占位 |
| ES 容器 (docker-compose) | ❌ |
| ElasticsearchConfig | ❌ |

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-22 | 创建 2 号日志，续接 dev-log.md | `a08329a` |
| 2026-06-22 | 完整待办汇总（29 项）+ T7 纠错 | `0cfd4ea` |
| 2026-06-22 | **打通推送管线 6 项** — MessageSendListener → MQ → MsgSendConsumer → PushService → PushConsumer → pushToChannel/Thread | `36bc4be` |

---

## 参考

- 前篇开发日志: `docs/dev-log.md`
- 项目计划: `docs/plan/`
- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
