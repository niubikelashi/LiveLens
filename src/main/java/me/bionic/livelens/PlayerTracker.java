package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerTracker {
    private final LiveLens plugin;
    private final Map<UUID, Double> activityScores = new HashMap<>();
    
    // Decay factor for old scores — prevents permanent accumulation
    private static final double DECAY_FACTOR = 0.95;
    // Threshold below which a player is considered "inactive"
    private static final double INACTIVE_THRESHOLD = 0.1;

    // Cache the isInsideBlock method for performance
    private static java.lang.reflect.Method IS_INSIDE_BLOCK_METHOD;
    
    static {
        try {
            IS_INSIDE_BLOCK_METHOD = org.bukkit.entity.Player.class.getMethod("isInsideBlock");
        } catch (NoSuchMethodException e) {
            // Paper 1.21+ may not have this method — will fallback to false
        }
    }

    /**
     * Check if player is inside a block using reflection for compatibility.
     * Returns false if the method doesn't exist (e.g., Spigot, older Paper).
     */
    private static boolean isPlayerInsideBlock(Player player) {
        if (IS_INSIDE_BLOCK_METHOD != null) {
            try {
                return (boolean) IS_INSIDE_BLOCK_METHOD.invoke(player);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public PlayerTracker(LiveLens plugin) { this.plugin = plugin; }

    /**
     * Update activity scores for all online players.
     * 
     * Detection methods:
     * - Movement speed (walking/running) → velocity.length() * factor
     * - Flying → bonus
     * - Jumping (onGround=false, not flying) → small bonus  
     * - Riding vehicles (minecart, boat, horse) → check getPassenger() || hasVehicle()
     */
    public void updateActivity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // Decay old scores to avoid permanent accumulation
            double currentScore = activityScores.getOrDefault(uuid, 0.0);
            currentScore *= DECAY_FACTOR;
            
            double scoreAddition = 0;
            
            // Movement speed contribution — walking/running counts as active
            double velocityMag = player.getVelocity().length();
            if (velocityMag > INACTIVE_THRESHOLD) {
                scoreAddition += velocityMag * 0.5;
            }
            
            // Flying contribution
            if (player.isFlying()) { 
                scoreAddition += 1.0; 
            }
            
            // Jumping contribution — when on ground=false and not flying, indicates jump/climb
            // NOT on ground AND (NOT inside block OR method unavailable) — jump/climb detection
            else if (!player.isOnGround() && !isPlayerInsideBlock(player)) {
                scoreAddition += 0.3;
            }
            
            // Vehicle/passenger bonus — riding minecarts, boats, horses increases velocity, we give extra points
            if (player.getVehicle() != null || !player.getPassengers().isEmpty()) {
                scoreAddition += 0.2;
            }

            activityScores.put(uuid, currentScore + scoreAddition);
        }
        
        // Clean up offline players' scores periodically — every N ticks to reduce overhead
        activityScores.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    /**
     * Get the most active player.
     * If all players have low scores, returns any online player (ensures at least one target).
     */
    @SuppressWarnings("unchecked")
    public Player getMostActivePlayer() {
        Collection<Player> onlinePlayers = (Collection<Player>) (Collection<? extends Player>) Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return null;
        
        // Use Iterator to avoid creating new collections
        Player bestPlayer = null;
        double highestScore = -1.0;
        
        for (Player player : onlinePlayers) {
            double score = activityScores.getOrDefault(player.getUniqueId(), 0.0);
            
            if (score > highestScore) {
                highestScore = score;
                bestPlayer = player;
            }
        }
        
        // If highest score is still low (e.g., just started), return first online player
        if (highestScore < INACTIVE_THRESHOLD && bestPlayer == null) {
            return onlinePlayers.iterator().next();
        }
        
        return bestPlayer;
    }
}