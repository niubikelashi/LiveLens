package me.bionic.livelens;

import org.bukkit.plugin.java.JavaPlugin;

public class LiveLens extends JavaPlugin {
    private CameraManager cameraManager;
    private PlayerTracker playerTracker;

    @Override
    public void onEnable() {
        this.playerTracker = new PlayerTracker(this);
        this.cameraManager = new CameraManager(this, playerTracker);
        getLogger().info("LiveLens has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("LiveLens has been disabled.");
    }

    public PlayerTracker getPlayerTracker() { return playerTracker; }
    public CameraManager getCameraManager() { return cameraManager; }
}