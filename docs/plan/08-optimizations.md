# 八、性能优化方案 (参考，Phase 5+ 可选)

> 当前实现方案足以支撑课程演示。以下为后续可选的优化方向记录。

---

## 8.1 Token 鉴权优化

### 现状

单 Token 模式：JWT 无过期时间（仅做 uid 载体），Redis key `token:{uid}` 存 JWT 值，5 天 TTL。每次请求查 Redis 校验 Token 值匹配。

### 方案 A：使用时自动续期 (renewalTokenIfNecessary)

MallChat plan Phase 2.5.1 提及的模式：Token 接近 TTL 阈值（如剩余 < 1 天）时，自动签发新 Token 并在响应头返回。

| 优点 | 缺点 |
|------|------|
| 改动最小（拦截器内加判断） | 只是续期，不解决 Redis 查询开销 |
| 前端无感知 | 旧 Token 仍在 Redis 存在直到 TTL 过期 |

### 方案 B：双 Token (access + refresh)

经典的 OAuth2 简化版：

| | access token | refresh token |
|------|-------------|---------------|
| TTL | 15 分钟 | 7 天 |
| 存储 | **无状态**（仅 JWT 签名验证，不查 Redis） | Redis `refresh:{uid}` |
| 用途 | 每次 API 请求携带 | 仅 `/auth/refresh` 换取新 access token |
| 泄露影响 | 15 分钟内自动过期 | 需 Redis 撤销 |

**流程**：
```
登录 → 返回 { accessToken (15min), refreshToken (7d) }
API 请求 → 带 accessToken → TokenInterceptor 仅 JWT 验签（不查 Redis）
accessToken 过期 → POST /auth/refresh { refreshToken } → 返回新 accessToken
refreshToken 过期 → 重新登录
```

**优点**：高频 API 请求不再每次查 Redis，性能提升显著
**缺点**：前端需处理 Token 刷新逻辑（拦截 401 → refresh → 重试原请求）

### 建议

- 课程演示：**当前单 Token 方案已足够**，不查 Redis 不会成为瓶颈
- 如果后续做性能压测：优先方案 B，改动点清晰（TokenInterceptor 分流 + AuthController 新增 refresh 端点 + RedisUtils 新增 Lua 脚本原子替换）

---

## 8.2 refreshToken 竞态条件（已修复）

原实现：
```java
RedisUtils.del(key);     // 删除旧 Token
String newToken = ...;
RedisUtils.set(key, newToken, ttl);  // 写入新 Token
```

DEL 和 SET 之间存在窗口期，并发请求会看到空值 → 被拒绝。

**修复**：直接 SET 覆盖（Redis SET 本身是原子操作），无需 DEL。旧 Token 被新值覆盖后，后续持有旧 Token 的请求在 `getValidUid()` 的 `token.equals(stored)` 校验中自然失败。

---

## 8.3 getValidUid Token 值比对（已修复）

原实现：
```java
return stored != null ? uid : null;  // 只判 key 存在
```

问题：设备 B 登录后 SET 新 Token，设备 A 的旧 Token 仍可通过（Redis key 存在即为有效）。

**修复**：
```java
return token.equals(stored) ? uid : null;  // 比对 Token 值
```

新登录会覆盖 Redis 中的 Token 值 → 旧 Token 立即失效 → 真正的单设备登录。
