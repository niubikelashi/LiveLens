package com.bionic.creativebox.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * /sb 指令处理器 + tab 补全。
 * 
 * 坑 #8: 控制台也可以使用（不强制 sender instanceof Player）。
 */
public class SandboxCommand implements CommandExecutor, TabCompleter {

    private final CreativeBoxPlugin plugin;
    
    // 1 秒冷却
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SandboxCommand(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /sb [reload|setspawn|reset] 管理员命令
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    return cmdReload(sender);
                case "setspawn":
                    return cmdSetSpawn(sender);
                case "reset":
                    return cmdReset(sender);
                default:
                    // 尝试当玩家名处理：/sb <player>（管理员版）
                    return cmdTeleportPlayer(sender, args[0]);
            }
        }

        // /sb — 进入或退出沙盒
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "&8[&b沙盒&8] ") + "控制台不支持直接进出沙盒，请使用 /sb <玩家>。"));
            return true;
        }

        Player player = (Player) sender;
        
        // 权限检查（坑 #9: 默认所有玩家都有）
        if (!player.hasPermission("creativebox.use")) {
            sendNoPerm(player);
            return true;
        }

        // 冷却检查
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null && now - lastUse < 1000) {
            sendCooldown(player);
            return true;
        }

        // enter/exit toggle
        SandboxService svc = plugin.getSandboxService();
        if (svc.isInSandbox(player)) {
            svc.exit(player);
        } else {
            svc.enter(player);
        }

        cooldowns.put(uuid, now);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender instanceof Player && !((Player) sender).hasPermission("creativebox.admin")) {
            return Collections.emptyList(); // 非 admin 只看到 sb
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String s : new String[]{"reload", "setspawn", "reset"}) {
                if (s.startsWith(partial)) completions.add(s);
            }
            // 列出在线玩家名（管理员可以传送到沙盒）
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial) && !p.getName().equals(sender.getName())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }

    private boolean cmdReload(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) {
            sendNoPerm(sender); return true;
        }
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.reload", "")));
        plugin.getLogger().info("Config reloaded by " + (sender instanceof Player ? ((Player)sender).getName() : "CONSOLE"));
        return true;
    }

    private boolean cmdSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) {
            sendNoPerm(sender); return true;
        }
        Player player = (Player) sender;
        Location loc = player.getLocation();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", loc.getYaw());
        config.set("spawn.pitch", loc.getPitch());
        
        // 更新沙盒出生点
        plugin.getWorldManager().initializeSandbox();
        plugin.saveConfig();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.setspawn-done", "")));
        return true;
    }

    private boolean cmdReset(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) {
            sendNoPerm(sender); return true;
        }
        
        plugin.getLogger().info("Resetting sandbox world...");
        SandboxService svc = plugin.getSandboxService();
        boolean ok = plugin.getWorldManager().resetSandboxWorld(svc);
        
        // 重新初始化世界（需要玩家下次进入时生效）
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + "&e沙盒世界已重置，下次 /sb 生效。"));
        return ok;
    }

    private boolean cmdTeleportPlayer(CommandSender sender, String playerName) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) {
            sendNoPerm(sender); return true;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.player-not-found", "")));
            return true;
        }

        SandboxService svc = plugin.getSandboxService();
        if (svc.isInSandbox(target)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + target.getName() + " 已在沙盒中"));
        } else {
            svc.enter(target);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + "&e已将 " + target.getName() + " 送入沙盒"));
        }
        return true;
    }

    private void sendNoPerm(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
    }

    private void sendCooldown(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.cooldown", "")));
    }
}
