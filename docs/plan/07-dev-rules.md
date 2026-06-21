# 十一、开发规范

## Git 提交粒度

每个 Phase 内，每完成一个子步骤组（如 3.1 所有 DDL、3.2 所有 Entity+Mapper+Dao）编译通过后提交一次。

提交信息格式：
```
feat(phaseN): <简短描述>

- 具体变更 1
- 具体变更 2
```

## 每日开发流程

```
1. 读 docs/plan/README.md → 确认当前 Phase 进度
2. 执行下一步 checkbox [ ]
3. ./mvnw clean compile — 通过才继续
4. 标记 [x]   → 继续下一步，或提交
```

## 编译验证命令

```bash
# Phase 0-7: 单体编译
./mvnw clean compile -pl community-server -am

# 完整打包
./mvnw clean package -DskipTests

# Phase 8: 全模块编译
./mvnw clean compile

# 基础设施
docker compose up -d
docker compose ps          # 确认全部 healthy
```

## 关键约束

- Phase 0-7 代码在 `community-server` 单模块内（按未来服务边界分包）
- Phase 8 之前不动 Spring Cloud 依赖（只声明 BOM，不引入组件）
- 每个 Phase 结束必须编译通过 + 至少一个 curl 可验证的新功能
- MallChat 代码只参考不复制，但工具类（JwtUtils/CursorUtils/RequestHolder）可直接移植
