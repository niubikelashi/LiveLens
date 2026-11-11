package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerTracker {
    private final LiveLens plugin;
    private final Map<UUID, Double> activityScores = new HashMap<>();

    public PlayerTracker(LiveLens plugin) { this.plugin = plugin; }

    public void updateActivity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double score = 0;
            score += player.getVelocity().length() * 0.5;
            if (player.getLastAttackTime() > 0) { score += 2.0; }
            if (player.isFlying()) { score += 1.0; }
            activityScores.put(player.getUniqueId(), activityScores.getOrDefault(player.getUniqueId(), 0.0) + score);
        }
    }

    public Player getMostActivePlayer() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return null;
        return activityScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> Bukkit.getPlayer(entry.getKey()))
            .orElse(null);
    }
}