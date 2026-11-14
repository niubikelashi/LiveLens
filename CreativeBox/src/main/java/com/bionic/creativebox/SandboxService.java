package com.bionic.creativebox;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心状态机：编排 enter()/exit() 完整流程。
 * 所有进入/退出的状态变更只能通过本类发生。
 * 
 * 坑 #5: TeleportEvent 递归 — 用 isEntering/isExiting Set 标记挡住。
 * 坑 #4: setFlying(true) 前必须先 setAllowFlight(true)。
 */
public class SandboxService {

    private final CreativeBoxPlugin plugin;
    private final SandboxWorldManager worldManager;
    private final PlayerStateStore stateStore;
    
    // 防止递归传送的标记（坑 #5）
    private final ConcurrentHashMap<UUID, String> enteringPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> exitingPlayers = new ConcurrentHashMap<>();

    public SandboxService(CreativeBoxPlugin plugin, 
                          SandboxWorldManager worldManager, PlayerStateStore stateStore) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.stateStore = stateStore;
    }

    /**
     * 进入沙盒的完整流程（坑 #5 顺序）：
     * 1. 幂等检查 → 2. 构造快照 → 3. state=IN_SANDBOX → 4. 先落盘 store.save(uuid) 
     * 5. 内存标记 → 6. inventory.clear() + enderChest.clear()
     * 7. 清空药水效果 → 8. teleport(sandboxSpawn) → 9. CREATIVE + allowFlight(true) + flying(true)
     * 10. 重置 health/food → 11. remove marking → 12. send enter message
     */
    public void enter(Player player) {
        UUID uuid = player.getUniqueId();

        // 1. 幂等检查：已在沙盒则返回
        PlayerSnapshot snap = stateStore.load(uuid);
        if (snap != null && snap.state == PlayerSnapshot.State.IN_SANDBOX) {
            plugin.getLogger().info("Player " + player.getName() + " already in sandbox (idempotent).");
            return;
        }

        // 2. 构造快照（保存全部可变状态）
        snap = PlayerSnapshot.fromPlayer(player);

        // 3-4. state=IN_SANDBOX → 先落盘
        snap.state = PlayerSnapshot.State.IN_SANDBOX;
        stateStore.save(snap);
        plugin.getLogger().info("Saved snapshot for " + player.getName() + " (state=IN_SANDBOX).");

        // 5. 内存标记（防递归）
        enteringPlayers.put(uuid, "entering");

        // 6. 清空背包和末影箱（内容已存盘）
        player.getInventory().clear();
        player.getEnderChest().getContents(new ItemStack[0]);
        // 正确清空末影箱：用 setContents(null array)
        player.getEnderChest().getInventory().setContents(new ItemStack[27]);

        // 7. 清空所有药水效果
        for (org.bukkit.potion.PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }

        // 8. 传送到沙盒出生点（坑 #5: teleport 前设置标记）
        Location spawn = worldManager.getSpawnLocation();
        if (spawn != null) {
            player.teleport(spawn);
        } else {
            plugin.getLogger().warning("Sandbox spawn is null! Using main world spawn.");
            Player mainWorldPlayer = Bukkit.getWorlds().isEmpty() ? player : 
                    Bukkit.getWorld(Bukkit.getWorlds().get(0).getName()).getSpawnLocation() != null ? player : player;
            player.teleport(Bukkit.getWorlds().get(0));
        }

        // 9. 设置创造模式 + 飞行（坑 #4: allowFlight → flying）
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        // 10. 重置 health/food 到满值（创造模式不需要饿）
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(100);

        // 11. remove entering marking
        enteringPlayers.remove(uuid);

        // 12. send enter message
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "&8[&b沙盒&8] ");
        String msg = config.getString("messages.enter", "&a已进入创意沙盒世界");
        player.sendMessage ChatColor.translateAlternateColorCodes('&', prefix + msg);

        plugin.getLogger().info("Player " + player.getName() + " entered sandbox.");
    }

    /**
     * 退出沙盒的完整流程（坑 #6 顺序）：
     * 1. state=NONE → store.save(uuid, ...) → remove marking → send exit message
     */
    public void exit(Player player) {
        UUID uuid = player.getUniqueId();

        // 幂等检查
        PlayerSnapshot snap = stateStore.load(uuid);
        if (snap == null || snap.state != PlayerSnapshot.State.IN_SANDBOX) {
            plugin.getLogger().info("Player " + player.getName() + " not in sandbox.");
            return;
        }

        // 记录 exiting 标记（GameModeChangeEvent 要放行）
        exitingPlayers.put(uuid, "exiting");

        // teleport back to origin first
        Location origin = new Location(
                Bukkit.getWorld(snap.originWorld),
                snap.originX, snap.originY, snap.originZ,
                snap.originYaw, snap.originPitch);
        
        if (origin.getWorld() != null) {
            player.teleport(origin);
        } else {
            // 原世界可能已卸载，传到第一个世界
            World first = Bukkit.getWorlds().isEmpty() ? null : 
                    Bukkit.getWorld(Bukkit.getWorlds().get(0).getName());
            if (first != null) {
                player.teleport(first.getSpawnLocation());
            } else {
                plugin.getLogger().severe("No worlds available for player " + player.getName() + "!");
            }
        }

        // 恢复游戏模式（不是退出流程中）
        player.setGameMode(snap.gameMode != null ? snap.gameMode : GameMode.SURVIVAL);
        
        // 飞行状态：不回存，玩家回主世界还能自己飞
        player.setAllowFlight(false);
        player.setFlying(false);

        // 恢复背包（坑 #2: setContents()）
        ItemStack[] inv = stateStore.deserializeItemsFromBase64(snap.inventoryBase64);
        player.getInventory().setContents(inv);

        // 恢复末影箱
        ItemStack[] ec = stateStore.deserializeItemsFromBase64(snap.enderChestBase64);
        player.getEnderChest().getInventory().setContents(ec);

        // 恢复经验：先 setLevel 再 setExp（坑 #3）
        player.setLevel(snap.level);
        player.setExp(Math.max(0, Math.min(1, snap.exp)));

        // 恢复 health/food/saturation/fire/freeze
        player.setHealth(Math.min(snap.health, player.getMaxHealth()));
        player.setFoodLevel(Math.max(0, snap.foodLevel));
        player.setSaturation(Math.max(0, snap.saturation));
        player.setFireTicks(snap.fireTicks);
        
        try {
            player.setFreezeTicks(snap.freezeTicks);
        } catch (Exception ignored) {}

        // 恢复药水效果：先 remove 所有，再逐个 add（坑 #8）
        for (org.bukkit.potion.PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }
        for (PlayerSnapshot.PotionEffectData ped : snap.potionEffects) {
            try {
                PlayerEffect effect = ped.toPotionEffect();
                if (effect != null) player.addPotionEffect(effect);
            } catch (Exception ignored) {}
        }

        // state=NONE → save
        snap.state = PlayerSnapshot.State.NONE;
        stateStore.save(snap);

        // remove exiting marking
        exitingPlayers.remove(uuid);

        // send exit message
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "&8[&b沙盒&8] ");
        String msg = config.getString("messages.exit", "&e已返回原世界");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));

        plugin.getLogger().info("Player " + player.getName() + " exited sandbox.");
    }

    /** 玩家是否在沙盒内 */
    public boolean isInSandbox(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerSnapshot snap = stateStore.load(uuid);
        return snap != null && snap.state == PlayerSnapshot.State.IN_SANDBOX;
    }

    /** 正在进入中（防递归标记） */
    public boolean isEntering(Player player) {
        return enteringPlayers.containsKey(player.getUniqueId());
    }

    /** 正在退出中（GameModeChange 放行标记） */
    public boolean isExiting(Player player) {
        return exitingPlayers.containsKey(player.getUniqueId());
    }
}
