package me.bionic.livelens;

import me.bionic.livelens.commands.CommandHandler;
import me.bionic.livelens.stream.StreamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class LiveLens extends JavaPlugin {

    private CameraManager cameraManager;
    private PlayerTracker playerTracker;
    private StreamManager streamManager;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize systems
        this.playerTracker = new PlayerTracker(this);
        this.cameraManager = new CameraManager(this, playerTracker);
        
        // Initialize livestream manager (RTMP push to Bilibili/Douyin)
        this.streamManager = new StreamManager(this);

        // Register commands
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("livelens").setExecutor(commandHandler);

        // Start the main update scheduler (runs every tick)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            playerTracker.updateActivity();
            cameraManager.update();
            streamManager.onTick();

            if ((getConfig().getBoolean("tracking.logging.enable", false) || getConfig().getBoolean("logging.enable", false))
                    && getConfig().getBoolean("logging.log-camera-position", false)) {
                Location loc = cameraManager.getCameraLocation();
                if (loc != null) {
                    getLogger().info("Camera at: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                }
            }
        }, 0L, getConfig().getInt("tracking.interval-ticks", 1));

        getLogger().info("LiveLens has been enabled! Camera system is running.");
    }

    @Override
    public void onDisable() {
        // Clean up livestream sessions
        if (streamManager != null) streamManager.shutdown();
        
        getLogger().info("LiveLens has been disabled. Camera stopped.");
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /** 获取直播推流管理器 */
    public StreamManager getStreamManager() {
        return streamManager;
    }
}