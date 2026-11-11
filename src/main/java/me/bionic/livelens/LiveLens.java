package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LiveLens extends JavaPlugin {
    private CameraManager cameraManager;
    private PlayerTracker playerTracker;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        getLogger().info("LiveLens loaded. Config saved.");

        this.playerTracker = new PlayerTracker(this);
        this.cameraManager = new CameraManager(this, playerTracker);

        // 注册命令 (用于测试切换视角)
        getCommand("livelens").setExecutor(new CommandHandler(this));

        // 启动后台任务：每 1 tick 更新摄像机和玩家活跃度
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            playerTracker.updateActivity();
            cameraManager.update();
        }, 0L, 1L);
    }

    @Override
    public void onDisable() {
        getLogger().info("LiveLens has been disabled.");
    }

    public PlayerTracker getPlayerTracker() { return playerTracker; }
    public CameraManager getCameraManager() { return cameraManager; }
}
