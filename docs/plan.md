# 社群平台 (Discord-like) 新建项目计划

> **详细内容已拆分为独立文件**，每个文件 ≤ 300 行，便于维护和阅读。

在 MallChat 重构经验基础上，新建一个 **Spring Cloud 社群平台**（Discord 模式），供软件实训课程使用。

- **目录位置**：`E:\Learn_zone\Code_zone\IDEA_code\community-platform\`
- **复用策略**：借鉴 MallChat 的设计模式和事件管道，业务代码重写
- **硬性要求**：Spring Cloud
- **评分重点**：演示视频 + 项目文档
- **数据库分库**：Phase 1 单库，Phase 2 拆

---

## 文档索引

| 文件 | 内容 |
|------|------|
| [plan/README.md](./plan/README.md) | 总索引 + 阶段总览 |
| [plan/01-architecture.md](./plan/01-architecture.md) | 功能模块全景、项目目录、技术栈版本、MallChat 模式复用速查 |
| [plan/02-business-flows.md](./plan/02-business-flows.md) | 六大核心业务流程（消息链路、权限解析、Thread/Reaction/文件上传） |
| [plan/03-api-spec.md](./plan/03-api-spec.md) | API 完整规格（40+ 端点）+ 前端交互时序 + WebSocket 状态机 |
| [plan/04-migration.md](./plan/04-migration.md) | Phase 1 代码组织 → Phase 2 微服务拆分映射 + DDL 设计要点 |
| [plan/05-dev-phases-0-4.md](./plan/05-dev-phases-0-4.md) | 开发 Phase 0~4：起点准备 → 基础设施 → 用户 → 社群 → 消息（80+ checkbox） |
| [plan/06-dev-phases-5-8.md](./plan/06-dev-phases-5-8.md) | 开发 Phase 5~8：WebSocket → 文件搜索 → 收尾 → 微服务拆分 |
| [plan/07-dev-rules.md](./plan/07-dev-rules.md) | 开发规范：Git 提交粒度、每日流程、编译命令、关键约束 |

---

## 阶段总览

```
Phase 0  → 起点准备      (项目骨架 + 基础设施)
Phase 1  → 基础设施层    (common + transaction)
Phase 2  → 用户模块      (注册/登录/JWT)
Phase 3  → 社群模块      (Server/Channel/RBAC)    ← 核心
Phase 4  → 消息模块      (Message/Thread/Reaction) ← 核心
Phase 5  → 实时通信      (WebSocket/Typing)
Phase 6  → 文件 + 搜索   (MinIO/ES)
Phase 7  → 收尾完善      (测试/文档/冒烟)
Phase 8  → 微服务拆分    (Spring Cloud)
```
