# 社群平台 (Discord-like) 新建项目计划

> **索引文件** — 各章节拆分为独立文件便于维护

## Context

在 MallChat 重构经验基础上，新建一个 **Spring Cloud 社群平台**（Discord 模式），供软件实训课程使用。

- **为什么新建**：MallChat 是扁平群聊模型，社群需要 Server > Category > Channel > Thread 嵌套结构 + RBAC
- **复用策略**：登录链路（微信 OAuth）完全移植，设计模式和事件管道迁移，业务代码重写
- **登录方式**：主路径为微信公众平台 OAuth 扫码（复用 MallChat 全链路）。用户名/密码仅作 seed data 测试通道（无注册端点，login 仅验证预置账号），供开发调试与课程演示快速上手
- **硬性要求**：Spring Cloud（Phase 8 微服务拆分）
- **架构策略**：Phase 0-7 单体编码 + 包级接口隔离（便于快速迭代）。Phase 8 按计划拆分为 Spring Cloud 微服务（Nacos + Gateway + Feign），此为**硬性要求**
- **评分重点**：演示视频 + 项目文档
- **目录位置**：独立路径 `E:\Learn_zone\Code_zone\IDEA_code\community-platform\`

---

## 文档索引

| 文件 | 内容 | 行数 |
|------|------|------|
| [mallchat-business-flows.md](./mallchat-business-flows.md) | **MallChat 现有业务流程精准描述**（微信扫码登录、好友/群组管理、消息全链路、WS 推送） | ~310 |
| [01-architecture.md](./01-architecture.md) | 功能模块全景、项目目录、技术栈版本、MallChat 模式复用速查 | ~160 |
| [02-business-flows.md](./02-business-flows.md) | **社群平台业务流程**（登录鉴权、Server/Channel CRUD、权限解析、消息订阅推送、Thread/Reaction/文件上传） | ~550 |
| [03-api-spec.md](./03-api-spec.md) | API 完整规格（40+ 端点）+ 前端交互时序 + WebSocket 状态机 | ~165 |
| [04-migration.md](./04-migration.md) | Phase 1 代码组织 + 包级接口隔离策略 + DDL 设计要点（15 张表 + 14 条决策） | ~165 |
| [05-dev-phases-0-4.md](./05-dev-phases-0-4.md) | 开发阶段：Phase 0 起点准备 → Phase 4 消息模块（~120 checkbox） | ~345 |
| [06-dev-phases-5-8.md](./06-dev-phases-5-8.md) | 开发阶段：Phase 5 WebSocket → Phase 8 微服务拆分（沿用 06+08 合并） | ~160 |
| [community-flows-visual.html](./community-flows-visual.html) | **可视化总览** — 4 Tab 页面：业务流程·实体关系 ER·接口映射·WS 协议（浏览器打开） | ~500 |
| [07-dev-rules.md](./07-dev-rules.md) | 开发规范：Git 提交粒度、每日流程、编译命令、关键约束 | ~48 |
| [08-microservice-upgrade-path.md](./08-microservice-upgrade-path.md) | **微服务升级操作手册** — 模块拆分 + Feign + Nacos/Gateway + 工作量估算 | ~290 |
| [08-optimizations.md](./08-optimizations.md) | 性能优化方案（补充）：双 Token、自动续期、安全加固记录 | ~90 |

---

## 阶段总览

```
Phase 0  → 起点准备      (项目骨架 + 基础设施)    ██ 地基
Phase 1  → 基础设施层    (common + transaction)   ██ 共享库
Phase 2  → 用户模块      (seed data 登录/微信扫码)  ██ 鉴权入口
Phase 3  → 社群模块      (Server/Channel/RBAC)    ██████ 核心一
Phase 4  → 消息模块      (Message/Thread/Reaction) ██████ 核心二
Phase 5  → 实时通信      (WebSocket/Typing)       ██ 体验
Phase 6  → 文件 + 搜索   (MinIO/ES)               ██ 增强
Phase 7  → 收尾完善      (测试/文档/冒烟)          ██ 交付
Phase 7+ → 边界审计 + WS 独立进程 (可选)            ██ 一步之遥
Phase 8  → 微服务拆分    (Spring Cloud)           ████ 架构升级 (详见 08-microservice-upgrade-path.md)
```

每个 Phase 结束条件：`mvn clean compile` 通过 + 本阶段 API 可 curl 验证。

---

## 当前进度

| Phase | 状态 | 提交 | 日期 |
|-------|------|------|------|
| Phase 0 | ✅ 完成 | `312d110` | 2026-06-20 |
| Phase 1 | ✅ 完成 | `65c3d60` | 2026-06-20 |
| Phase 2 | ✅ 完成 | `3fab073` | 2026-06-22 |
| Phase 3 | ✅ 完成 | `943ebfa` | 2026-06-22 |
| Phase 4 | ✅ 完成 | `632e093` | 2026-06-22 |
| Phase 5 | ⏳ 待开始 | — | — |
| Phase 6 | ⏳ 待开始 | — | — |
| Phase 7 | ⏳ 待开始 | — | — |
| Phase 8 | ⏳ 待开始 | — | — |

### 已完成但超出计划范围

| 交付物 | 说明 |
|--------|------|
| Docker 部署 | `Dockerfile` + `docker-compose.yml` + `application-docker.yml`（`1f7d513`） |
| README 重写 | 290 行精准指南：启动方式 + 功能清单 + 50+ 端点（`25e7dbe`） |
| 开发日志 | `docs/dev-log.md` 记录 Phase 0-4 所有实现细节和决策 |
