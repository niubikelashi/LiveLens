package com.bionic.creativebox.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SandboxCommand implements CommandExecutor, TabCompleter {

    private final CreativeBoxPlugin plugin;
    
    // ConcurrentHashMap for thread-safe access
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public SandboxCommand(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload": return cmdReload(sender);
                case "setspawn": return cmdSetSpawn(sender);
                case "reset": return cmdReset(sender);
                default: return cmdTeleportPlayer(sender, args[0]);
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "&8[&b沙盒&8] ") + "Console cannot enter/exit sandbox directly. Use /sb <player>."));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("creativebox.use")) {
            sendNoPerm(player); return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.put(uuid, now);
        if (lastUse != null && now - lastUse < 1000) {
            sendCooldown(player); return true;
        }

        SandboxService svc = plugin.getSandboxService();
        if (svc.isInSandbox(player)) {
            svc.exit(player);
        } else {
            svc.enter(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender instanceof Player && !((Player) sender).hasPermission("creativebox.admin")) {
            return Collections.emptyList();
        }
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String s : new String[]{"reload", "setspawn", "reset"}) {
                if (s.startsWith(partial)) completions.add(s);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial) && !p.getName().equals(sender.getName())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }

    private boolean cmdReload(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) { sendNoPerm(sender); return true; }
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.reload", "")));
        return true;
    }

    private boolean cmdSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) { sendNoPerm(sender); return true; }
        Player player = (Player) sender;
        Location loc = player.getLocation();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", loc.getYaw());
        config.set("spawn.pitch", loc.getPitch());
        plugin.getWorldManager().initializeSandbox();
        plugin.saveConfig();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.setspawn-done", "")));
        return true;
    }

    private boolean cmdReset(CommandSender sender) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) { sendNoPerm(sender); return true; }
        SandboxService svc = plugin.getSandboxService();
        plugin.getWorldManager().resetSandboxWorld(svc);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix", "") + "&eSandbox world reset. Takes effect on next /sb."));
        return true;
    }

    private boolean cmdTeleportPlayer(CommandSender sender, String playerName) {
        if (!(sender instanceof Player) || !((Player) sender).hasPermission("creativebox.admin")) { sendNoPerm(sender); return true; }
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.player-not-found", "")));
            return true;
        }
        SandboxService svc = plugin.getSandboxService();
        if (svc.isInSandbox(target)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + target.getName() + " is already in sandbox."));
        } else {
            svc.enter(target);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix", "") + "&eSent " + target.getName() + " to sandbox."));
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
