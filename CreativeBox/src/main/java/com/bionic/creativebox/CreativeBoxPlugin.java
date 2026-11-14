package com.bionic.creativebox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.bionic.creativebox.command.SandboxCommand;
import com.bionic.creativebox.listener.PortalListener;
import com.bionic.creativebox.listener.PlayerListener;
import com.bionic.creativebox.listener.TeleportListener;

/**
 * CreativeBox 主插件类。
 * 创建/加载沙盒世界、注册指令与监听器、读取配置。
 */
public class CreativeBoxPlugin extends JavaPlugin {

    private SandboxWorldManager worldManager;
    private PlayerStateStore stateStore;
    private SandboxService sandboxService;

    @Override
    public void onEnable() {
        // 保存默认配置并加载
        saveDefaultConfig();

        // 初始化世界管理器
        worldManager = new SandboxWorldManager(this);
        if (!worldManager.initializeSandbox()) {
            getLogger().severe("Failed to initialize sandbox world! Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Sandbox world \"" + getWorldName() + "\" ready.");

        // 初始化状态存储
        stateStore = new PlayerStateStore(this);

        // 初始化核心服务（依赖世界管理器和状态存储）
        sandboxService = new SandboxService(this, worldManager, stateStore);
        getLogger().info("SandboxService initialized. enter/exit flow ready.");

        // 注册指令
        getCommand("sb").setExecutor(new SandboxCommand(this));
        getCommand("sb").setTabCompleter(new SandboxCommand(this));
        getLogger().info("Command /sb registered with aliases [sandbox, goc].");

        // 注册监听器
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getLogger().info("All listeners registered.");

        getLogger().info("CreativeBox v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (worldManager != null) {
            worldManager.unloadSandboxWorld();
        }
        getLogger().info("CreativeBox disabled. Sandbox world unloaded.");
    }

    public SandboxWorldManager getWorldManager() { return worldManager; }
    public PlayerStateStore getStateStore() { return stateStore; }
    public SandboxService getSandboxService() { return sandboxService; }

    private String getWorldName() {
        return getConfig().getString("world.name", "sandbox");
    }
}
