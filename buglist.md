# 第2阶段待修复问题：
- 暂无记录的问题，如有新增请补充。

# 第3阶段待修复问题：
- 暂无记录的问题，如有新增请补充。

# 第4阶段待修复问题：
- 暂无记录的问题，如有新增请补充。

# 第5阶段待修复问题：
- ✅ [已修复] Swagger 免登录访问：使用 Spring Security 7 新 DSL `http.oauth2AuthorizationServer()` 配置双安全链，授权端点链（Order 0）仅匹配 `/oauth2/**` 和 `/.well-known/**`，应用链（Order 1）放行 Swagger/actuator/公共接口。上线前需评估是否保留或移除放行规则。
