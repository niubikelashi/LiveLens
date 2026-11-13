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
            else if (!player.isOnGround() && !player.isInsideBlock()) {
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