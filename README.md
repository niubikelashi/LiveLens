# LiveLens

A Minecraft plugin that lets you stream your camera view to Twitch, YouTube, or Bilibili.

## Features

- **Live Camera Streaming** — Stream your first-person view directly from the game
- **Multiple Platforms** — Supports Twitch, YouTube, and custom RTMP endpoints  
- **FFmpeg Encoding** — Hardware-accelerated video encoding support
- **Configurable Quality** — Adjust resolution, FPS, and bitrate on the fly

## Installation

1. Download the latest release JAR from [Releases](https://github.com/niubikelashi/LiveLens/releases)
2. Place it in your server's `plugins/` directory
3. Restart or reload your server
4. Configure in `plugins/LiveLens/config.yml`

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/livelens start` | Start streaming | `livelens.use` |
| `/livelens stop` | Stop streaming | `livelens.use` |
| `/livelens status` | Check stream status | `livelens.use` |

## Configuration

Edit `plugins/LiveLens/config.yml`:

```yaml
stream:
  rtmp-url: "rtmp://live.bilibili.com/..."
  video-quality: high
  resolution: "1920x1080"
  fps: 30
```

## Building from Source

Requires Java 21+ and Maven:

```bash
mvn clean package -DskipTests
```

The compiled JAR will be in `LiveLens/target/`.
