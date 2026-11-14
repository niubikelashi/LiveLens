package com.bionic.creativebox.listener;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 玩家事件监听器：
 * - JoinEvent: 崩服恢复（坑 #3: 登录时读文件，IN_SANDBOX → 放回沙盒）
 * - QuitEvent: 只存档不做传送（坑 #8）
 * - RespawnEvent: 沙盒内死亡在出生点复活（坑 #4）
 * - BedEnterEvent: 取消沙盒内睡觉（坑 #6）
 * - GameModeChangeEvent: 沙盒内非创造则取消（坑 #7, 退出流程放行）
 * - MoveEvent: 虚空保护 + 节流（坑 #10）
 */
public class PlayerListener implements Listener {

    private final CreativeBoxPlugin plugin;

    public PlayerListener(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    /** 玩家上次移动时间戳（tick），用于节流虚空检测（坑 #10） */
    private final java.util.Map<java.util.UUID, Long> lastVoidCheckTime = new java.util.HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 崩服恢复：读取数据文件，如果 state=IN_SANDBOX → 直接放回沙盒（坑 #3）
        var svc = plugin.getSandboxService();
        if (svc.isInSandbox(player)) {
            World spawn = plugin.getWorldManager().getSandboxWorld();
            if (spawn != null) {
                Location spawnLoc = plugin.getWorldManager().getSpawnLocation();
                player.teleport(spawnLoc);
                player.setGameMode(GameMode.CREATIVE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.prefix", "&8[&b沙盒&8] ") + "&e崩服恢复：已把你送回创意沙盒"));
            } else {
                // 世界可能还没加载，延迟重连
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (plugin.getWorldManager().getSandboxWorld() != null && 
                                svc.isInSandbox(player)) {
                            player.teleport(plugin.getWorldManager().getSpawnLocation());
                            player.setGameMode(GameMode.CREATIVE);
                            player.setAllowFlight(true);
                            player.setFlying(true);
                        }
                    }
                }.runTaskLater(plugin, 20L); // wait 1 tick for world to load
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        var svc = plugin.getSandboxService();
        if (svc.isInSandbox(player)) {
            // 只存档，不恢复背包（坑 #8）
            plugin.getLogger().info("Player " + player.getName() + " quit while in sandbox. Snapshot saved.");
            // state stays IN_SANDBOX — on rejoin, PlayerJoinEvent handles restoration
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                player.getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            // 沙盒内死亡：重生点在出生点（坑 #4）
            Location spawnLoc = plugin.getWorldManager().getSpawnLocation();
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
                
                FileConfiguration config = plugin.getConfig();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        config.getString("messages.prefix", "") + config.getString("messages.respawn", "")));
                plugin.getLogger().info("Player " + player.getName() + " respawned at sandbox spawn.");
            }
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                player.getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            // 取消沙盒内睡觉（坑 #6）
            event.setCancelled(true);
            plugin.getLogger().info("Player " + player.getName() + " bed entry cancelled in sandbox.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                player.getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            var svc = plugin.getSandboxService();
            
            // 退出流程中放行（坑 #7）
            if (svc.isExiting(player)) return;

            // 非创造模式则取消（坑 #7）
            if (event.getNewGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                plugin.getLogger().info("Player " + player.getName() + " game mode change to " 
                        + event.getNewGameMode() + " cancelled in sandbox.");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                player.getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            
            // 虚空保护（节流，坑 #10）
            FileConfiguration config = plugin.getConfig();
            boolean voidEnabled = config.getBoolean("void-protection.enabled", true);
            if (!voidEnabled) return;

            double threshold = plugin.getWorldManager().getSpawnLocation().getY() 
                    - config.getDouble("void-protection.threshold", 10);

            // 每 10 tick 检查一次（坑 #10: 节流）
            long now = System.currentTimeMillis();
            Long lastCheck = lastVoidCheckTime.get(player.getUniqueId());
            if (lastCheck != null && now - lastCheck < 500) return; // ~10 ticks at ~50ms/tick

            if (player.getLocation().getY() < threshold) {
                Location spawn = plugin.getWorldManager().getSpawnLocation();
                player.teleport(spawn);
                plugin.getLogger().info("Player " + player.getName() + " pulled back from void.");
            }

            lastVoidCheckTime.put(player.getUniqueId(), now);
        }
    }
}
