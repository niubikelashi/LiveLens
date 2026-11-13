# LiveLens — Minecraft 动态摄像机追踪 + 直播推流插件

> **Paper / Spigot 1.20+ · Leaves** · Java 21 · 实时追踪最活跃玩家，三模式切换，支持推流到 Bilibili/Douyin

## ✨ 功能简介

LiveLens 是一款用于 Minecraft 服务器的动态摄像机系统。它会：
- **自动检测最活跃的玩家**（综合移动速度、跳跃、飞行、载具等因素评分）
- **实时平滑跟随**该玩家的摄像机位置，让被追踪玩家看到自定义视角
- 支持三种摄像机模式，可自由切换
- 🎥 **直播推流** — 将游戏画面编码为 H264，通过 RTMP 推送到 Bilibili（哔哩哔哩）、Douyin（抖音）或任意 RTMP 服务器

| 模式 | 描述 |
|------|------|
| `GOD_VIEW` (上帝视角) | 第三人称俯视，从固定高度和后方观察目标玩家 |
| `FIRST_PERSON` (第一人称) | 跟随被追踪玩家的视线位置（眼睛高度） |
| `ORBIT_VIEW` (轨道环绕) | 以目标玩家为中心水平旋转的环绕摄像机 |

### 📡 RTMP 直播推流

LiveLens 内置 **H264 编码 + RTMP 推流**，可直接将游戏画面直播到：

- **Bilibili（哔哩哔哩）** — `rtmp://live-push.bilivideo.com/live-bvc/STREAM_KEY`
- **Douyin（抖音直播）** — `rtmp://push-l3-c.douyinvod.com/?stream_key=STREAM_KEY`
- **自定义 RTMP 服务器** — 填写完整 URL

推流参数：
- 分辨率：1280x720 (HD) / 可配置
- 帧率：30 FPS（B站/Douyin 推荐）
- 编码：H264 (libx264 via Javacv/FFmpeg)
- 自动超时保护：默认 60 分钟，可配置

## 📦 安装

1. **下载**：从 [GitHub Releases](https://github.com/niubikelashi/LiveLens/releases/latest) 获取最新 `.jar`。
2. **放置**：将 jar 放入服务器的 `plugins/` 目录。
3. **重启**：重新启动服务器（或使用 `/reload`）。
4. **配置**：编辑 `plugins/LiveLens/config.yml`，按需调整参数。

## ⌨️ 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/livelens god` | livelens.use | 切换为上帝视角（第三人称） |
| `/livelens first` | livelens.use | 切换为第一人称模式 |
| `/livelens orbit` | livelens.use | 切换为轨道环绕模式 |
| `/livelens switch` | livelens.use | 循环切换三种模式（God → First → Orbit → God） |
| `/livelens status` | livelens.use | 查看当前摄像机模式和追踪状态 |
| `/livelens live start` | livelens.stream.start | **开始直播推流**到 Bilibili/Douyin |
| `/livelens live stop` | livelens.stream.start | **停止直播推流** |
| `/livelens help` | livelens.use | 显示帮助信息 |

> **注意**：所有命令控制台（RCON/命令方块）均可执行。玩家需 `livelens.use` 权限，默认 OP 自动拥有。直播需要额外 `livelens.stream.start` 权限（OP 自动拥有）。

## 🔑 权限

| 节点 | 默认值 | 说明 |
|------|--------|------|
| `livelens.use` | true | 使用 LiveLens 命令的权限 |
| `livelens.stream.start` | op | 启动直播推流的权限（OP 默认拥有） |

## 🎥 直播配置 (`config.yml`)

```yaml
stream:
  # 推流平台: bilibili, douyin, custom
  provider: bilibili
  
  # 分辨率
  resolution-width: 1280
  resolution-height: 720
  
  # 帧率 (FPS) — B站/Douyin 推荐 30fps
  fps: 30
  
  # 推流密钥（Bilibili/Douyin 后台获取）
  stream-key: livelens_stream
  
  # 自定义 RTMP URL（provider=custom 时使用，支持 ${stream_key} 占位符）
  custom-rtmp-url: ""
  
  # 最大直播时长（分钟），0 = 不限制
  max-duration-minutes: 60
  
  # 采样间隔（tick 数），2 ≈ 15fps, 1 ≈ 30fps
  capture-interval-ticks: 2
```

### Bilibili 推流步骤：
1. 进入 [B站直播中心](https://live.bilibili.com/pc/web/my-live) → 开播设置
2. 获取 **RTMP 地址**和**流密钥（Stream Key）**
3. 在 `config.yml` 中设置 `provider: bilibili` 和对应的 `stream-key`

### Douyin 推流步骤：
1. 进入 [抖音直播伴侣](https://live.douyin.com/) → 网页开播
2. 获取 RTMP 推流地址和密钥
3. 在 `config.yml` 中设置 `provider: douyin` 和对应的 `stream-key`

## ⚙️ 完整配置说明

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

## 🏗️ 构建

```bash
# Maven 编译（需要 JDK 21+）
mvn clean package

# 产物位置
target/LiveLens-1.0-SNAPSHOT.jar
```

服务器运行要求：
- **Java 21**（Paper 1.21+ class file version 65）
- **Paper / Leaves** 核心（使用 Paper API 的 `setCamera()`，兼容 Spigot 回退）
- Javacv/FFmpeg 原生库自动打包在 jar 中，无需额外安装

## 📝 已知限制

- 需要 **Paper 1.20+**（依赖 `Player.setCamera(Location)` API，Spigot 原版通过反射回退支持）
- `api-version: 1.20` 限定在 1.20.x 系列运行
- 直播推流质量取决于方块颜色映射精度（非实际渲染截图），适合展示游戏场景概览
- 推流帧率与服务器 TPS 相关，低 TPS 时可能影响推流流畅度

## 📄 License

MIT
