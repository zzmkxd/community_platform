# 社群平台 (Discord-like) 新建项目计划

> **索引文件** — 各章节拆分为独立文件便于维护

## Context

在 MallChat 重构经验基础上，新建一个 **Spring Cloud 社群平台**（Discord 模式），供软件实训课程使用。

- **为什么新建**：MallChat 是扁平群聊模型，社群需要 Server > Category > Channel > Thread 嵌套结构 + RBAC
- **复用策略**：借鉴 MallChat 的设计模式和事件管道，业务代码重写
- **硬性要求**：Spring Cloud
- **评分重点**：演示视频 + 项目文档
- **目录位置**：独立路径 `E:\Learn_zone\Code_zone\IDEA_code\community-platform\`
- **数据库分库**：Phase 1 单库，Phase 2 拆

---

## 文档索引

| 文件 | 内容 | 行数 |
|------|------|------|
| [01-architecture.md](./01-architecture.md) | 功能模块全景、项目目录、技术栈版本、MallChat 模式复用速查 | ~170 |
| [02-business-flows.md](./02-business-flows.md) | 六大核心业务流程（消息链路、权限解析、Thread/Reaction/文件上传） | ~185 |
| [03-api-spec.md](./03-api-spec.md) | API 完整规格（40+ 端点）+ 前端交互时序 + WebSocket 状态机 | ~155 |
| [04-migration.md](./04-migration.md) | Phase 1 代码组织 → Phase 2 微服务拆分映射 + DDL 设计要点 | ~115 |
| [05-dev-phases-0-4.md](./05-dev-phases-0-4.md) | 开发阶段：Phase 0 起点准备 → Phase 4 消息模块（80+ checkbox） | ~300 |
| [06-dev-phases-5-8.md](./06-dev-phases-5-8.md) | 开发阶段：Phase 5 WebSocket → Phase 8 微服务拆分 | ~160 |
| [07-dev-rules.md](./07-dev-rules.md) | 开发规范：Git 提交粒度、每日流程、编译命令、关键约束 | ~45 |

---

## 阶段总览

```
Phase 0  → 起点准备      (项目骨架 + 基础设施)    ██ 地基
Phase 1  → 基础设施层    (common + transaction)   ██ 共享库
Phase 2  → 用户模块      (注册/登录/JWT)          ██ 鉴权入口
Phase 3  → 社群模块      (Server/Channel/RBAC)    ██████ 核心一
Phase 4  → 消息模块      (Message/Thread/Reaction) ██████ 核心二
Phase 5  → 实时通信      (WebSocket/Typing)       ██ 体验
Phase 6  → 文件 + 搜索   (MinIO/ES)               ██ 增强
Phase 7  → 收尾完善      (测试/文档/冒烟)          ██ 交付
Phase 8  → 微服务拆分    (Spring Cloud)           ████ 架构升级
```

每个 Phase 结束条件：`./mvnw clean compile` 通过 + 本阶段 API 可 curl 验证。
