# MCP 集成说明

目标：为 AI/代理提供稳定的上下文读取能力，避免重复索引整个仓库。

## 推荐资源配置
- `file-system` 服务器：指向仓库根目录，允许只读访问；可按需挂载 `user-center/` 子目录。
- `git` 资源：暴露当前分支的日志和 diff，便于生成变更摘要。
- 环境变量：`DOCKER_HOST=unix:///Users/wangyiwen/.docker/run/docker.sock`（如需运行 Testcontainers 相关指令）。

## 访问建议
- 读取文件时优先使用 MCP 资源（如 `file:/...`），避免重复扫描。
- 大文件或二进制（如 `target/`、`var/`）默认不暴露；如需可显式允许。
- 生成代码前先查询 `AGENTS.md`、`plan.md`、`buglist.md` 以获取最新约束和遗留问题。

## 调试提示
- 若代理需要运行测试命令，应声明是否依赖 Docker/Testcontainers，并附带必要的环境变量。
- 不要将私钥、JWK 文件等持久化密钥暴露给外部服务。默认 `var/` 已在 `.gitignore` 中忽略。
