package me.bionic.livelens.commands;

import me.bionic.livelens.CameraManager;
import me.bionic.livelens.LiveLens;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final LiveLens plugin;

    public CommandHandler(LiveLens plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players in-game.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("livelens.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        CameraManager cameraManager = plugin.getCameraManager();

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "god":
                cameraManager.setMode(CameraManager.CameraMode.GOD_VIEW);
                player.sendMessage(ChatColor.GREEN + "Camera switched to God View! (Third-person follow mode)");
                break;

            case "first":
                cameraManager.setMode(CameraManager.CameraMode.FIRST_PERSON);
                player.sendMessage(ChatColor.GREEN + "Camera switched to First Person Mode! (Follow player's eyes with inventory display)");
                break;

            case "orbit":
                cameraManager.setMode(CameraManager.CameraMode.ORBIT_VIEW);
                player.sendMessage(ChatColor.GREEN + "Camera switched to Orbit View! (Rotating around the player)");
                break;

            case "status":
                player.sendMessage(ChatColor.YELLOW + "=== LiveLens Status ===");
                player.sendMessage(ChatColor.WHITE + "Current mode: " + ChatColor.GRAY + cameraManager.getCurrentMode().name());
                Player focusPlayer = plugin.getPlayerTracker().getMostActivePlayer();
                if (focusPlayer != null) {
                    player.sendMessage(ChatColor.WHITE + "Tracking player: " + ChatColor.AQUA + focusPlayer.getName());
                } else {
                    player.sendMessage(ChatColor.WHITE + "Tracking player: " + ChatColor.GRAY + "None");
                }
                var camLoc = cameraManager.getCameraLocation();
                if (camLoc != null) {
                    player.sendMessage(ChatColor.WHITE + "Camera position: " + 
                        ChatColor.GOLD + String.format("%.1f, %.1f, %.1f", 
                            camLoc.getX(), camLoc.getY(), camLoc.getZ()));
                } else {
                    player.sendMessage(ChatColor.WHITE + "Camera position: " + ChatColor.GRAY + "Not initialized yet");
                }
                player.sendMessage(ChatColor.WHITE + "Online players: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
                break;

            case "switch":
                CameraManager.CameraMode currentMode = cameraManager.getCurrentMode();
                if (currentMode == CameraManager.CameraMode.GOD_VIEW) {
                    cameraManager.setMode(CameraManager.CameraMode.FIRST_PERSON);
                    player.sendMessage(ChatColor.GREEN + "Camera switched to First Person Mode!");
                } else if (currentMode == CameraManager.CameraMode.FIRST_PERSON) {
                    cameraManager.setMode(CameraManager.CameraMode.GOD_VIEW);
                    player.sendMessage(ChatColor.GREEN + "Camera switched to God View!");
                } else {
                    if (currentMode == CameraManager.CameraMode.ORBIT_VIEW) {
                        cameraManager.setMode(CameraManager.CameraMode.GOD_VIEW);
                        player.sendMessage(ChatColor.GREEN + "Camera switched to God View!");
                    } else {
                        cameraManager.setMode(CameraManager.CameraMode.ORBIT_VIEW);
                        player.sendMessage(ChatColor.GREEN + "Camera switched to Orbit View!");
                    }
                }
                break;

            case "help":
                sendHelpMessage(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /livelens help for usage.");
                break;
        }

        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== LiveLens Commands ===");
        player.sendMessage(ChatColor.WHITE + "/livelens god" + ChatColor.GRAY + " - Switch to God View (third-person camera)");
        player.sendMessage(ChatColor.WHITE + "/livelens first" + ChatColor.GRAY + " - Switch to First Person Mode (follow eyes with inventory display)");
        player.sendMessage(ChatColor.WHITE + "/livelens orbit" + ChatColor.GRAY + " - Switch to Orbit View (rotating around the player)");
        player.sendMessage(ChatColor.WHITE + "/livelens switch" + ChatColor.GRAY + " - Toggle between camera modes");
        player.sendMessage(ChatColor.WHITE + "/livelens status" + ChatColor.GRAY + " - Show current camera and tracking info");
        player.sendMessage(ChatColor.WHITE + "/livelens help" + ChatColor.GRAY + " - Show this help message");
    }
}