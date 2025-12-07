# Repository Guidelines

## 项目结构与模块划分
- 单体内的模块化：`company-platform` 提供脚手架（`platform-common`/`web`/`jpa`/`security`/`ai`），`user-center` 承载业务域（`uc-api`、`uc-domain-tenant`、`uc-domain-identity`、`uc-domain-access`、`uc-domain-auth`、`uc-start`）。
- 设计/路线图分别位于 `info.md` 与 `plan.md`；数据库迁移脚本在 `user-center/uc-start/src/main/resources/db/migration`。

## 构建、测试与本地运行
- 前置：JDK 17、Maven 3.9+、本机 Docker（用于 Testcontainers），PostgreSQL/Redis 配置参考 `.env.example`（默认 PG 端口 5433，Redis 端口 6379）。
- 全量构建与测试：`mvn clean verify`。仅跑启动模块集成测试：`mvn test -pl user-center/uc-start -am`（会自动拉起 Postgres Testcontainer）。
- 本地启动：`mvn -pl user-center/uc-start spring-boot:run -Dspring-boot.run.profiles=local`，需确保数据库可达；快速迭代可加 `-DskipTests`。

## 代码风格与命名
- Java 4 空格缩进，UTF-8。包名小写（平台 `com.company.platform.*`，业务 `com.company.usercenter.*`）；类 PascalCase，方法/字段 camelCase，常量 UPPER_SNAKE_CASE。
- 领域实体仅承载领域属性，入参/出参使用 DTO/record；工程内注释统一使用中文，保持简短清晰。

## 测试规范（TDD）
- 框架：JUnit 5 + Spring Boot Test，集成测试使用 Testcontainers PostgreSQL，覆盖租户头缺失/隔离等负例。
- 命名：单测 `*Test`，集成测 `*IT`，与模块同路径存放；提交前至少运行 `mvn test -pl user-center/uc-start -am`。

## 提交与 PR 要求
- 提交遵循 Conventional Commits（如 `feat: add tenant aggregate`、`fix: tenant header validation`），保持粒度可回滚。
- PR 需说明变更范围、关联 issue、数据库迁移影响、已执行的测试命令及结果；涉及接口请附请求/响应示例或 curl。

## 缺陷记录
- 所有阶段遗留问题需记录在 `buglist.md`，按“第X阶段待修复问题：”分段；后续 review/验收发现的新问题也要追加到对应阶段段落。

## 安全与配置提示
- 机密信息放环境变量或被忽略的本地文件，不要入库；数据库账号按最小权限配置。
- `/api/**` 默认要求 `X-Tenant-Id` 头（`/api/tenants` 和 `/actuator` 例外），缺失会返回 400；无鉴权逻辑前，测试阶段默认放行，但后续请补齐权限控制。
- Redis 用于验证码、短期 state、会话黑名单及热点缓存，连接信息见 `.env.example`；若停用 Redis，上述功能需退化到数据库或本地缓存。
