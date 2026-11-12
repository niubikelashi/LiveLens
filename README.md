# LiveLens — Minecraft 动态摄像机追踪插件

> **Paper / Spigot 1.20+** · Java 17 · 实时追踪最活跃玩家，上帝视角、第一人称、轨道环绕三模式切换

## ✨ 功能简介

LiveLens 是一款用于 Minecraft 服务器的动态摄像机系统。它会：
- **自动检测最活跃的玩家**（综合移动速度、跳跃、飞行、载具等因素评分）
- **实时平滑跟随**该玩家的摄像机位置，让被追踪玩家看到自定义视角
- 支持三种摄像机模式，可自由切换：

| 模式 | 描述 |
|------|------|
| `GOD_VIEW` (上帝视角) | 第三人称俯视，从固定高度和后方观察目标玩家 |
| `FIRST_PERSON` (第一人称) | 跟随被追踪玩家的视线位置（眼睛高度） |
| `ORBIT_VIEW` (轨道环绕) | 以目标玩家为中心水平旋转的环绕摄像机 |

## 📦 安装

1. **下载**：从 GitHub Actions 构建的 Artifacts 中获取 `LiveLens-*.jar`，或自行 Maven 编译。
2. **放置**：将 jar 放入服务器的 `plugins/` 目录。
3. **重启**：重新启动服务器（或使用 `/reload`）。
4. **配置**：编辑 `plugins/LiveLens/config.yml`，按需调整摄像机参数。

## ⌨️ 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/livelens god` | livelens.use | 切换为上帝视角（第三人称） |
| `/livelens first` | livelens.use | 切换为第一人称模式 |
| `/livelens orbit` | livelens.use | 切换为轨道环绕模式 |
| `/livelens switch` | livelens.use | 循环切换三种模式（God → First → Orbit → God） |
| `/livelens status` | livelens.use | 查看当前摄像机模式和追踪状态 |
| `/livelens help` | livelens.use | 显示帮助信息 |

> **注意**：所有命令控制台（RCON/命令方块）均可执行。玩家需 `livelens.use` 权限，默认 OP 自动拥有。

## 🔑 权限

| 节点 | 默认值 | 说明 |
|------|--------|------|
| `livelens.use` | true | 使用 LiveLens 命令的权限 |

## ⚙️ 配置说明 (`config.yml`)

### 摄像机设置 (camera)

```yaml
camera:
  god-view:
    height: 15.0          # 上帝视角高度（玩家头顶上方）
    back-offset: 8.0      # 后方偏移距离
  
  smooth-speed: 0.1       # 平滑速度 (0~1，越大越快)
  
  orbit-view:
    radius: 10.0          # 环绕半径（水平距离）
    height: 5.0           # 高度偏移（玩家头顶上方）
    rotation-speed: 0.02  # 旋转速度（弧度/tick）
```

### 追踪系统 (tracking)

```yaml
tracking:
  interval-ticks: 1       # 活动检测间隔（tick，20次/秒=1 tick）
  decay-factor: 0.95      # 分数衰减因子（越小衰减越快）
```

### 日志 (logging)

```yaml
logging:
  enable: true              # 启用日志开关
  log-camera-position: false # 每 tick 输出摄像机坐标（调试用）
```

> **提示**：`enable` 和 `log-camera-position` 同时为 true 时才会输出日志。默认只开启 enable，不刷屏。

## 🏗️ 构建

```bash
mvn clean package
```

产物会生成在 `target/` 目录下。确保服务器已安装 **Paper**（使用 Paper API 的 `setCamera()`）。

## 📝 已知限制

- 需要 **Paper 1.20+**（依赖 `Player.setCamera(Location)` API，Spigot 原版不支持）
- `api-version: 1.20` 限定在 1.20.x 系列运行
- 目前只改变被追踪玩家的摄像机视角，其他玩家不受影响

## 📄 License

MIT
