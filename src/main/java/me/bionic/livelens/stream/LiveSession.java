package me.bionic.livelens.stream;

import org.bukkit.entity.Player;

/**
 * 直播会话 — 封装推流状态信息。
 */
public class LiveSession {
    private final Player player;
    private final StreamProvider provider;
    private final int width;
    private final int height;
    private final int fps;
    private final long startTime;

    public LiveSession(Player player, StreamProvider provider, int width, int height, int fps) {
        this.player = player;
        this.provider = provider;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.startTime = System.currentTimeMillis();
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

    /** 是否仍在运行（检查超时） */
    private final long maxDurationMs;

    public LiveSession(Player player, StreamProvider provider, int width, int height, int fps, long maxDurationMs) {
        this.player = player;
        this.provider = provider;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.startTime = System.currentTimeMillis();
        this.maxDurationMs = maxDurationMs;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - startTime) > maxDurationMs;
    }

    /** 会话摘要 */
    @Override
    public String toString() {
        return String.format("LiveSession{player=%s, provider=%s, res=%dx%d@%dfps, uptime=%.0fs}",
                player.getName(), provider.getDisplayName(), width, height, fps, getUptimeSeconds());
    }
}
