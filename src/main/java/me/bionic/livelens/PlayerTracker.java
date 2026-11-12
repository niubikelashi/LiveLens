package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerTracker {
    private final LiveLens plugin;
    private final Map<UUID, Double> activityScores = new HashMap<>();
    // 用于平滑分数：每隔一段时间衰减旧分数，避免永远累积
    private static final double DECAY_FACTOR = 0.95;

    public PlayerTracker(LiveLens plugin) { this.plugin = plugin; }

    public void updateActivity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // 衰减旧分数，避免永远累积
            double currentScore = activityScores.getOrDefault(uuid, 0.0);
            currentScore *= DECAY_FACTOR;
            
            double scoreAddition = 0;
            
            // Movement speed contribution — 走路、跑步、飞行都算活跃
            scoreAddition += player.getVelocity().length() * 0.5;
            
            // Jumping/Flying contribution
            if (player.isFlying()) { 
                scoreAddition += 1.0; 
            }

            activityScores.put(uuid, currentScore + scoreAddition);
        }
        
        // 清理离线玩家的分数
        activityScores.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    public Player getMostActivePlayer() {
        var onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return null;
        
        Player bestPlayer = null;
        double highestScore = -1.0;
        
        for (Player player : onlinePlayers) {
            double score = activityScores.getOrDefault(player.getUniqueId(), 0.0);
            // 至少给每个在线玩家一个基础分数，确保活跃玩家会被选中
            if (score <= highestScore && bestPlayer != null) continue;
            
            if (score > highestScore) {
                highestScore = score;
                bestPlayer = player;
            }
        }
        
        return bestPlayer;
    }
}