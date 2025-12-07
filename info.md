把 **user_center** 做成一个 **DDD 风格的模块化单体**，基于 **Spring Boot 4 + Java 21**，同时在这个项目里沉淀一套公司的 **统一 Java 脚手架（company-platform）**。
在领域设计上参考 Logto 的理念（Application、Tenant/Organization、User/Identity 等），但功能只取核心 20%，把复杂的 Connector、细粒度策略等放到后续版本。

下面我分 8 部分讲清楚：领域划分 → 架构 → 工程脚手架 → 认证设计 → TDD SOP → AI/MCP 集成 → 实施路线图。

---

## 1. 总体目标 & 设计原则

**目标**

* 建一个面向全公司的“用户中心”：

  * 统一的 **租户（Tenant）**、**组织（Organization）**、**用户**、**登录 / SSO** 管理
  * 为所有内部/外部系统提供 **OIDC/OAuth2.1 IdP** 能力（access token、id token、refresh token 等）([docs.logto.io][1])
* 在这个项目中落地一套 **新的 Java 开发框架**：

  * DDD 分层 + TDD 开发流程
  * 标准化依赖、监控、安全、中间件接入
  * 内建 AI/MCP 能力，方便后续项目“AI 原生开发”

**关键技术基线**

* Spring Boot 4.0.x（基于 Spring Framework 7 & Jakarta EE 11）([GitHub][2])
* JDK：**最少 17，推荐 21（支持虚拟线程）**([Home][3])
* Servlet 6.1（Tomcat 11 / Jetty 12）([spring.pleiades.io][4])
* Build：Gradle 8.14+/9（官方已支持 Gradle 9）([GitHub][5])

**只参考，不照抄 Logto 的地方**

从 Logto 借鉴的概念：

* Application（客户端应用）及类型：SPA、Native、Web、M2M；clientId、secret、redirectUris 等([docs.logto.io][1])
* User 数据分层：基础信息 + 社交身份 + 自定义字段([docs.logto.io][6])
* Tenant / Organization 区分：Tenant 为物理隔离单元，Organization 为一个 Tenant 内的用户组织与上下文([docs.logto.io][7])

刻意 **暂不做**（防止过度设计）：

* 大量第三方 Connector（微信/支付宝/GitHub/…）的可视化配置
* SAML IdP/SP、复杂企业 SSO 变体
* 组织模板、可视化策略编辑器、高度动态的 RBAC 模板([docs.logto.io][8])
* 完整 Logto Console 那种大而全的 UI

---

## 2. DDD 领域划分 & 核心模型

### 2.1 顶层领域（Bounded Context）

1. **Tenant 域（租户域）**

   * 负责：租户生命周期、隔离策略、配额与登录策略
   * 核心聚合：

     * `Tenant`：id、code、name、type（INTERNAL / CUSTOMER / PARTNER）、status、settings（登录策略、密码策略、MFA 要求等）
   * 事件：

     * `TenantCreated`, `TenantSettingsChanged`, `TenantSuspended`, `TenantDeleted`

2. **Organization 域（组织域）**

   * 负责：每个 Tenant 下组织树（部门 / 团队）
   * 聚合：

     * `OrganizationUnit`：id、tenantId、parentId、name、path、level、status
   * 事件：

     * `OrganizationUnitCreated`, `OrganizationUnitMoved`, `OrganizationUnitRemoved`

3. **Identity 域（身份域）**

   * 负责：**用户本体 + 身份信息 + 凭证**
   * 聚合：

     * `User`：全局用户 id、displayName、primaryEmail、primaryPhone、status、customData（JSON）
     * `UserIdentity`（实体/值对象）：

       * type：LOCAL_PASSWORD / PHONE_OTP / EMAIL_OTP / EXTERNAL_OIDC / WECHAT …
       * identifier：用户名 / 手机 / 邮箱 / 外部 subject
       * secret：密码哈希或外部引用
   * 事件：

     * `UserRegistered`, `UserVerified`, `UserDisabled`, `CredentialsChanged`

4. **Membership 域（成员域）**

   * 负责：用户在不同租户/组织中的成员关系
   * 聚合：

     * `Membership`：membershipId、userId、tenantId、orgUnitId、roles（在该上下文下的角色）、status
   * 事件：

     * `MembershipCreated`, `MembershipRoleChanged`, `MembershipDisabled`

5. **Access Control 域（访问控制域）**

   * 负责：角色、权限、资源
   * 聚合：

     * `Permission`：code（如 `user:read`）、description
     * `Role`：roleId、scope（GLOBAL/TENANT/ORG/APP）、permissions[]
     * `ApiResource`：标识某个服务/作用域
   * 事件：

     * `RoleCreated`, `RoleUpdated`, `RoleDeleted`, `PermissionAssigned`

6. **Application 域（客户端应用域）**

   * 负责：对接的业务系统（类似 Logto Application）([docs.logto.io][1])
   * 聚合：

     * `ClientApplication`：

       * clientId、type（SPA / NATIVE / WEB / M2M）
       * redirectUris、postLogoutRedirectUris
       * corsAllowedOrigins
       * clientSecret（仅私密客户端）
       * 授权类型、scope、tokenSettings（TTL 等）

7. **AuthSession 域（认证会话域）**

   * 负责：登录会话、token、刷新 token
   * 聚合：

     * `AuthSession`：sessionId、userId、tenantId、clientId、createdAt、lastSeenAt、expiresAt、ip、userAgent
     * `RefreshToken`（实体）：tokenId、sessionId、expiresAt、revoked
   * 事件：

     * `UserLoggedIn`, `UserLoginFailed`, `UserLoggedOut`, `RefreshTokenRotated`

8. **Audit 域（审计域）**

   * 负责：安全与操作审计
   * 聚合：

     * `AuditLog`：eventType、subject（User/Tenant/Org）、actor（userId/clientId）、payload、time

### 2.2 领域事件关系（简化）

* `TenantCreated` → 触发：

  * Organization 域：创建默认 `Root` 组织
  * Access 域：为该租户初始化默认角色模板
* `UserRegistered` → 触发：

  * Membership 域：在“平台租户”生成默认会员关系
  * Audit 域：记录注册事件
* `UserLoggedIn` → 触发：

  * AuthSession 域：创建会话与 token
  * Audit 域：记录登录来源、IP、设备
* `MembershipRoleChanged` → 影响：

  * token 中的权限声明，下次发 token 时刷新 claims

技术上，领域事件可以先用 **Spring ApplicationEvent + 事务内事件分派**，后面再演进到 Kafka/RabbitMQ 这种跨服务事件总线。

---

## 3. 系统架构 & Spring Boot 4 技术栈

### 3.1 部署架构：模块化单体（Modular Monolith）

第一阶段建议：

* 单一 Spring Boot 4 应用进程，内部用模块划分领域（多 module + 清晰包结构）
* 对外暴露：

  * RESTful 管理 API（给 admin/运营后台）
  * OAuth2/OIDC 标准端点（/authorize, /token, /userinfo, /.well-known/openid-configuration 等）([docs.logto.io][1])
* 对内通过网关 / Service Mesh 访问

搭配组件：

* DB：PostgreSQL（推荐），使用 UUID/ULID 主键
* Cache：Redis（session blacklist、验证码、短期 state 等）
* 消息：Kafka/RabbitMQ（后续做事件驱动和审计异步化）
* Observability：OpenTelemetry + Prometheus + Loki/Jaeger（Spring Boot 4 新的 OTel starter 可以直接用）([GitHub][5])

### 3.2 Spring Boot 4 的重点使用点

利用一些 4.0 的新特性，为“脚手架化”服务：([GitHub][5])

* **HTTP Service Clients**

  * 用 `@HttpExchange` 定义内部/外部 HTTP 客户端接口，减少 Feign/手写 RestTemplate ([GitHub][5])
* **API Versioning**

  * 用官方 API Versioning 自动配置，统一版本策略（Header / URL / MediaType）([GitHub][5])
* **OpenTelemetry Starter**

  * 引入 `spring-boot-starter-opentelemetry`，标准化 trace/metrics 输出
* **RestTestClient**

  * 测试阶段使用新的 `RestTestClient`，写端到端 HTTP 测试，比 `MockMvc` 更统一([GitHub][5])
* **配置元数据跨模块**

  * 使用 `@ConfigurationPropertiesSource` 让不同 module 的配置也能自动生成 IDE 提示，为公司级 starter 打基础([GitHub][5])

---

## 4. 工程 & 脚手架设计（company-platform + user_center）

### 4.1 仓库整体结构（建议范例）

```text
user_center/
  settings.gradle.kts
  build.gradle.kts              // 根配置：Java 21, Spring Boot 4, 代码规范等

  platform/                     // 通用脚手架
    platform-bom/               // 统一版本管理（Spring, DB, Test, Observability）
    platform-domain/            // DDD 基础：AggregateRoot, Entity, DomainEvent, Repository 抽象
    platform-web/               // Web 通用：异常处理、统一返回体、API 版本、全局日志
    platform-data-jpa/          // JPA/Hibernate 配置、多租户过滤、审计字段
    platform-security/          // Spring Security 通用配置（JWT 解析、租户解析、角色映射）
    platform-test/              // 测试基类、RestTestClient 封装、Testcontainers 支持
    platform-ai/                // 与 MCP/AI 的集成工具（见第 7 部分）

  services/
    user-center-app/            // Boot 主应用，仅聚合其他模块
      src/main/java/...         // Application 层、启动类、API 网关层
    user-center-tenant/         // Tenant 领域模块
    user-center-organization/   // Organization 领域模块
    user-center-identity/       // Identity + Membership
    user-center-access/         // AccessControl 域
    user-center-auth/           // OAuth2/OIDC & Session 域
    user-center-audit/          // 审计域
```

后续新项目可以直接：

* 引入 `platform-bom`
* 依赖 `platform-web`, `platform-data-jpa`, `platform-security`, `platform-test`, `platform-ai`
* 再在 `services/xxx-service` 里按同样的 DDD 分层模式开发

### 4.2 模块内分层结构

以 `user-center-identity` 为例：

```text
com.company.usercenter.identity
  ├─ domain
  │   ├─ model            // 聚合根、实体、值对象
  │   ├─ event            // 领域事件
  │   ├─ repository       // 仓储接口
  │   └─ service          // 领域服务（复杂业务规则）
  ├─ application
  │   ├─ command          // Command DTO
  │   ├─ query            // Query DTO
  │   └─ service          // 应用服务（用例编排）
  ├─ infrastructure
  │   ├─ jpa              // JPA 实现、Entity 映射
  │   └─ messaging        // 事件发布适配
  └─ interfaces
      ├─ rest             // REST Controller
      └─ converter        // DTO ↔ Domain 转换
```

### 4.3 多租户策略

* 数据层：大多数表采用 **共享库 + tenant_id 列** 模式
* 身份层：

  * `User` 为全局实体，不带 tenant_id
  * `Membership` 挂 `tenantId`+`orgUnitId`，控制该用户在该租户的权限与组织
* 技术实现：

  * `platform-data-jpa` 提供 `TenantContext`，从 Security 的 `Authentication` 中解析当前 tenant
  * Hibernate 全局 Filter（或 `@Where`）自动追加 `tenant_id = :currentTenant`，避免业务代码到处手动加条件

---

## 5. 认证 / 登录设计（对标 Logto，适度收敛）

### 5.1 支持的协议与流程（Phase 1）

* 协议：**OAuth 2.1 + OpenID Connect Core**
* 授权模式：

  1. Authorization Code + PKCE（SPA、Native）
  2. Authorization Code（传统 Web）
  3. Client Credentials（M2M 服务间调用）
* 标准端点（示例）：

  * `GET /oauth2/authorize`
  * `POST /oauth2/token`
  * `GET /.well-known/openid-configuration`
  * `GET /oauth2/userinfo`
  * `POST /oauth2/introspect`
  * `POST /oauth2/revoke`
  * `GET /oauth2/logout`

实现上可以使用 **Spring Authorization Server 适配 Spring Boot 4/Spring Security 7**([GitHub][5])：

* 客户端信息持久化到 `ClientApplication` 表
* 用户认证通过 Identity 域（本地密码、短信、邮箱验证码等）
* Token 内 embed：

  * sub（userId）、tenantId、membershipId
  * roles、permissions（按租户/组织）

### 5.2 与 Logto 的“取舍”

从 Logto Application data structure 借鉴：Application type、redirect URIs、post logout URIs、CORS allowed origins、token 相关配置等([docs.logto.io][1])

但做如下简化：

* **只支持必要的 token 策略**

  * 不做“Always issue refresh token”、“复杂 refresh token rotation 策略配置”，先通过统一 TTL + 黑名单搞定([docs.logto.io][1])
* **暂不引入 SAML**

  * 未来如果公司有大量对接外部 IdP 再考虑
* **暂不做 Connector 市场**

  * 只抽象一个 `IdentityProvider` 接口，后期再实现具体微信/企业微信等

---

## 6. DDD + TDD 的开发 SOP（写进脚手架）

你希望统一研发方式，所以在 `platform-test` 和项目模板里可以约定一套 **TDD SOP**：

### 6.1 用例驱动（Use Case First）

每个需求故事都要先写一个 **用例说明**（可以存 `docs/use-cases/xxx.md`），结构类似：

* 场景：`注册用户并加入指定租户`
* Given：

  * 某租户已存在
  * email 未被占用
* When：

  * 调用 `RegisterUserAndJoinTenant` 命令
* Then：

  * 创建 User
  * 创建 Membership（默认角色为 MEMBER）
  * 发出 `UserRegistered` 与 `MembershipCreated` 事件

### 6.2 测试分三层

1. **纯领域测试（不启 Spring）**

   * 测试对象：`domain.model`、`domain.service`
   * 使用 JUnit 5 + AssertJ
   * 示例：

   ```java
   class RegisterUserDomainServiceTest {

       @Test
       void should_create_user_and_emit_event() {
           var command = new RegisterUserCommand("tenant-1", "foo@bar.com");

           var result = domainService.register(command);

           assertThat(result.user().getId()).isNotNull();
           assertThat(result.events())
               .extracting(DomainEvent::type)
               .contains("UserRegistered");
       }
   }
   ```

2. **应用服务测试（启动精简 Spring 上下文）**

   * 使用 `@SpringBootTest` + `@Import`，或 Spring 的 slice test
   * 使用内存 DB / Testcontainers PostgreSQL
   * 验证 repository、事务、一致性

3. **接口/端到端测试（RestTestClient）**

   * 使用 Spring Boot 4 的 `RestTestClient` 做 HTTP 调用([GitHub][5])
   * 模式：

     * `@SpringBootTest(webEnvironment = RANDOM_PORT)`
     * 自动注入 `RestTestClient`
   * 针对关键流程（注册、登录、刷新 token、创建租户等）写端到端测试

### 6.3 脚手架层面的约束

在 `platform-test` 里提供：

* `DomainTestBase`：内置 DomainEvent 收集器、时间假对象等
* `ApplicationTestBase`：带 Testcontainers 容器、统一清理数据
* `ApiTestBase`：封装 RestTestClient、标准断言（HTTP 状态 + 错误码）

在 CI 中约束：

* 新增模块必须有 **领域测试 + 接口测试**
* 行覆盖率/分支覆盖率门槛，如 80%+，为关键模块设更高

---

## 7. AI & MCP 在研发流程中的集成

你很看重 AI 赋能和 MCP，这里把它设计成 **“开发工具层”**，不强绑在业务运行时。

### 7.1 AGENTS/Spec 驱动开发

仓库根目录：

* `AGENTS.md`：给 AI 的项目说明（领域边界、关键名词、编码规范、测试策略）
* `docs/spec/`：更细的领域说明（比如 Identity、Tenant 模块的用例、状态机）

约定：

* 每个新故事，先更新 spec，再开始写代码/测试
* 所有 AI 辅助工具都以 `AGENTS.md + spec` 为第一上下文

### 7.2 MCP Server 设计（Java 实现）

利用 MCP 官方 Java SDK，可以做一个 **`user-center-mcp-server`**：([GitHub][9])

* 提供的 MCP tool 举例：

  1. `domain.use_case.list`：列出当前仓库中的用例文档
  2. `domain.entity.get`：按实体名返回字段、约束和关系
  3. `code.run.tests`：触发 `./gradlew test --tests 'pattern'` 并返回结果
  4. `db.schema.describe`：基于 Flyway/Migration 脚本解析当前 schema

这样你在 Cursor/Windsurf/Claude Desktop 里接入这个 MCP server 后：

* AI 能 **实时读到领域模型和最新 DDL**，生成代码/测试更准确
* 可以在 Chat 里直接调用 `code.run.tests`，做 TDD 循环

### 7.3 platform-ai 模块

`platform-ai` 主要存放：

* 与 MCP server 交互的 Java 客户端（如有需要）
* 一些 AI 辅助的脚本/规范（生成测试、生成 API 文档的模板等）

---

## 8. 实施路线图（按阶段）

**Phase 0：脚手架 & 基础设施（2–3 周级别）**

* 搭建 multi-module Gradle 工程
* 实现 `platform-*` 模块基本能力：

  * 日志、配置、JPA、多租户过滤、基础 Security、test 基类
* 配置 OpenTelemetry、健康检查、基础监控([GitHub][5])
* 初始化 AGENTS.md 和核心领域 spec 文档

**Phase 1：租户 / 组织 / 用户管理（MVP）**

* 实现 Tenant、Organization、User、Membership 领域 & API
* 支持：

  * 创建/冻结租户
  * 为租户创建组织树
  * 注册用户、邀请用户加入租户
  * 基础 RBAC（租户级角色）
* 为这些流程补齐端到端测试

**Phase 2：认证 / 登录 & Token 服务**

* 集成 Spring Authorization Server，完成：

  * OIDC discovery、/authorize、/token、/userinfo
  * Authorization Code + PKCE、Client Credentials
* 客户端管理（ClientApplication）API
* 接入至少一个内部系统验证 SSO 闭环

**Phase 3：审计、扩展协议 & AI 深集成**

* 审计日志域完善：登录、角色变更、租户变更等审计
* 视公司需求考虑：

  * 加入简单的外部 IdP 对接（如企业微信 / 内部 SSO）
  * 更复杂的多组织/多租户模板
* 完成 MCP server 的第一个可用版本（领域查询、跑测试）

---

如果你愿意，下一步我可以：

* 直接给出 **具体的 package 命名与 Maven/Gradle 配置示例**，以及
* 某一个领域（比如 Identity 域）的 **实体 + 表结构 + API 设计草图**，方便你落地第一版代码。

[1]: https://docs.logto.io/integrate-logto/application-data-structure "Application data structure | Logto docs"
[2]: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide?utm_source=chatgpt.com "Spring Boot 4.0 Migration Guide"
[3]: https://docs.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com "System Requirements :: Spring Boot"
[4]: https://spring.pleiades.io/spring-boot/system-requirements.html?utm_source=chatgpt.com "システム要件 :: Spring Boot - リファレンスドキュメント"
[5]: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes "Spring Boot 4.0 Release Notes · spring-projects/spring-boot Wiki · GitHub"
[6]: https://docs.logto.io/user-management/user-data?utm_source=chatgpt.com "User data structure"
[7]: https://docs.logto.io/organizations?utm_source=chatgpt.com "Organizations"
[8]: https://docs.logto.io/authorization/organization-template?utm_source=chatgpt.com "Organization template"
[9]: https://github.com/modelcontextprotocol "Model Context Protocol · GitHub"
