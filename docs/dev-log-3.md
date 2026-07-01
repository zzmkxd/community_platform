# 社群平台 开发日志 (3)

> **续篇** — 前篇 `dev-log-2.md` 覆盖 Phase 5 ~ P2-2 审计修复。
> 本文档记录代码质量扫描发现的 Bug 及后续工作。

## 项目信息

- **项目名称**: community-platform (社群平台)
- **当前状态**: P0-P2 审计全部完成，冒烟测试 17/17 通过
- **最新提交**: `4d155bc` — docs: 同步审计状态
- **日期**: 2026-06-26

---

## 代码质量扫描 (2026-06-26)

P0-P2 审计主要覆盖安全/性能/异常处理。本轮扫描侧重**业务逻辑正确性**——竞态条件、边界条件、权限校验完整性。扫描范围 232 个 Java 文件。

### 结论：9 项确认，其中 6 项影响功能运行

---

## Bug 清单

### 高严重度（权限/业务逻辑错误）

#### Bug #1 — kickOut 可踢出 Server Owner

**位置**: `MemberServiceImpl.java:144-152`

**根因**: `leaveOrKick()` 中 leave 路径（line 135-138）检查了 `server.getOwnerId().equals(uid)` 阻止 Owner 自行离开，但 kick 路径（line 144-152）只检查 `KICK_MEMBERS` 权限，没有判断目标 `userId` 是否为 Owner。

**影响**: 拥有 KICK_MEMBERS 权限的 admin 可以把服务器创建者踢出去。

**验证方法**: 代码审查。`leaveOrKick(serverId, userId)` 当 `uid != userId` 时走 line 144 else 分支，line 145-146 仅校验 `KICK_MEMBERS` 权限位，之后直接 `setStatus(KICKED)`。整段无 `server.getOwnerId().equals(userId)` 判断。

---

#### Bug #2 — transferOwnership 不转移 Owner 角色

**位置**: `MemberServiceImpl.java:193-220`

**根因**: `transferOwnership()` 只执行了 `server.setOwnerId(newOwnerId)` 更新 Server 表的 owner_id 字段。没有：
- 删除旧 Owner 的 Owner 角色（ADMINISTRATOR 权限位，position=999）
- 为新 Owner 分配 Owner 角色

权限检查链路 `PermissionServiceImpl.checkPermission()` (line 64-66) 完全依赖角色位图 `basePermissions & PermissionBit.ADMINISTRATOR.getBit()`，不直接检查 `server.ownerId`。

**影响**: 新 owner 名义上拥有服务器（server 表 owner_id 已变更），但没有 ADMINISTRATOR 权限位，无法管理角色/频道/成员。旧 owner 反而保留了 Owner 角色和管理权限。

**验证方法**: 代码审查。`transferOwnership()` 方法体共 28 行，唯一的数据库写操作是 line 218 `serverDao.updateById(server)` 更新 ownerId。未出现 RoleDao/MemberRoleDao 的任何调用。

---

### 中严重度（数据正确性）

#### Bug #3 — 已是成员仍消耗邀请次数

**位置**: `InviteServiceImpl.java:108-136`

**根因**: `joinByInvite()` 中：
1. Line 108: `if (existing == null)` → 新成员，插入记录
2. Line 126: `else if (status != ACTIVE)` → 重新激活旧成员
3. 若用户已是活跃成员，两个分支均不命中，但 line 132 `usedCount++` 照常执行

**影响**: 活跃成员重复使用邀请码（如误点链接），白白消耗邀请次数。若 maxUses=1，首个成员加入后任何后续误操作都会导致邀请失效。

**验证方法**: 代码审查。line 132-136 在 if/else if 块之后独立执行，无 `else { return; }` 跳过逻辑。

---

#### Bug #4 — 邀请 usedCount 竞态条件

**位置**: `InviteServiceImpl.java:92-94 + 132-136`

**根因**: `usedCount` 的读取-判断-递增-写回不是原子操作：
1. Line 92: `invite.getUsedCount() >= invite.getMaxUses()` — 读取判断
2. Line 132: `invite.setUsedCount(invite.getUsedCount() + 1)` — 读取-修改
3. Line 136: `inviteDao.updateById(invite)` — 写回

`@Transactional` 默认隔离级别 REPEATABLE_READ 无法阻止两个并发事务同时读到相同的 usedCount、同时通过检查、同时写入。

**影响**: 并发加群可突破 `maxUses` 限制。例如 maxUses=1 的邀请，两个用户同时请求可能都成功加入。

**修复方向**: 使用原子 UPDATE `UPDATE invite SET used_count = used_count + 1 WHERE id = ? AND used_count < max_uses`，或 SELECT FOR UPDATE 行锁。

**验证方法**: 代码审查。无 `SELECT ... FOR UPDATE`、无原子 `UPDATE ... WHERE used_count < max_uses`、无 `@Version` 乐观锁。

---

#### Bug #5 — Thread messageCount 永远是 0

**位置**: `ThreadServiceImpl.java:71`

**根因**: 创建 Thread 时 `thread.setMessageCount(0)`，之后任何消息发送流程（`MessageServiceImpl.sendMessage()` / `AbstractMsgHandler.checkAndSaveMsg()`）都没有递增对应 Thread 的 messageCount。数据库 `thread.message_count` 列存在但从未被 UPDATE。

**影响**: API 返回的话题列表/详情中 `messageCount` 始终为 0，前端无法展示话题实际消息数。

**验证方法**: Grep 全量代码 `setMessageCount` 仅出现在 `ThreadServiceImpl.java:71`（初始化为 0）和 `toThreadVO()`（映射 VO），无递增调用。

---

#### Bug #6 — 创建 Thread 不校验消息存在

**位置**: `ThreadServiceImpl.java:67`

**根因**: `createThread()` 接收 `rootMsgId` 参数直接 `thread.setRootMsgId(rootMsgId)`，不查询 message 表验证该消息是否存在、是否未删除、是否属于同一 channel。

**影响**: 可创建指向不存在或已删除消息的 Thread。后续 getThread 展示此 rootMsgId 时前端可能渲染死链。

**验证方法**: 代码审查。`createThread()` 方法体中无任何 `messageDao` 查询调用（line 44 注入了 messageDao 但未用于 rootMsgId 校验）。

---

### 低严重度（P2-2 漏网）

#### Bug #7-9 — 三处魔法数字漏改

| # | 位置 | 代码 | 应为 |
|---|------|------|------|
| 7 | `MemberServiceImpl.java:77` | `.eq(Server::getStatus, 1)` | `ServerStatusEnum.NORMAL.getStatus()` |
| 8 | `MemberServiceImpl.java:199` | `.eq(Server::getStatus, 1)` | `ServerStatusEnum.NORMAL.getStatus()` |
| 9 | `ThreadServiceImpl.java:120` | `.ne(Message::getStatus, 1)` | `MessageStatusEnum.DELETED.getStatus()` |

**根因**: P2-2 枚举替换时 `MemberServiceImpl.java` 和 `ThreadServiceImpl.java` 未在变更范围内（这两个文件不属于 P2-2 计划中列出的文件）。

---

## 变更记录

| 日期 | 内容 | 提交 |
|------|------|------|
| 2026-06-26 | 代码质量扫描: 9 项 Bug 确认并记录 | 本文档 |

---

## 参考

- 前篇开发日志: `docs/dev-log-2.md`
- P2-2 审计计划: `C:\Users\Ourten\.claude\plans\melodic-purring-phoenix.md` (已删除, 已完成)
