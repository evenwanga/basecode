# Repository Guidelines（中文版）

## 项目结构与模块划分
- 模块化单体：**company-platform**（通用脚手架）与 **user-center**（业务域）。子模块含 `platform-dependencies`、`platform-common`、`platform-web`、`platform-jpa`、`platform-security`、`platform-ai`、`platform-test`，以及 `uc-api`、`uc-domain-tenant`、`uc-domain-identity`、`uc-domain-access`、`uc-domain-auth`、`uc-start`。
- 按 DDD 划分领域：Tenant、Organization、Identity、Membership、Access Control、Application、AuthSession、Audit。保持聚合边界清晰，通过 `uc-api` 提供 DTO 和接口。
- 持久化主存储为 PostgreSQL；Redis 用于验证码/状态/缓存（配置见 `.env.example`）。设计说明放 `info.md`，路线更新放 `plan.md`。

## 构建、测试与开发命令
- 前置：Java 17、Maven 3.9+、PostgreSQL 15+ 与 Redis 7+（本地示例配置见 `.env.example`），Testcontainers 需本机 Docker。
- 全量构建与测试：`mvn clean verify`（含单测/集成测与 Checkstyle/SpotBugs，如在父 POM 配置）。
- 本地运行：`mvn -pl uc-start spring-boot:run -Dspring-boot.run.profiles=local`（需要本地数据库与样例数据；快迭代可加 `-DskipTests`）。

## 代码风格与命名
- Java：4 空格缩进、UTF-8、去除行尾空格。包名小写：`com.company.platform.*`（平台），`com.company.usercenter.*`（业务）。类 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE_CASE`。
- 请求/响应建议用 record 或轻量 DTO；领域实体不夹杂传输字段。
- 提交前跑格式化（若已配置）如 `mvn -pl platform-common,uc-start -DskipTests fmt:format`，并遵循父级 Checkstyle 规则。

## 测试准则
- 单测用 JUnit 5 + Mockito；集成测试用 Testcontainers PostgreSQL 对齐生产行为。
- 命名：单元测试 `*Test`，集成测试 `*IT`；与模块同路径放置（如 `uc-domain-tenant/src/test/java`）。
- 覆盖失败路径（权限/校验）与领域事件断言；仅在 CI 明确支持时才用 `-DskipITs=true` 跳过集成测。

## 提交与 PR 规范
- 采用 Conventional Commits：`feat: add tenant aggregate`、`fix: prevent duplicate membership`、`chore: bump spring boot`。保持提交小而可回滚。
- PR 需写清范围、关联 issue、数据库变更、测试命令/结果，以及 API 变更示例（请求/响应）和用户可见行为的截图或 curl 示例。

## 安全与配置提示
- 密钥不入库；使用环境变量或被 .gitignore 的 `.env.local` 存放数据库与 OAuth 机密。
- 数据库账户最小权限；身份域使用强哈希（BCrypt/Argon2）。本地使用 `SPRING_PROFILES_ACTIVE=local`，默认不开启生产专用的指标导出或远程 tracing。
- Redis 默认端口 6379，凭据见 `.env.example`，用于验证码、短期 state 与缓存；若无需可在本地停用但相关功能将退化。
