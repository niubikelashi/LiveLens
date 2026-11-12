package me.bionic.livelens;

import me.bionic.livelens.commands.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class LiveLens extends JavaPlugin {

    private CameraManager cameraManager;
    private PlayerTracker playerTracker;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize systems
        this.playerTracker = new PlayerTracker(this);
        this.cameraManager = new CameraManager(this, playerTracker);

        // Register commands
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("livelens").setExecutor(commandHandler);

        // Start the main update scheduler (runs every tick)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            playerTracker.updateActivity();
            cameraManager.update();

            if (getConfig().getBoolean("logging.enable") && getConfig().getBoolean("logging.log-camera-position")) {
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
        getLogger().info("LiveLens has been disabled. Camera stopped.");
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }
}