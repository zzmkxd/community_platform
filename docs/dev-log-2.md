# 社群平台 开发日志 (2)

> **续篇** — 前篇 `dev-log.md` 覆盖 Phase 0 ~ Phase 4.6（项目骨架 → CI/CD）。
> 本文档记录 Phase 5 起的工作。

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前 Phase**: Phase 6 完成（ES 搜索已集成），微服务拆分完成（zjwSpringCloud 分支）
- **最新提交**: `1969b0c` — 集成 ES
- **日期**: 2026-06-25

---

## Phase 5-6: 基础设施 + 文件 + 搜索 (2026-06-22 ~ 2026-06-25)

### Phase 5: WebSocket 实时推送

打通消息推送全链路（6 项，提交 `36bc4be`）：
```
MessageSendListener → RocketMQ SEND_MSG_TOPIC
  → MsgSendConsumer → PushService → PushConsumer
  → WebSocket pushToChannel/pushToThread (Redis Set 订阅制)
```

Typing 状态、在线/离线广播、WSAdapter 18 种推送类型全部补齐（提交 `7af4244`）。

### 消息管线增强

- **敏感词过滤**: AC 自动机 + DFAFilter 双模式，移植自 MallChat（11 文件）
- **EmojiMsgHandler**: 自定义表情消息处理
- **MentionParser**: @username/@all/@everyone 文本解析

### Phase 6: 文件 + 搜索

- **MinIO 预签名上传**: presign → confirm → download 三阶段
- **Elasticsearch 搜索**: 替代 MySQL LIKE，docker-compose 已包含 ES 容器（提交 `1969b0c`）

---

## 微服务拆分 (2026-06-24 ~ 2026-06-25, zjwSpringCloud 分支)

### 架构

```
客户端 → Gateway(:8080) → Nacos 服务发现
  ├── /api/v1/auth/**, /api/v1/users/**        → user-service    (:8081)
  ├── /api/v1/servers/**, /api/v1/channels/*/permissions/**
  │                                              → server-service  (:8082)
  ├── /api/v1/channels/*/messages/**, /api/v1/servers/*/search/**
  │   /api/v1/messages/*/reactions/**, /api/v1/channels/*/threads/**
  │                                              → message-service (:8083)
  └── /api/v1/upload/**                         → file-service    (:8084)

WebSocket: websocket (Netty :8091)，独立部署，不经过 Gateway
```

### 模块清单

| Maven 模块 | 端口 | 职责 |
|-----------|------|------|
| `community-gateway` | 8080 | Gateway + AuthGlobalFilter + Nacos 路由 |
| `community-user-service` | 8081 | 鉴权 + 用户 CRUD + 微信 OAuth |
| `community-server-service` | 8082 | Server/Category/Channel/Member/Role + RBAC |
| `community-message-service` | 8083 | 消息策略链 + Thread + Reaction + Search |
| `community-file-service` | 8084 | MinIO 预签名上传 |
| `community-websocket` | :8091 | Netty WS + PushConsumer |
| `community-common` | — | 共享库（utils/config/transaction/annotation/aspect） |

### 关键变更

- **Nacos 共享配置**: `community-platform-common.yaml`（数据源、Redis、RocketMQ、MyBatis-Plus）
- **OpenFeign 远程调用**: 跨服务调用替代本地 Service 注入
- **Gateway 路由**: 路径匹配 + JWT 全局鉴权（AuthGlobalFilter）
- **Docker Compose**: 7 个微服务容器 + Nacos 初始化容器（自动发布共享配置）
- 旧单体 `community-server/` 已删除

---

## 代码质量审计 (2026-06-25)

> 全量审查微服务架构下 232 个 Java 文件，发现 29 项问题，分 P0/P1/P2 三级。

### P0 — 数据丢失 / 安全泄露 (4 项)

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| P0-1 | **MQ Consumer 吞异常** | `PushConsumer.java:34` `MsgSendConsumer.java:89` | `catch (Exception e)` 仅 log，不重试不死信，消息静默丢失 |
| P0-2 | **MinIO 空 catch** | `MinIOConfiguration.java:31` | bucket 创建失败也返回 "OK"，不可达时静默 |
| P0-3 | **敏感配置泄露** | 6 个 `application-*.yml/properties` | 微信 AppSecret + JWT/DB/Redis/MinIO 密码全部明文 |
| P0-4 | **Gateway 鉴权绕过** | `AuthGlobalFilter.java` | 白名单 `List.contains()` 精确匹配可被尾部 `/` 绕过；`/internal/` 端点无来源 IP 过滤 |

### P1 — 性能 / 异常处理 (4 项)

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| P1-1 | **N+1 查询 (4 处)** | `ChannelReadStateServiceImpl.java:63-79` + Role assign + Thread archive + Permission check | 循环内 DB 查询，50 频道 = 101 次往返 |
| P1-2 | **冗余 DB 查询** | `ServerServiceImpl.java:149-218` (7 次) + `MemberServiceImpl` (SELECT*) + `InviteServiceImpl` (6 次) | 可合并为 JOIN 或批量查询 |
| P1-3 | **缺索引** | `message(channel_id, id)` + `thread(status, last_active)` | 游标分页 + 归档扫描无复合索引 |
| P1-4 | **异常处理缺陷** | `ChannelServiceImpl` 返回 null + `SecureInvokeService.java:112` catch Throwable + `@Async` 吞异常 | NPE 风险 + Error 类型不应重试 |

### P2 — 安全加固 / 代码习惯 (2 项)

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| P2-1 | **输入校验缺失** | 14 个 Controller 无 `@Valid` + Nacos 未认证 + WS Token URL 传递 | 参数校验、服务发现、WS 认证三面 |
| P2-2 | **代码习惯** | 多处 | `getById` 替代 `lambdaQuery` + 魔法数字 + catch 块写业务逻辑 |

### 已确认无需修复

| 项目 | 判定 |
|------|------|
| `@SecureInvoke` 本地消息表 | ✅ 已移植（`7aad1db`），MQ at-least-once 保障 |
| CQ1-CQ4 重复代码抽取 | ✅ 已修复（`7aad1db`）— MembershipValidator + MessageAdapter |
| ES 搜索 | ✅ 已集成（`1969b0c`） |
| 微服务拆分 | ✅ 已完成（zjwSpringCloud 分支） |

---

## 待办追踪

### 仍未完成（Phase 7+ 规划）

| # | 条目 | 说明 |
|---|------|------|
| — | WebRTC 语音频道 | Phase 7 |
| — | 加入审批流 (ServerMemberApply) | Phase 7 |
| — | 零测试覆盖 | Phase 7.1 规划中 |
| — | WxMsg 持久化表 | 后续按需 |
| — | mallchat-flows-visual.html | 文档项，未移植 |

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-22 | 创建 2 号日志，续接 dev-log.md | `a08329a` |
| 2026-06-22 | 打通推送管线 6 项 | `36bc4be` |
| 2026-06-22 | Phase 4.x 遗留 + Phase 5 6 项 — Typing/Online/WSAdapter | `7af4244` |
| 2026-06-23 | Backlog: 敏感词 + EmojiMsgHandler + MentionParser | `6ee4354` |
| 2026-06-23 | Phase 6: MinIOConfiguration + FileServiceImpl + ES 容器 | `6ee4354` |
| 2026-06-23 | 审计修复 (A1-A11): 端口同步 + SQL注入 + FileController + 分页 | `6ee4354` |
| 2026-06-23 | API 全量测试 + 运行时修复 (A19-A22): MinIO region + FileVO.status | `6ee4354` |
| 2026-06-23 | Plan-vs-Code 全量审计: 46/46 端点通过 + D1-D9 结构差异 | `6ee4354` |
| 2026-06-23 | Round 1-4: Bug修复 + Redisson + 频控AOP + AbstractLocalCache | `6ee4354` |
| 2026-06-23 | Phase 0-6 功能缺口审计: G1-G16 + N1-N4 | `f5f20cc` ~ `04a5877` |
| 2026-06-23 | @SecureInvoke 移植 (CR1) + 重复代码抽取 (CQ1-CQ4) | `7aad1db` |
| 2026-06-24 | 微服务拆分: Nacos + Gateway + Feign，6 服务 + 1 共享库 | `b72e7ef` ~ `fd9ef2e` |
| 2026-06-25 | 全链路验证通过: 登录 → Server → Channel → WS → 消息推送 | `e2e5f99` |
| 2026-06-25 | 文档同步微服务架构: CLAUDE.md + README.md + docker-compose | `8640f6f` |
| 2026-06-25 | 删除旧单体 + 服务健康检查初始化 | `67063d5` |
| 2026-06-25 | ES 搜索集成 | `1969b0c` |
| 2026-06-25 | **全量代码审计**: 29 项问题 (P0×4 / P1×4 / P2×2) + 10 项待办录入 | 本文档 |
| 2026-06-26 | DDL JSON→TEXT: MySQL 5.6 兼容，保留 5.7+ 升级路径 | `111fca7` |
| 2026-06-26 | **全栈跑通**: POM skip 修复 + Nacos 本地配置 + 冒烟脚本 17/17 通过 | `dcbbf30` |

---

## 参考

- 前篇开发日志: `docs/dev-log.md`
- 项目计划: `docs/plan/`
- MallChat 源项目: `E:\Learn_zone\Code_zone\IDEA_code\Mall\MallChat\`
