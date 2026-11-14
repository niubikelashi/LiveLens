package com.bionic.creativebox;

import org.bukkit.*;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

/**
 * 沙盒世界管理器：创建/加载世界、设置出生点、配置 gamerule、重置世界。
 */
public class SandboxWorldManager {

    private final CreativeBoxPlugin plugin;
    private World sandboxWorld;
    private Location spawnLocation;

    public SandboxWorldManager(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    /** 初始化沙盒世界：判断是否存在 → 不存在则创建 → 设置出生点 → 配置 gamerule */
    public boolean initializeSandbox() {
        FileConfiguration config = plugin.getConfig();

        // 1. 获取/创建世界（必须先 getWorld 判空！坑 #1）
        String worldName = config.getString("world.name", "sandbox");
        this.sandboxWorld = Bukkit.getWorld(worldName);
        
        if (this.sandboxWorld == null) {
            WorldType type;
            try {
                type = WorldType.valueOf(config.getString("world.type", "FLAT"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid world type, using FLAT.");
                type = WorldType.FLAT;
            }

            boolean generateStructures = config.getBoolean("world.generate-structures", false);
            long seed = config.getLong("world.seed", 0);

            this.sandboxWorld = new WorldCreator(worldName)
                    .type(type)
                    .generateStructures(generateStructures)
                    .seed(seed)
                    .createWorld();

            if (this.sandboxWorld == null) {
                plugin.getLogger().severe("Failed to create sandbox world!");
                return false;
            }

            // 自动铺设平台（如果配置开启）
            if (config.getBoolean("world.auto-build-platform", true)) {
                buildAutoPlatform();
            }

            plugin.getLogger().info("Created new sandbox world: " + worldName);
        } else {
            plugin.getLogger().info("Reusing existing sandbox world: " + worldName);
        }

        // 2. 设置出生点（从配置读取）
        spawnLocation = loadSpawnFromConfig();
        this.sandboxWorld.setSpawnLocation(spawnLocation);
        plugin.getLogger().info("Sandbox spawn set to " + spawnLocation.toVector());

        // 3. 配置 gamerule
        applyGamerules();

        return true;
    }

    /** 在出生点下方铺设 5x5 平台 */
    private void buildAutoPlatform() {
        int baseY = (int) spawnLocation.getY() - 1;
        Location loc = new Location(sandboxWorld,
                Math.floor(spawnLocation.getX()) - 2.0, baseY, 
                Math.floor(spawnLocation.getZ()) - 2.0);

        for (double dx = 0; dx < 5; dx++) {
            for (double dz = 0; dz < 5; dz++) {
                loc.add(dx, 0, dz).getBlock().setType(Material.BEDROCK);
                loc.add(0, 1, 0).getBlock().setType(Material.STONE);
            }
        }
    }

    /** 从配置文件加载出生点坐标 */
    private Location loadSpawnFromConfig() {
        FileConfiguration config = plugin.getConfig();
        return new Location(sandboxWorld,
                config.getDouble("spawn.x", 0.5),
                config.getDouble("spawn.y", 65.0),
                config.getDouble("spawn.z", 0.5),
                (float) config.getDouble("spawn.yaw", 0.0),
                (float) config.getDouble("spawn.pitch", 0.0));
    }

    /** 应用 gamerule 到沙盒世界 */
    private void applyGamerules() {
        FileConfiguration config = plugin.getConfig();
        
        if (sandboxWorld != null) {
            sandboxWorld.setGameRule(World.GameRule.DO_DAYLIGHT_CYCLE, 
                    config.getBoolean("gamerules.do-daylight-cycle", false));
            sandboxWorld.setGameRule(World.GameRule.DO_WEATHER_CYCLE, 
                    config.getBoolean("gamerules.do-weather-cycle", false));
            sandboxWorld.setGameRule(World.GameRule.DO_MOB_SPAWNING, 
                    config.getBoolean("gamerules.do-mob-spawning", false));
            sandboxWorld.setGameRule(World.GameRule.MOB_GRIEFING, 
                    config.getBoolean("gamerules.mob-griefing", false));
            sandboxWorld.setGameRule(World.GameRule.KEEP_INVENTORY, 
                    config.getBoolean("gamerules.keep-inventory", true));
            sandboxWorld.setGameRule(World.GameRule.PVP, 
                    config.getBoolean("gamerules.pvp", false));

            plugin.getLogger().info("Gamerules applied to sandbox world.");
        }
    }

    /** 卸载沙盒世界（所有玩家必须先被移出） */
    public boolean unloadSandboxWorld() {
        if (sandboxWorld == null) return true;
        
        for (Player player : sandboxWorld.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0));
        }

        boolean unloaded = sandboxWorld.unload(true, false);
        if (unloaded) {
            plugin.getLogger().info("Sandbox world unloaded successfully.");
        } else {
            plugin.getLogger().warning("Failed to unload sandbox world!");
        }
        this.sandboxWorld = null;
        return unloaded;
    }

    /** 获取沙盒世界（可能为 null） */
    public World getSandboxWorld() { return sandboxWorld; }

    /** 获取出生点位置 */
    public Location getSpawnLocation() { return spawnLocation; }

    /** 重置沙盒世界：unload → delete directory → reinitialize */
    public boolean resetSandboxWorld(SandboxService service) {
        // Step 1: 所有玩家安全送回主世界（走 SandboxService exit 流程）
        for (Player player : sandboxWorld.getPlayers()) {
            if (service != null) {
                service.exit(player);
            } else {
                player.teleport(Bukkit.getWorlds().get(0));
            }
        }

        // Step 2: 卸载世界
        unloadSandboxWorld();

        // Step 3: 删除世界目录（Paper/Leaves 使用 .db 后缀）
        String dirName = plugin.getConfig().getString("world.name", "sandbox");
        File worldDir = new File(plugin.getDataFolder().getParentFile().getParent(), dirName + ".db");
        if (!worldDir.exists()) {
            worldDir = new File(plugin.getDataFolder().getParentFile().getParent(), dirName);
        }
        
        if (worldDir.isDirectory()) {
            deleteDirectory(worldDir);
            plugin.getLogger().info("Deleted sandbox world directory: " + worldDir.getName());
        }

        return true;
    }

    private void deleteDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirectory(child);
            }
        }
        dir.delete();
    }

    public String getWorldName() {
        return plugin.getConfig().getString("world.name", "sandbox");
    }
}
