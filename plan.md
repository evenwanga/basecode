我已经仔细分析了你提供的 `info.md` 文档，并结合你刚才提出的 **5 点核心约束**（JDK 17、Maven、PgSQL、Redis、Spring Boot 4），为你制定了以下可落地的开发计划。

---

### 最新执行进展（截至当前）

- 已完成：去除 Hibernate FilterDef 导致的 ASM 解析异常，改为仓储按租户 ID 查询；多租户线程上下文通过 `TenantHeaderFilter` 保障。
- 已完成：Auth 占位安全链改为放行全部请求（后续再细分权限），`mvn test -pl user-center/uc-start -am` 集成测试通过，覆盖租户头缺失返回 400 与租户隔离校验。
- 已完成：`mvn clean verify` 全量构建通过；`AGENTS.md` 改为中文贡献指南，明确本地 DB/Testcontainers 配置与开发规范。
- 已完成：架构调整为使用 Redis 承载验证码/短期状态/缓存，配置写入 `.env.example`，文档同步。
- 第三阶段：完成 Spring Authorization Server 接入（JDBC 授权/同意服务、双安全链、JWK 生成）、领域用户登录接入、默认客户端初始化，新增 OAuth2 表迁移；单元测试补充并通过。待处理：JWK 未持久化（重启令牌失效）已录入 buglist。

### 阶段进度对照

- **第一阶段（脚手架与基础设施初始化）**：已完成骨架、模块依赖对齐、基础配置（全局异常、JPA 审计、密码编码器、多租户头过滤器）、Auth 占位、Flyway 基线及本地专用 PG 实例。
- **第二阶段（核心领域模型实现）**：已完成租户/组织、用户/身份、成员、角色/权限实体与仓储，基础服务与 API（租户创建、组织查询、用户注册、身份查询、成员绑定），以及数据库迁移与多租户头必填集成测试。

-----

### 1\. 项目核心技术栈与约束确认

我们将严格遵循以下基线进行工程搭建：

  * **开发语言**：Java 17 (LTS) —— *放弃 Java 21 的虚拟线程，使用标准的线程模型。*
  * **核心框架**：Spring Boot 4.0.0 (基于 Spring Framework 7 & Jakarta EE 11) —— *注：我们将重点使用文档中提到的 `@HttpExchange`、Observability 等新特性。*
  * **构建工具**：Maven 3.9+ —— *使用 Multi-module 结构管理依赖。*
  * **数据库**：PostgreSQL 15+ —— *作为主存储。*
  * **缓存/会话**：Redis 7+（本机 Docker 配置写在 `.env.example`），用于验证码、短期 state、会话黑名单/缓存；持久化数据仍落地 PostgreSQL。
  * **架构风格**：模块化单体 (Modular Monolith) + DDD (领域驱动设计)。

-----

### 2\. 工程架构设计 (Maven 多模块)

我们将项目拆分为 **基础脚手架 (`company-platform`)** 和 **业务应用 (`user-center`)** 两大部分，全部在一个 Maven Reactor 中管理（或分为两个独立 Repo，建议初期在一个 Repo 中以便调试）。

**目录结构预览：**

```text
user-center-monolith/
├── pom.xml (Root POM: 定义 Spring Boot 4 Parent, Java 17, 全局版本)
├── company-platform/                 # [脚手架] 通用能力沉淀
│   ├── pom.xml
│   ├── platform-dependencies/        # BOM (统一管理依赖版本)
│   ├── platform-common/              # 工具类, 异常定义, Result封装
│   ├── platform-web/                 # 全局异常处理, Jackson配置, WebMvc配置
│   ├── platform-jpa/                 # JPA配置, 多租户拦截器, 审计字段
│   ├── platform-security/            # Security基础链, 密码加密, JWT工具
│   ├── platform-ai/                  # MCP集成与AI工具
│   └── platform-test/                # 单元测试与集成测试基类 (Testcontainers)
├── user-center/                      # [业务域] 模块化单体
│   ├── pom.xml
│   ├── uc-api/                       # 对外暴露的 DTO 和 Feign/HttpExchange 接口
│   ├── uc-domain-tenant/             # 租户 & 组织域 (Domain + Infrastructure)
│   ├── uc-domain-identity/           # 用户 & 身份域 (User, Identity, Membership)
│   ├── uc-domain-access/             # 权限域 (RBAC, Role, Permission)
│   ├── uc-domain-auth/               # 认证域 (OAuth2 Server, Session管理 - 纯DB实现)
│   └── uc-start/                     # 启动模块 (Application, API Controller, 配置聚合)
```

-----

### 3\. 详细实施路线图 (Roadmap)

我们将分 5 个阶段完成开发。

#### 第一阶段：脚手架与基础设施初始化 (Platform Foundation)

**目标**：搭建 Maven 骨架，统一规范，打通数据库连接。

1.  **Maven配置**：
      * 创建父工程 `pom.xml`，锁定 JDK 17，引入 `spring-boot-starter-parent` (4.0.0)。
      * 配置 Checkstyle 和 Spotbugs 插件。
2.  **Platform Core**：
      * `platform-common`：定义 `BaseEntity` (id, created\_at, updated\_at)，`Result<T>` 统一响应体。
      * `platform-jpa`：集成 `spring-boot-starter-data-jpa` + `postgresql`。
      * **关键点**：实现多租户隔离（TenantContext + SQL 过滤），Redis 仅作缓存/状态，不承载主数据。
3.  **Platform Web**：
      * 实现 `GlobalExceptionHandler`。
      * 配置 Swagger/OpenAPI (SpringDoc)。

#### 第二阶段：核心领域模型实现 (Domain Implementation)

**目标**：完成 Tenant, Organization, Identity 三大核心域的 CRUD。

1.  **Tenant 域** (`uc-domain-tenant`)：
      * 实体：`Tenant`, `OrganizationUnit`。
      * 功能：租户创建、组织树管理。
2.  **Identity 域** (`uc-domain-identity`)：
      * 实体：`User`, `UserIdentity` (账号/手机/邮箱), `Membership` (关联 Tenant)。
      * **Redis 策略**：验证码（OTP）存 Redis（键包含 receiver+type，TTL 控制），同时可选落库审计；获取验证码前清理旧键。
3.  **Access 域** (`uc-domain-access`)：
      * 实体：`Role`, `Permission`。
      * 功能：简单的 RBAC 授权逻辑。

#### 第三阶段：认证与授权服务 (Auth & Security)

**目标**：实现 OIDC/OAuth2.1 能力，替代 Redis 进行会话管理。当前进度：已实现，待补 JWK 持久化。

1.  **Spring Authorization Server 集成**：
      * 在 `uc-domain-auth` 中引入 `spring-boot-starter-oauth2-authorization-server`。
      * **配置**：JDBC RegisteredClient/Authorization/Consent 服务，JWK 生成、JWT 解码，双安全链（授权端点 + 应用接口）。
      * **状态**：已完成，JWK 目前内存生成，重启会使旧令牌失效（待持久化）。
      * Schema 初始化：新增迁移 `V4__oauth2_authorization_server.sql` 创建 `oauth2_*` 表。
2.  **登录流程**：
      * 实现 `DomainUserDetailsService` 对接 Identity 域本地密码身份。
      * 支持 `authorization_code`、`client_credentials`、`refresh_token`。
      * 默认客户端：启动时初始化 `user-center-web`（见 `DefaultOAuth2ClientInitializer`）。

#### 第四阶段：对外接口与应用层 (Application Layer)

**目标**：暴露 REST API，完成 Application 管理。

1.  **Client Application 管理**：
      * 仿照 Logto，提供 API 管理 `ClientApplication` (clientId, secret, redirectUris)。
      * 将注册的 Client 同步写入 `oauth2_registered_client` 表。
2.  **User Center API**：
      * `/api/v1/users/me` (获取当前用户信息)。
      * `/api/v1/tenants` (切换租户)。
3.  **Http Interfaces**：
      * 使用 Spring Boot 4 的 `@HttpExchange` 定义模块间（如果未来拆微服务）或对外的声明式客户端。

#### 第五阶段：TDD 流程与 AI 集成

**目标**：建立质量保障体系。

1.  **测试策略**：
      * 使用 JUnit 5 + Mockito。
      * 使用 **Testcontainers (PostgreSQL)** 进行 Repository 和 Controller 的集成测试（替代 H2，确保与生产环境数据库行为一致）。
2.  **AI/MCP 集成**：
      * 按照文档要求，创建 `AGENTS.md` 和 `docs/spec/*.md`。
      * 编写 MCP Server 工具类（可选），用于让 AI 读取项目结构。

-----

### 4\. 关键技术点解答 (Q\&A)

**Q: Redis 的作用与本地配置？**

  * **A**: Redis 用于验证码、短期 state/nonce、会话黑名单及热点查询缓存；主数据（用户、租户、令牌持久化）仍在 PostgreSQL 中。`.env.example` 已提供本机 Redis 配置（默认端口 6379，可通过 Docker 启动），开发/测试按需启停。
  * **优化策略**：对热点读（Tenant、Role 定义等）可引入 Spring Cache（Redis backed 或 Caffeine 进程内）并设置合理 TTL；仍需保证数据库索引优化。

**Q: Spring Boot 4 的兼容性？**

  * **A**: 虽然 Spring Boot 4 是前沿版本，但我们将主要依赖 Spring Framework 6.1+/7 的标准 API。如果 4.0.0 的某些 Starter 尚未发布，我们将回退到 Spring Boot 3.2+ (它是 4.0 的基础)，但代码风格（如 Records, HTTP Interfaces）保持最新，确保无缝升级。

-----
