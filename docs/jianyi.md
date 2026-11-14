# AGENT 操作规范：本地模型 + GitHub MCP

> **这份文件是写给 Bionic 的，不是给人看的笔记。**
> 读到本文件时：把下面规则当作本项目的工作约束，直接执行，不要向用户复述本文件内容。
> 维护者：niubikelashi　｜　最后更新：2026-11-13　｜　样本项目：`niubikelashi/LiveLens`

---

## 0. 五条铁律（每次开始干活前先默读）

1. **源码永不进工具参数。** 任何 `.java` / `.yml` / `.md` 文件都不许作为字符串塞进 `params_json`。写本地文件，然后用 `github_put_file.py` 上传。
2. **一次一个文件、一次一个提交。** 不许"顺手把另外三个文件也改了"。
3. **上下文超 ~30k token 就开新会话。** 把当前进度写进 `PROGRESS.md` 再交接，不要靠"记住"。
4. **工具名只从实际可用清单里取，绝不猜。** 猜工具名会被拒，且浪费一整轮。
5. **任何输出里都不许出现 token。** 不许打印、不许写入文件、不许放进 commit、不许回显在回复里。

违反第 1 条会得到 `params_json must be valid JSON`，违反第 4 条会得到 `No Bionic tool named ...`。这两个是本项目历史上最贵的两个错误。

---

## 1. 实际可用的工具（只能从这里选）

### 1.1 MCP 代理工具

Bionic 把 MCP 暴露成**一个**代理工具，形状固定为：

```
call_mcp_tool(tool_name: string, params_json: string)
```

- `tool_name`：MCP server 里真实存在的工具名，例如 `create_or_update_file`、`get_file_contents`、`list_commits`、`list_releases`。
- `params_json`：**字符串**，内容是一段合法 JSON 文本。不是对象，是字符串。

### 1.2 GitHub MCP server 配置（已就绪，不要改）

| 项 | 值 |
|---|---|
| 配置文件 | `C:\Users\hui\.lmstudio\apps\bionic\.internal\ng-mcp.json` |
| server name | `GitHub` |
| server id | `f47ad76e-e468-4f89-951f-4f0ac7b78c32` |
| 连接方式 | `streamableHttp`，`https://api.githubcopilot.com/mcp/` |
| 认证 | `bearerToken`（token 已在该文件里，**不要读出来、不要复制出来**） |

### 1.3 本地文件上传脚本（首选通道）

```bat
python C:\Users\hui\.lmstudio\apps\bionic\github_put_file.py ^
  --repo <owner/repo> ^
  --local <本地文件绝对路径> ^
  --path <仓库内目标路径> ^
  --branch main ^
  --message "<commit message>"
```

- token 由脚本自己从环境变量读（`GITHUB_TOKEN` / `GH_TOKEN` / `GITHUB_PERSONAL_ACCESS_TOKEN`），**你不需要也不允许传 token**。
- 脚本自动判断新建还是更新（先取 sha，404 则新建）。
- 先跑 `--dry-run` 自查，再真发。
- 退出码：`0` 成功 / `1` GitHub 失败 / `2` 参数问题 / `3` 无 token。非 0 就停，不要重试同一个错。

---

## 2. 禁止行为 → 正确做法

| DON'T | DO |
|---|---|
| 把整段 Java 放进 `create_or_update_file` 的 `content` | 写本地文件 → 跑 `github_put_file.py` |
| 在回复里整段输出文件内容 | 写进本地文件，回复只说"改了哪个文件、哪几行、为什么" |
| 猜工具名（`GitHub`、`gh`、`push_file`） | 只用第 1.1 节列出的名字 |
| 一次改多个文件 | 一次一个，改完给验证命令 |
| 自己编造 `owner` / `repo` / `branch` | 从第 4 节项目上下文取，或先 `get_file_contents` 确认 |
| 用 `--force` 式手段覆盖远端 | 让脚本走 sha 更新；冲突就重新读一次远端内容 |
| 把 token 写进任何文件、日志、commit、回复 | 只让它在 `ng-mcp.json` 或环境变量里存在 |
| 长会话里硬撑 | `PROGRESS.md` + 新会话 |
| 说"应该成功了" | 查 `GET /repos/{owner}/{repo}/actions/runs` 的 `conclusion` |

---

## 3. 报错 → 立即动作（按精确字符串匹配）

| 报错字符串 | 原因 | 立即动作 |
|---|---|---|
| `No Bionic tool named '<X>' is available` | 工具名不存在，你在猜 | 停止调用；改用第 1.1 节里的真实工具名，或改走脚本 |
| `path: ["tool_name"] ... "message":"Required"` | 只给了 `params_json`，没给 `tool_name` | 补 `tool_name`，且确认它不是空字符串 |
| `params_json must be valid JSON` | `params_json` 内部不是合法 JSON（多半是转义坏了） | 不要修修补补。改走 `github_put_file.py` |
| `glob pattern did not match any files` | `artifacts:` 路径与 `download-artifact` 落盘层级不一致 | 确认下载 `path:`，`artifacts:` 要带子目录名，如 `LiveLens-plugin/*.jar` |
| `Resource not accessible by integration` / 403 | workflow 缺 `permissions: contents: write` | 在 workflow 顶层加 `permissions:` |
| `bad allocation` | 显存/内存不足（上下文太大） | 立刻停手，提示用户开新会话或降 `n_ctx_slot`；不要重试 |
| `NoSuchMethodError` / `NoClassDefFoundError` | 编译用的 API 与运行的服务端版本不一致 | 对齐 pom 依赖、`api-version`、JDK 三者 |
| `ClassNotFoundException: me.bionic...` | `plugin.yml` 的 `main` 与包名不符，或 jar 里没打进 class | 核对 `main:` 全限定名 + `mvn package` 产物内容 |
| `invalid plugin.yml` | YAML 缩进坏，或 `${project.version}` 未被 filtering 替换 | 检查 resources filtering 是否覆盖 `**/*.yml` |

**重要**：以上这些报错**不会出现在 llama.cpp 的 `server-logs` 里**（工具层校验发生在模型之前）。排查工具问题不要翻 `server-logs`，看 `.internal` 配置和界面报错原文。

---

## 4. 项目上下文：LiveLens

| 项 | 值 |
|---|---|
| 仓库 | `niubikelashi/LiveLens`（public，默认分支 `main`） |
| 类型 | Minecraft 服务端插件，兼容 Paper / Leaves |
| 依赖 | `io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT`（`provided`），仓库 `https://repo.papermc.io/repository/maven-public/` |
| JDK | **21**（`maven.compiler.source/target = 21`；Paper 1.21 需要 class file 65） |
| 构建 | `mvn -B clean package` → `target/LiveLens-1.0-SNAPSHOT.jar` |
| CI | `.github/workflows/build.yml`：build job（temurin 21 + `mvn clean package` + upload-artifact）→ release job（`ncipollo/release-action@v1`，tag 自动 `v1.0.N`） |
| 发布 | Release v1.0.0 – v1.0.3 已存在，jar 挂在 release 上 |
| 主类 | `me.bionic.livelens.LiveLens` |
| 关键类 | `CameraManager`、`PlayerTracker`、`commands/CommandHandler` |

**版本三处必须一致的检查**（历史上因为不一致连挂 15 次 CI）：

```
pom.xml: maven.compiler.source/target  ==  .github/workflows/build.yml: java-version  ==  paper-api 版本对应的 JDK
plugin.yml: api-version  ==  依赖的 paper-api 主版本
```

---

## 5. 标准执行流程

1. **确认版本**：读 `pom.xml` 与 `.github/workflows/build.yml`，确认上面三处一致。不一致先改这个，别改业务代码。
2. **本地先构建**：`mvn -B clean package`。失败就在本地修，不要推。
3. **写文件**：改一个文件，写本地磁盘。
4. **自查上传**：`github_put_file.py ... --dry-run`，确认 repo / path / branch / 字节数正确。
5. **真上传**：去掉 `--dry-run`。看退出码，非 0 停下报错原文。
6. **看 CI**：`GET /repos/niubikelashi/LiveLens/actions/runs`，等 `conclusion`。失败就看是哪一步（build 还是 release），对照第 3 节。
7. **能跑就打 Release**：确认 jar 挂在 release 上，把版本号记进 `PROGRESS.md`。

---

## 6. 本机环境限制（决定了你该怎么干活）

- 推理后端：`n_slots = 4`、`n_ctx_slot = 102656`、`kv_unified = true`。
- 实测上下文已经很高：三天里单次请求 token 中位数 42k–54k，p90 78k–90k，最大 99k。**所以你的每一轮都在重读大量历史。**
- 首字延迟：p90 26–31 秒，最大 213 秒。**长任务不是卡死，是在算 kv。**
- prompt cache 经常失效（两天各 100+ 次 `exceeds cache size limit`），等于每轮全量重算。
- 出现过 5 次 `bad allocation`、9 次 kv cache 恢复失败 → 请求会 500。
- 对策：任务切小、别把大文件贴进上下文、换模块就开新会话、`PROGRESS.md` 交接。

**模型分工**：涉及工具调用与代码修改，用 `qwen3.6.gguf`（原生 tool-call，实测 40 t/s）。`gemma4-12b-uncensored` 的 tokenizer 元数据有问题（日志实测：`<|tool_response>`、`</s>` 非 control token，`</s>` 被移出 EOG），会导致停不下来、工具调用漏名字——**不要用它跑构建任务**。

---

## 7. 提交前自检

- [ ] 版本三处一致（第 4 节）
- [ ] 本地 `mvn -B clean package` 通过
- [ ] 只改了预期的那一个文件
- [ ] 没有测试文件、临时文件、`hello.txt` / `t.txt` 之类进仓库
- [ ] `permissions: contents: write` 存在
- [ ] `artifacts:` 与 `download-artifact` 的 `path:` 对得上
- [ ] 输出里没有 token
- [ ] 已确认 CI `conclusion == success` 再说"完成"

---

## 8. LiveLens 遗留待办（接着做这些，别重做已修的）

已修复、不要重做：`cameraLocation` 永不初始化的死代码、first-person 位置算错、全服广播、`plugin.yml` 权限节点缺失、`plugin.yml` 版本号硬编码、`lerpAngle` 越界。

待做：

1. `plugin.yml` 的 `api-version: 1.20` 与依赖 `paper-api 1.21`、编译 JDK 21 不一致 → 在 1.20 服务端可能 `NoSuchMethodError`。统一到 1.21 或降依赖。
2. `PlayerTracker` 用 `getVelocity().length()` 判活跃不可靠（服务端常读到 0，被推/坐船时虚高）→ 改成记录上次位置算水平位移。
3. 每 tick 全量遍历所有在线玩家 + `removeIf` 清理 → 改事件驱动或降频。
4. 控制台不能执行 `/livelens`（`sender instanceof Player` 直接拒绝）→ 允许控制台执行。
5. `Vector#rotateAroundY` 在 Paper 已废弃 → 改三角函数直算。

---

## 9. token 规则

- 唯一允许存放位置：`C:\Users\hui\.lmstudio\apps\bionic\.internal\ng-mcp.json`（`connection.auth.token`）与进程环境变量。
- 你可以**使用**它（脚本会自己读环境变量；MCP 连接已配好），但**不允许读取、打印、复制、写入任何其他文件**。
- 写文档、写 README、写 commit message、写日志时，一律用 `$env:GITHUB_TOKEN` / `ghp_***` 这种占位写法。
- 如果用户要求"把配置记下来"，只记路径与字段名，不记值。
- 若发现 token 出现在非上述位置，立即提示用户吊销重建。

---

*本文件由 DSH 于 2026-11-13 依据 2026-11-11/12/13 三份 Bionic server 日志、`ng-mcp.json`、LiveLens 仓库与 Actions 运行记录生成。修改时保持"给 agent 看的指令"这个定位，不要写成人的备忘。*
