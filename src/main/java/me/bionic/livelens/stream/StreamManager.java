package me.bionic.livelens.stream;

import me.bionic.livelens.CameraManager;
import me.bionic.livelens.LiveLens;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 直播管理器 — 核心控制器。
 * 
 * 功能：
 *   - 初始化 FFmpegEncoder，配置编码参数（分辨率、帧率、RTMP推流地址）
 *   - 为每个玩家启动独立的后台捕获线程
 *   - 从 CameraManager 获取相机位置，逐 tick 采样画面并编码推流
 *   - 支持自动超时停止（防止无限运行）
 *   - 多玩家同时推流隔离
 */
public class StreamManager {

    private final LiveLens plugin;
    
    // RTMP 推流配置
    private String rtmpUrl;
    private int width, height, fps;
    private long maxDurationMs;
    private int captureTickInterval;

    // 每个活跃玩家的会话 + 编码器（并发安全）
    private final java.util.Map<Player, LiveSession> sessions = new java.util.HashMap<>();
    private final java.util.Map<Player, FFmpegEncoder> encoders = new java.util.HashMap<>();

    public StreamManager(LiveLens plugin) {
        this.plugin = plugin;
        
        // 从 config.yml 读取推流参数
        width = plugin.getConfig().getInt("stream.resolution-width", 1280);
        height = plugin.getConfig().getInt("stream.resolution-height", 720);
        fps = plugin.getConfig().getInt("stream.fps", 30);
        
        String streamKey = plugin.getConfig().getString("stream.stream-key", "livelens_stream");
        StreamProvider provider = StreamProvider.fromConfig(
                plugin.getConfig().getString("stream.provider", "bilibili"));
        rtmpUrl = provider.buildUrl(streamKey, 
                plugin.getConfig().getString("stream.custom-rtmp-url", ""));
        
        maxDurationMs = plugin.getConfig().getLong("stream.max-duration-minutes", 60) * 60_000L;
        captureTickInterval = plugin.getConfig().getInt("stream.capture-interval-ticks", 2); // ~15fps at tick=2
        
        plugin.getLogger().info(String.format(
                "StreamManager initialized: %dx%d@%dfps → RTMP (%s)", 
                width, height, fps, provider.getDisplayName()));
    }

    /**
     * 启动直播推流。
     */
    public void startLive(Player player) {
        if (sessions.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "You are already streaming!");
            return;
        }

        try {
            // 1. 创建编码会话
            LiveSession session = new LiveSession(player, 
                    StreamProvider.fromConfig(plugin.getConfig().getString("stream.provider", "bilibili")),
                    width, height, fps);

            // 2. 初始化 FFmpegEncoder
            FFmpegEncoder encoder = new FFmpegEncoder(width, height, fps);
            encoder.setRtmpUrl(rtmpUrl);
            encoder.setMaxDurationMs(maxDurationMs);
            encoder.start();
            
            sessions.put(player, session);
            encoders.put(player, encoder);

            // 3. 启动后台捕获线程
            startCaptureThread(player, encoder);

            player.sendMessage(ChatColor.GREEN + "[LiveLens] Stream started! " 
                    + ChatColor.WHITE + "Provider: " + session.getProvider().getDisplayName()
                    + ", Resolution: " + width + "x" + height
                    + ", FPS: " + fps);
            
            plugin.getLogger().info("Stream started for player: " + player.getName());

        } catch (Exception e) {
            String msg = "[LiveLens] Failed to start stream: " + e.getMessage();
            player.sendMessage(ChatColor.RED + msg);
            plugin.getLogger().severe(msg);
        }
    }

    /** 停止指定玩家的推流 */
    public void stopLive(Player player) {
        LiveSession session = sessions.remove(player);
        FFmpegEncoder encoder = encoders.remove(player);
        
        if (encoder != null) encoder.stop();
        
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not streaming.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "[LiveLens] Stream stopped. Uptime: " 
                + ChatColor.WHITE + String.format("%.0f seconds", session.getUptimeSeconds()));
        
        plugin.getLogger().info("Stream stopped for player: " + player.getName());
    }

    /** 获取玩家当前推流状态 */
    public LiveSession getSession(Player player) {
        return sessions.get(player);
    }

    /** 检查所有活跃会话的超时状态并自动停止 */
    public void checkTimeouts() {
        for (Player p : new java.util.HashSet<>(sessions.keySet())) {
            if (sessions.containsKey(p) && sessions.get(p).isExpired()) {
                p.sendMessage(ChatColor.YELLOW + "[LiveLens] Stream timed out (max duration reached).");
                stopLive(p);
            }
        }
    }

    /** 获取所有活跃会话 */
    public java.util.Collection<LiveSession> getActiveSessions() {
        return sessions.values();
    }

    /** 每 tick 调用的主循环方法（由 LiveLens.onEnable 中的 scheduler 调用） */
    public void onTick() {
        checkTimeouts();
    }

    // ========== 内部方法 ==========

    private void startCaptureThread(Player player, FFmpegEncoder encoder) {
        Thread thread = new Thread(() -> {
            while (encoder.isRunning()) {
                try {
                    // 从 CameraManager 获取相机位置
                    CameraManager cm = plugin.getCameraManager();
                    Location camPos = cm.getCameraLocation();
                    
                    if (camPos != null) {
                        byte[] frameData = FrameCapture.captureFrame(
                                camPos.getWorld(), camPos, width, height);
                        
                        if (frameData != null && frameData.length > 0) {
                            encoder.captureFrame(frameData, captureTickInterval);
                        }
                    } else {
                        Thread.sleep(50L); // wait for camera to initialize
                    }

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    plugin.getLogger().warning("Stream capture error: " + e.getMessage());
                    try { Thread.sleep(100L); } catch (InterruptedException ie) { break; }
                }
            }
        }, "LiveLens-Stream-" + player.getName());
        thread.setDaemon(true); // 不阻止 JVM 退出
        thread.start();
    }

    public void shutdown() {
        for (Player p : sessions.keySet()) {
            stopLive(p);
        }
    }
}
