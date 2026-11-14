package me.bionic.livelens.stream;

import org.bukkit.entity.Player;

/** 直播会话 — 封装推流状态信息。 */
public class LiveSession {
    private final Player player;
    private final StreamProvider provider;
    private final int width, height, fps;
    private final long startTime;
    private final long maxDurationMs;

    public LiveSession(Player player, StreamProvider provider, int width, int height, int fps) {
        this(player, provider, width, height, fps, 3600_000L);
    }

    public LiveSession(Player player, StreamProvider provider, int width, int height, int fps, long maxDurationMs) {
        this.player = player;
        this.provider = provider;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.startTime = System.currentTimeMillis();
        this.maxDurationMs = Math.max(maxDurationMs, 0);
    }

    public Player getPlayer() { return player; }
    public StreamProvider getProvider() { return provider; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getFps() { return fps; }
    public long getStartTime() { return startTime; }

    /** 运行时长（秒） */
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000L;
    }

    /** 是否超时 */
    public boolean isExpired() {
        return maxDurationMs > 0 && (System.currentTimeMillis() - startTime) > maxDurationMs;
    }
}