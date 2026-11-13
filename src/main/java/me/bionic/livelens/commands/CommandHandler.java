package me.bionic.livelens.commands;

import me.bionic.livelens.CameraManager;
import me.bionic.livelens.LiveLens;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bionic.livelens.stream.StreamManager;

public class CommandHandler implements CommandExecutor {

    private final LiveLens plugin;

    public CommandHandler(LiveLens plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限（控制台和 OP 玩家自动通过）
        if (!sender.hasPermission("livelens.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        CameraManager cameraManager = plugin.getCameraManager();
        boolean isPlayer = sender instanceof Player;

        if (args.length == 0) {
            // Show help message
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "god":
                cameraManager.setMode(CameraManager.CameraMode.GOD_VIEW, isPlayer ? (Player) sender : null);
                if (isPlayer) {
                    ((Player) sender).sendMessage(ChatColor.GREEN + "Camera switched to God View! (Third-person follow mode)");
                }
                break;

            case "first":
                cameraManager.setMode(CameraManager.CameraMode.FIRST_PERSON, isPlayer ? (Player) sender : null);
                if (isPlayer) {
                    ((Player) sender).sendMessage(ChatColor.GREEN + "Camera switched to First Person Mode! (Follow player's eyes with inventory display)");
                }
                break;

            case "orbit":
                cameraManager.setMode(CameraManager.CameraMode.ORBIT_VIEW, isPlayer ? (Player) sender : null);
                if (isPlayer) {
                    ((Player) sender).sendMessage(ChatColor.GREEN + "Camera switched to Orbit View! (Rotating around the player)");
                }
                break;

            case "status":
                sender.sendMessage(ChatColor.YELLOW + "=== LiveLens Status ===");
                sender.sendMessage(ChatColor.WHITE + "Current mode: " + ChatColor.GRAY + cameraManager.getCurrentMode().name());
                
                Player focusPlayer = plugin.getPlayerTracker().getMostActivePlayer();
                if (focusPlayer != null) {
                    sender.sendMessage(ChatColor.WHITE + "Tracking player: " + ChatColor.AQUA + focusPlayer.getName());
                } else {
                    sender.sendMessage(ChatColor.WHITE + "Tracking player: " + ChatColor.GRAY + "None");
                }
                
                // Show current camera location if available
                var camLoc = cameraManager.getCameraLocation();
                if (camLoc != null) {
                    sender.sendMessage(ChatColor.WHITE + "Camera position: " + 
                        ChatColor.GOLD + String.format("%.1f, %.1f, %.1f", 
                            camLoc.getX(), camLoc.getY(), camLoc.getZ()));
                } else {
                    sender.sendMessage(ChatColor.WHITE + "Camera position: " + ChatColor.GRAY + "Not initialized yet");
                }

                // Show livestream status
                if (isPlayer) {
                    Player player = (Player) sender;
                    me.bionic.livelens.stream.LiveSession session = plugin.getStreamManager().getSession(player);
                    if (session != null) {
                        sender.sendMessage(ChatColor.WHITE + "Live stream: " + ChatColor.GREEN + "ON");
                        sender.sendMessage(ChatColor.WHITE + "  Provider: " + session.getProvider().getDisplayName());
                        sender.sendMessage(ChatColor.WHITE + "  Resolution: " + session.getWidth() + "x" + session.getHeight() + "@" + session.getFps() + "fps");
                        sender.sendMessage(ChatColor.WHITE + "  Uptime: " + String.format("%.0f seconds", session.getUptimeSeconds()));
                    } else {
                        sender.sendMessage(ChatColor.WHITE + "Live stream: " + ChatColor.RED + "OFF");
                    }
                }
                
                // Show online players count
                sender.sendMessage(ChatColor.WHITE + "Online players: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
                break;

            case "switch":
                // Toggle between modes (God <-> First -> Orbit)
                CameraManager.CameraMode currentMode = cameraManager.getCurrentMode();
                CameraManager.CameraMode nextMode;
                if (currentMode == CameraManager.CameraMode.GOD_VIEW) {
                    nextMode = CameraManager.CameraMode.FIRST_PERSON;
                } else if (currentMode == CameraManager.CameraMode.FIRST_PERSON) {
                    nextMode = CameraManager.CameraMode.ORBIT_VIEW;
                } else {
                    // Orbit -> God View (cycle through all 3)
                    nextMode = CameraManager.CameraMode.GOD_VIEW;
                }
                cameraManager.setMode(nextMode, isPlayer ? (Player) sender : null);
                if (isPlayer) {
                    ((Player) sender).sendMessage(ChatColor.GREEN + "Camera switched to " + nextMode.name() + "!");
                }
                break;

            case "live":
                // Livestream subcommands: start, stop, status
                handleLiveCommand(sender, args.length > 1 ? args[1].toLowerCase() : "");
                break;

            case "help":
                sendHelpMessage(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /livelens help for usage.");
                break;
        }

        return true;
    }

    /** 处理直播子命令 */
    private void handleLiveCommand(CommandSender sender, String action) {
        if (!sender.hasPermission("livelens.stream.start")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to start a livestream!");
            return;
        }

        boolean isPlayer = sender instanceof Player;
        StreamManager streamMgr = plugin.getStreamManager();

        switch (action) {
            case "start":
                if (!isPlayer) {
                    sender.sendMessage(ChatColor.RED + "Only players can start a livestream.");
                    return;
                }
                Player player = (Player) sender;
                // 检查是否已经有追踪目标（直播需要 CameraManager 初始化）
                if (plugin.getPlayerTracker().getMostActivePlayer() == null) {
                    player.sendMessage(ChatColor.RED + "No target player found. Ensure at least one player is online.");
                    return;
                }
                streamMgr.startLive(player);
                break;

            case "stop":
                if (!isPlayer) {
                    sender.sendMessage(ChatColor.RED + "Only players can stop their livestream.");
                    return;
                }
                streamMgr.stopLive((Player) sender);
                break;

            default:
                // 如果 live 后面没有参数，显示直播帮助
                sendLivestreamHelp(sender);
                break;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== LiveLens Commands ===");
        sender.sendMessage(ChatColor.WHITE + "/livelens god" + ChatColor.GRAY + " - Switch to God View (third-person camera) [perm: livelens.use]");
        sender.sendMessage(ChatColor.WHITE + "/livelens first" + ChatColor.GRAY + " - Switch to First Person Mode (follow eyes with inventory display) [perm: livelens.use]");
        sender.sendMessage(ChatColor.WHITE + "/livelens orbit" + ChatColor.GRAY + " - Switch to Orbit View (rotating around the player) [perm: livelens.use]");
        sender.sendMessage(ChatColor.WHITE + "/livelens switch" + ChatColor.GRAY + " - Cycle through camera modes [perm: livelens.use]");
        sender.sendMessage(ChatColor.WHITE + "/livelens status" + ChatColor.GRAY + " - Show current camera and tracking info [perm: livelens.use]");
        sender.sendMessage(ChatColor.WHITE + "/livelens live" + ChatColor.GRAY + " - Livestream subcommands (start/stop/status)");
        sender.sendMessage(ChatColor.WHITE + "/livelens help" + ChatColor.GRAY + " - Show this help message [perm: livelens.use]");
    }

    private void sendLivestreamHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== LiveLens Livestream Commands ===");
        sender.sendMessage(ChatColor.WHITE + "/livelens live start" + ChatColor.GRAY + " - Start streaming to Bilibili/Douyin [perm: livelens.stream.start]");
        sender.sendMessage(ChatColor.WHITE + "/livelens live stop" + ChatColor.GRAY + " - Stop current livestream [perm: livelens.stream.start]");
        sender.sendMessage(ChatColor.WHITE + "/livelens status" + ChatColor.GRAY + " - Check stream status");
    }
}