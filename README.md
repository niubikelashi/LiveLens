# LiveLens - Minecraft 直播推流插件

![Paper](https://img.shields.io/badge/Paper-1.20%2B-green)
Leaves Compatible | Java 21 Required

LiveLens 是一个功能丰富的 Minecraft 服务器端摄像机系统，支持动态追踪最活跃玩家并实时推流到 Bilibili/Douyin。

## 安装步骤

1. **编译**: mvn clean package -DskipTests  
   JAR: LiveLens/target/LiveLens-1.0-SNAPSHOT-with-deps.jar
2. **部署**: 放入服务器 plugins/ 目录
3. **配置**: 编辑 plugins/LiveLens/config.yml

## 命令列表

| 命令 | 权限 | 描述 |
|------|------|------|
| /livelens god | livelens.use | 上帝视角 |
| /livelens first | livelens.use | 第一人称跟随 |
| /livelens orbit | livelens.use | 轨道环绕 |
| /livelens switch | livelens.use | 循环切换模式 |
| /livelens status | livelens.use | 查看摄像机状态 |

## RTMP 推流设置

### Bilibili 密钥获取 (1) live.bilibili.com -> 创作服务中心 -> 开始直播 -> OBS/第三方软件
复制 RTMP URL + 推流密钥。

`yaml
stream:
  provider: bilibili
  stream-key: "你的B站密钥"
`

### Douyin 密钥获取 (2) creator.douyin.com -> 开播 -> 第三方推流软件
复制地址和密钥。

`yaml
stream:
  provider: douyin  
  stream-key: "你的抖音密钥"
`

### 自定义 RTMP (3)
`yaml
stream:
  provider: custom
  custom-rtmp-url: "rtmp://server/live/\"
  stream-key: "my_stream"
`

## 关键配置参数

| 路径 | 默认值 | 说明 |
|------|--------|------|
| camera.god-view.height | 15.0 | 上帝视角高度 |
| camera.smooth-speed | 0.1 | 平滑速度 (0~1) |
| stream.fps | 30 | 推流帧率 |
| stream.capture-interval-ticks | 2 | 采样间隔(2=15fps, 1=30fps) |
| stream.max-duration-minutes | 60 | 最大直播时长(0=不限制) |

## 权限节点

- livelens.use (default: true) - 基本命令
- livestream.start (default: op) - 推流控制

## 兼容性

Paper/Spigot 1.20+ / Leaves (Java 21, Javacv内置shaded)


<!-- CI test -->

<!-- CI test -->