# 全流程留痕与构建前复核工作流规范 (Workflow Guidelines)

## 零、核心交互与执行准则 (最高准则)
1. **未明确要求不主动修改项目**：用户没有明确提出修改要求时，绝不主动修改项目的任何代码、配置或文档；
2. **未明确要求构建 APK 不执行打包发布**：用户没有明确要求构建 APK 时，无需执行测试打包、APK 编译构建和发布流程。

## 一、留痕规范
每一次针对用户需求迭代与版本发布，必须严格在以下文件中进行变更留痕：
1. `UPDATE_LOG.md`：记录当前版本编号、改动需求列表、文件修改清单与产物哈希；
2. `CHANGELOG.md`：记录面向用户的发布日志，包含新增特性、优化项与修复项；
3. `PROJECT.md`：同步最新架构状态、版本号及当前主要组件能力；
4. `README.md`：更新版本号、最新功能清单与构建运行指引；
5. `walkthrough.md`：生成详尽的改动走查与验证记录。

## 二、构建前复核清单 (Pre-Build Review Checklist)
在触发 Gradle APK 构建前，必须逐项核对：
- [x] 1. 用户提出的全部需求点 100% 落实；
- [x] 2. 检查代码语法与 Compose 闭包作用域无异常；
- [x] 3. 检查 `app/build.gradle.kts` 中 `splits.abi` 与 `versionCode` 设置正确；
- [x] 4. 执行单元测试，确保测试全部通过；
- [x] 5. 确保在 `releases` 目录下输出唯一定名的单一安装包（格式：`Echo-v<version>-arm64-v8a.apk`）。

## 三、交付验收标准
- 经过完整编译（`compileDebugKotlin` / `compileReleaseKotlin`）；
- 通过全部单元测试验证；
- 产出 Release APK 安装包并校验 SHA256；
- 检查 Git 工作区，无敏感信息泄漏，完成代码提交。
