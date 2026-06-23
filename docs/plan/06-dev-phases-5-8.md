# 十、开发阶段划分 — Phase 5~8

> Phase 0-4 → [05-dev-phases-0-4.md](./05-dev-phases-0-4.md)

每个 Phase 结束条件：`mvn clean compile` 通过 + 本阶段 API 可 curl 验证。

---

### Phase 5: 实时通信（WebSocket）

**目标**：Netty WebSocket 服务器，频道订阅/退订，实时消息推送，输入状态，在线通知
**状态**：✅ 全部完成（含消息推送管线修复 + 成员/频道/服务器生命周期 WS 推送）

> Phase 5 初始提交 `7af4244` 完成 Netty + 订阅 + 输入 + 在线。后续加固：
> - 推送管线打通 (`36bc4be`, `7af4244`)：MessageSendListener → MQ → MsgSendConsumer → PushService → PushConsumer
> - WS 生命周期推送 (`04a5877`)：Member join/leave/kick + Channel create/update/delete + Server update

#### 5.1 Netty 服务器
- [x] 5.1.1 `NettyWebSocketServer.java` — 端口 8091，Pipeline 配置（从 MallChat 照搬）
- [x] 5.1.2 `HttpHeadersHandler.java` — 从 URL 参数提取 Token + IP（照搬）
- [x] 5.1.3 `NettyUtil.java` — Channel Attribute 管理（照搬）
- [x] 5.1.4 `NettyWebSocketServerHandler.java` — WS 帧处理（照搬 + 扩展）

#### 5.2 连接管理
- [x] 5.2.1 `WebSocketService.connect(channel)` — 存储 WS Channel
- [x] 5.2.2 `WebSocketService.authorize(channel, token)` — JWT 验证 → 绑定 uid → 发送 LOGIN_SUCCESS
- [x] 5.2.3 `WebSocketService.removed(channel)` — 清理订阅 → 广播离线
- [x] 5.2.4 `WebSocketService.subscribeChannel(uid, channelId)` — 订阅频道
- [x] 5.2.5 `WebSocketService.unsubscribeChannel(uid, channelId)` — 退订
- [x] 5.2.6 `WebSocketService.subscribeThread(uid, threadId)` — 订阅话题
- [x] 5.2.7 `WebSocketService.unsubscribeThread(uid, threadId)` — 退订

#### 5.3 消息推送
- [x] 5.3.1 `PushConsumer` — RocketMQ PUSH_TOPIC 消费
- [x] 5.3.2 `WebSocketService.sendToUid(uid, wsBaseResp)` — 单用户推送
- [x] 5.3.3 `WebSocketService.sendToSubscribers(channelId/threadId, wsBaseResp)` — 按订阅推送
- [x] 5.3.4 `WSAdapter` — 构建各种 WS 响应（buildMsgSend, buildReactionAdd, buildTyping...）

#### 5.4 输入状态
- [x] 5.4.1 处理 `TYPING_START` 请求 → 广播给同频道/Thread 其他订阅者
- [x] 5.4.2 处理 `TYPING_STOP` → 广播停止
- [x] 5.4.3 前端防抖：连续输入中不重复发 TYPING_START（2s 间隔）

#### 5.5 在线状态
- [x] 5.5.1 用户上线 → 广播 MEMBER_ONLINE 给同服务器在线成员（可选）
- [x] 5.5.2 用户离线 → 广播 MEMBER_OFFLINE

#### 5.6 验证
- [x] 5.6.1 WebSocket 连接 → AUTHORIZE → 收到 LOGIN_SUCCESS
- [x] 5.6.2 SUBSCRIBE_CHANNEL → 发消息 → 实时收到 MESSAGE 推送
- [x] 5.6.3 SUBSCRIBE_THREAD → Thread 内有新消息 → 实时收到
- [x] 5.6.4 UNSUBSCRIBE_CHANNEL → 发消息 → 不收到推送
- [x] 5.6.5 断连 → subscriptions 清理
- [x] 5.6.6 输入 → 另一客户端收到 TYPING_INDICATOR
- [x] 5.6.7 Git 提交：`feat(phase5): Netty WebSocket 实时推送 + 频道订阅 + 输入状态`

**MallChat 参考**：`NettyWebSocketServer.java`、`HttpHeadersHandler.java`、`NettyWebSocketServerHandler.java`、`WebSocketServiceImpl.java`、`PushConsumer`、`WSAdapter`

---

### Phase 6: 文件上传 + 消息搜索

**目标**：MinIO 预签名上传全流程 + Elasticsearch 全文搜索

#### 6.1 MinIO 文件上传
- [x] 6.1.1 `MinIOConfiguration.java` — MinioClient Bean（从 MallChat 移植配置）
- [x] 6.1.2 `file_attachment` 表 DDL
- [x] 6.1.3 `FileAttachment` Entity + Mapper + Dao
- [x] 6.1.4 `FileService.getPresignedUrl()` — 生成 PUT 预签名 URL（5 分钟有效期）
- [x] 6.1.5 `FileService.confirmUpload()` — 校验文件存在 → 更新状态 → 提取元数据
- [x] 6.1.6 `FileService.getFile()` — 文件元数据查询
- [x] 6.1.7 `FileService.getDownloadUrl()` — 生成 GET 预签名 URL
- [x] 6.1.8 `FileController` — /api/v1/upload/presigned, /confirm, /files/{id}, /files/{id}/download

#### 6.2 消息中的文件关联
- [x] 6.2.1 `ImageMsgHandler` 校验 → fileId 必须已确认上传
- [x] 6.2.2 `FileMsgHandler` 校验 → 同上
- [x] 6.2.3 `MessageAdapter` 组装时填充 attachments[] 为 FileVO 列表

#### 6.3 Elasticsearch 搜索
- [ ] 6.3.1 `ElasticsearchConfig.java` — ES Client Bean
- [ ] 6.3.2 `MessageDocument` — ES 索引文档（id, channelId, serverId, fromUid, content, type, createTime）
- [ ] 6.3.3 `MessageSearchRepository` — Spring Data ES Repository
- [ ] 6.3.4 `SearchService.indexMessage()` — 消息发送后异步写入 ES（通过 RocketMQ）
- [ ] 6.3.5 `SearchService.searchMessages()` — 全文搜索 + 过滤（serverId, channelId?, from?, to?）
- [ ] 6.3.6 `SearchController` — /api/v1/servers/{id}/search?q=&channelId=&from=&to=

#### 6.4 验证
- [x] 6.4.1 获取预签名 URL → 前端 PUT 到 MinIO → confirm → 返回 FileVO
- [x] 6.4.2 发图片消息带 fileId → 消息返回含 attachments
- [ ] 6.4.3 发多条消息 → ES 搜索关键词 → 返回匹配消息列表
- [ ] 6.4.4 按频道/时间范围过滤搜索 → 结果正确
- [x] 6.4.5 Git 提交：`feat(phase6): MinIO 预签名上传 + Elasticsearch 全文搜索`

**MallChat 参考**：`mallchat-oss-starter/` 的 MinIO 配置和预签名 URL 模式

---

### Phase 7: 收尾完善

**目标**：测试、文档、冒烟测试脚本、API 文档，达到可交付状态

#### 7.1 测试
- [ ] 7.1.1 权限解析算法单元测试（至少 6 个用例）
- [ ] 7.1.2 `MessageService` 集成测试（Spring Boot Test + H2 或 Testcontainers）
- [ ] 7.1.3 `UserService` 集成测试
- [ ] 7.1.4 关键 Controller MockMvc 测试

#### 7.2 API 文档
- [ ] 7.2.1 Springdoc 注解：所有 Controller + VO 标注 @Tag/@Operation/@Schema
- [ ] 7.2.2 Knife4j 可访问 `/doc.html`
- [ ] 7.2.3 验证：Knife4j 页面显示所有 40+ 接口

#### 7.3 冒烟测试脚本
- [ ] 7.3.1 创建 `scripts/smoke-test.sh`（参考 MallChat smoke-test.sh）
- [ ] 7.3.2 覆盖：注册 → 登录 → 创服务器 → 创频道 → 发消息 → 搜消息 → 文件上传 → 权限拒绝
- [ ] 7.3.3 验证：30+ 断言全通过

#### 7.4 文档
- [ ] 7.4.1 `README.md` — 项目说明 + 快速启动 + 技术栈 + 端口映射 + FAQ
- [ ] 7.4.2 `docs/api-spec.md` — API 接口文档（可自动生成）
- [ ] 7.4.3 `docs/design-notes.md` — 设计决策记录（权限模型、Thread 复用 message 表、事件管道）
- [ ] 7.4.4 `docs/schema.sql` 确认与代码一致

#### 7.5 验证
- [ ] 7.5.1 Docker Compose 一键启动全部服务 + 应用
- [ ] 7.5.2 `bash scripts/smoke-test.sh` 全部通过
- [ ] 7.5.3 Knife4j 文档可浏览
- [ ] 7.5.4 `mvn clean package -DskipTests` BUILD SUCCESS
- [ ] 7.5.5 Git 提交：`release(phase7): 完整交付 — 测试 + 文档 + 冒烟`

---

### Phase 7+: 接口边界审计 + WebSocket 独立进程（可选）

**目标**：验证包级接口隔离 + 可选拆分 Netty WebSocket 为独立进程。

> 详细设计见 [04-migration.md](./04-migration.md) 5.2 节

#### 7+.1 接口边界审计
- [ ] 7+.1.1 检查所有 `import`：无跨包 DAO/Mapper 引用
- [ ] 7+.1.2 检查所有 `import`：无跨包 Entity 引用
- [ ] 7+.1.3 检查每个业务包的 service 子包：关键接口有完整定义
- [ ] 7+.1.4 验证：`mvn clean compile` 全模块通过

#### 7+.2 WebSocket 独立进程（可选，推荐）
- [ ] 7+.2.1 创建 `community-websocket` 模块 — 仅含 Netty + PushConsumer，不依赖 Spring Web/Tomcat
- [ ] 7+.2.2 创建 `NettyWsApplication` 启动类（`@SpringBootApplication`，排除 Web 自动配置）
- [ ] 7+.2.3 主服务 `community-server` 共享 `community-common` JAR
- [ ] 7+.2.4 两个进程通过 RocketMQ 通信（PUSH_TOPIC 已就绪，无需改代码）
- [ ] 7+.2.5 验证：同时启动两个进程 → WS 连接 8091 → HTTP 请求 8080 → 消息推送正常
- [ ] 7+.2.6 Git 提交：`feat(phase7+): 接口边界审计通过 + WebSocket 独立进程`

**如不拆分 websocket**：在 `community-server` 中通过 `@Profile("local")` 控制 Netty 随主进程启动（默认行为），生产环境如需独立部署则激活 `@Profile("prod")`。

---

### Phase 8: Spring Cloud 微服务拆分

**目标**：单体 → 5 个微服务 + Gateway + Nacos。功能不变，架构升级。

> 详细设计见 [04-migration.md](./04-migration.md)

#### 8.1 Nacos 基础
- [ ] 8.1.1 docker-compose 添加 Nacos 容器（端口 8848）
- [ ] 8.1.2 每个服务添加 `spring-cloud-starter-alibaba-nacos-discovery` 依赖
- [ ] 8.1.3 验证：所有服务注册到 Nacos 控制台

#### 8.2 Gateway
- [ ] 8.2.1 创建 `gateway-server` 模块（spring-cloud-starter-gateway）
- [ ] 8.2.2 路由规则：/api/v1/auth/** → user-service, /api/v1/servers/** → server-service, ...
- [ ] 8.2.3 JWT 全局过滤器：从 Header 提取 Token → Feign 调 user-service 验证
- [ ] 8.2.4 CORS 配置

#### 8.3 服务拆分
- [ ] 8.3.1 提取 `user-service`（独立启动类 + 独立端口 8081）
- [ ] 8.3.2 提取 `server-service`（独立启动类 + 独立端口 8082）
- [ ] 8.3.3 提取 `message-service`（独立启动类 + 独立端口 8083）
- [ ] 8.3.4 提取 `file-service`（独立启动类 + 独立端口 8084）
- [ ] 8.3.5 提取 `websocket-server`（独立启动类，8091 WS + 8092 health）

#### 8.4 Feign 跨服务调用
- [ ] 8.4.1 `UserServiceClient` — user-service 暴露的 Feign 接口
- [ ] 8.4.2 `ServerServiceClient` — server-service 暴露的 /internal/permission/check
- [ ] 8.4.3 替换 message-service 中所有直接调用为 Feign 调用
- [ ] 8.4.4 熔断降级（Resilience4j fallback）

#### 8.5 验证
- [ ] 8.5.1 Docker Compose 编排全部 6 个服务（5 业务 + Gateway）
- [ ] 8.5.2 Phase 7 冒烟测试脚本仍然全部通过
- [ ] 8.5.3 Nacos 控制台显示全部服务在线
- [ ] 8.5.4 Gateway 路由分发正确（直接调 Gateway 端口而非各服务端口）
- [ ] 8.5.5 Git 提交：`feat(phase8): Spring Cloud 微服务拆分 — Nacos + Gateway + Feign`
