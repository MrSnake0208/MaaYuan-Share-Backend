# Repository Guidelines

## Project Structure & Module Organization
- Backend: `src/main/kotlin/plus/maa/backend/*`（按层分包：`config/`、`common/`、`controller/`、`service/`、`repository/`）。
- Resources: `src/main/resources/*`（配置、模板、静态资源）。
- Tests: `src/test/kotlin/*`（单元/集成测试）。
- Ops: `dev-docker/`、`docker/`、`systemd/`，脚本：`scripts/`。

## Build, Test, and Development Commands
- 开发运行：`./gradlew bootRun`（使用本地 `application-dev.yml`）。
- 运行依赖：`docker compose -f dev-docker/docker-compose.yml up -d`（Redis、MongoDB）。
- 构建可运行包：`./gradlew bootJar`；镜像：`./gradlew bootBuildImage`。
- 测试：`./gradlew test`。
- 代码格式检查/修复：`./gradlew ktlintCheck` / `./gradlew ktlintFormat`。
- OpenAPI/客户端生成：见 `build.gradle.kts` 中 `openApi` 与 `swaggerSources` 配置对应任务。

## Coding Style & Naming Conventions
- Kotlin 使用 4 空格缩进（见 `.editorconfig`）；最大行长 140；LF；UTF‑8。
- 使用 ktlint（IntelliJ IDEA 风格），禁止通配符导入；提交前请运行 `ktlintCheck`。
- 包命名小写、类名 `PascalCase`，方法/变量 `camelCase`，常量 `UPPER_SNAKE_CASE`。

## Testing Guidelines
- 框架：JUnit 5 + Spring Boot Test，Mock 工具：MockK。
- 测试命名：类以 `*Test.kt` 结尾；包路径与被测代码一致。
- 覆盖重点：控制器边界条件、服务核心逻辑、仓储交互与安全相关分支。
- 运行：`./gradlew test`（建议搭配本地 Redis/MongoDB，或使用模拟/容器）。

## Commit & Pull Request Guidelines
- 提交遵循 Conventional Commits（建议中文描述），示例：
  - `feat: 增加评论分页 API`
  - `fix: 修复 Jwt 过期判断`
- PR 要求：清晰描述变更、动机与影响；关联 Issue；附本地运行/测试说明与重要日志片段；界面/协议变更请同步 OpenAPI 文档与客户端生成。

## Security & Configuration Tips
- 基于模板创建配置：复制 `src/main/resources/application-template.yml` 为 `application-dev.yml` 或 `application-prod.yml`。
- 切勿提交密钥与私密配置；通过环境变量或外部挂载覆盖敏感项；使用 `--spring.profiles.active=dev|prod` 切换配置。

