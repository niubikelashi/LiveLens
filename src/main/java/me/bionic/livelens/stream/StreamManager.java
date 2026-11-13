package me.bionic.livelens.stream;

import me.bionic.livelens.CameraManager;
import me.bionic.livelens.LiveLens;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** 直播管理器 — 核心控制器。 */
public class StreamManager {

    private final LiveLens plugin;
    private String rtmpUrl;
    private int width, height, fps;
    private long maxDurationMs;
    private int captureTickInterval;
    
    // Per-player session + encoder (concurrent-safe)
    private final java.util.Map<Player, LiveSession> sessions = new java.util.HashMap<>();
    private final java.util.Map<Player, FFmpegEncoder> encoders = new java.util.HashMap<>();

    public StreamManager(LiveLens plugin) {
        this.plugin = plugin;

        width = plugin.getConfig().getInt("stream.resolution-width", 1280);
        height = plugin.getConfig().getInt("stream.resolution-height", 720);
        fps = plugin.getConfig().getInt("stream.fps", 30);

        String streamKey = plugin.getConfig().getString("stream.stream-key", "livelens_stream");
        StreamProvider provider = StreamProvider.fromConfig(
                plugin.getConfig().getString("stream.provider", "bilibili"));
        rtmpUrl = provider.buildUrl(streamKey,
                plugin.getConfig().getString("stream.custom-rtmp-url", ""));

        maxDurationMs = plugin.getConfig().getLong("stream.max-duration-minutes", 60) * 60_000L;
        captureTickInterval = plugin.getConfig().getInt("stream.capture-interval-ticks", 2);

        plugin.getLogger().info(String.format(
                "StreamManager initialized: %dx%d@%dfps -> RTMP (%s)",
                width, height, fps, provider.getDisplayName()));
    }

    public void startLive(Player player) {
        if (sessions.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "You are already streaming!");
            return;
        }

        try {
            LiveSession session = new LiveSession(player,
                    StreamProvider.fromConfig(plugin.getConfig().getString("stream.provider", "bilibili")),
                    width, height, fps);

            FFmpegEncoder encoder = new FFmpegEncoder(width, height, fps);
            encoder.setRtmpUrl(rtmpUrl);
            encoder.setMaxDurationMs(maxDurationMs);
            encoder.start();

            sessions.put(player, session);
            encoders.put(player, encoder);

            startCaptureThread(player, encoder);

            player.sendMessage(ChatColor.GREEN + "[LiveLens] Stream started! Provider: " 
                    + session.getProvider().getDisplayName()
                    + ", Resolution: " + width + "x" + height
                    + ", FPS: " + fps);
            
            plugin.getLogger().info("Stream started for player: " + player.getName());

        } catch (Exception e) {
            String msg = "[LiveLens] Failed to start stream: " + e.getMessage();
            player.sendMessage(ChatColor.RED + msg);
            plugin.getLogger().severe(msg);
        }
    }

    public void stopLive(Player player) {
        LiveSession session = sessions.remove(player);
        FFmpegEncoder encoder = encoders.remove(player);
        
        if (encoder != null) encoder.stop();
        
        if (session == null) {
            player.sendMessage(ChatColor.RED + "You are not streaming.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "[LiveLens] Stream stopped. Uptime: " 
                + String.format("%.0f seconds", session.getUptimeSeconds()));
        plugin.getLogger().info("Stream stopped for player: " + player.getName());
    }

    public LiveSession getSession(Player player) { return sessions.get(player); }

    public void checkTimeouts() {
        for (Player p : new java.util.HashSet<>(sessions.keySet())) {
            if (sessions.containsKey(p) && sessions.get(p).isExpired()) {
                stopLive(p);
            }
        }
    }

    public java.util.Collection<LiveSession> getActiveSessions() { return sessions.values(); }

    /** 每 tick 调用的主循环方法 */
    public void onTick() { checkTimeouts(); }

    // ========== Internal methods ==========

    private void startCaptureThread(Player player, FFmpegEncoder encoder) {
        Thread thread = new Thread(() -> {
            while (encoder.isRunning()) {
                try {
                    CameraManager cm = plugin.getCameraManager();
                    Location camPos = cm.getCameraLocation();
                    
                    if (camPos != null && camPos.getWorld() != null) {
                        byte[] frameData = FrameCapture.captureFrame(
                                camPos.getWorld(), camPos, width, height);
                        
                        if (frameData != null && frameData.length > 0) {
                            encoder.captureFrame(frameData, captureTickInterval);
                        }
                    } else {
                        Thread.sleep(50L);
                    }
                } catch (InterruptedException e) { break; }
                catch (Exception e) {
                    plugin.getLogger().warning("Stream capture error: " + e.getMessage());
                    try { Thread.sleep(100L); } catch (InterruptedException ie) { break; }
                }
            }
        }, "LiveLens-Stream-" + player.getName());
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        for (Player p : sessions.keySet()) stopLive(p);
    }
}