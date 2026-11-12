package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerTracker {
    private final LiveLens plugin;
    private final Map<UUID, Double> activityScores = new HashMap<>();
    // 用于平滑分数：每隔一段时间衰减旧分数，避免永远累积
    private static final double DECAY_FACTOR = 0.95;
    // 活跃度阈值，低于此值视为"不活跃"
    private static final double INACTIVE_THRESHOLD = 0.1;

    public PlayerTracker(LiveLens plugin) { this.plugin = plugin; }

    /**
     * 更新所有在线玩家的活跃度分数。
     * 
     * 检测方式：
     * - 移动速度（走路/跑步）→ velocity.length() * factor
     * - 飞行 → bonus
     * - 跳跃（onGround=false，且未飞行）→ small bonus
     * - 坐矿车/船/马 → 检查 getPassenger() != null || hasVehicle()
     */
    public void updateActivity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            
            // 衰减旧分数，避免永远累积
            double currentScore = activityScores.getOrDefault(uuid, 0.0);
            currentScore *= DECAY_FACTOR;
            
            double scoreAddition = 0;
            
            // Movement speed contribution — 走路、跑步都算活跃
            double velocityMag = player.getVelocity().length();
            if (velocityMag > INACTIVE_THRESHOLD) {
                scoreAddition += velocityMag * 0.5;
            }
            
            // Flying contribution
            if (player.isFlying()) { 
                scoreAddition += 1.0; 
            }
            
            // Jumping contribution — 在地面且未飞行时，onGround=false 表示跳跃/攀爬
            else if (!player.isOnGround() && !player.isInsideBlock()) {
                scoreAddition += 0.3;
            }
            
            // Vehicle/passenger bonus — 坐矿车、船、马等会提高 velocity，但我们也给额外加分
            if (player.getVehicle() != null || !player.getPassengers().isEmpty()) {
                scoreAddition += 0.2;
            }

            activityScores.put(uuid, currentScore + scoreAddition);
        }
        
        // 清理离线玩家的分数 — 每 N tick 执行一次，减少开销
        if (Bukkit.getScheduler().isCurrentlyRunning(Bukkit.getScheduler().getMainTaskId())) {
            int taskId = Bukkit.getScheduler().getPrimaryThread();
            if (taskId % 100 == 0) { // 每 5 秒清理一次
                activityScores.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
            }
        } else {
            // 非主线程时每次都清理（安全）
            activityScores.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }
    }

    /**
     * 获取当前最活跃的玩家。
     * 如果所有玩家分数都很低，则返回任意一个在线玩家（确保至少有一个目标）。
     */
    public Player getMostActivePlayer() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) return null;
        
        // 使用 Iterator 避免创建新集合的开销
        Player bestPlayer = null;
        double highestScore = -1.0;
        
        for (Player player : onlinePlayers) {
            double score = activityScores.getOrDefault(player.getUniqueId(), 0.0);
            
            if (score > highestScore) {
                highestScore = score;
                bestPlayer = player;
            }
        }
        
        // 如果最高分仍很低（比如刚启动），返回第一个在线玩家
        if (highestScore < INACTIVE_THRESHOLD && bestPlayer == null) {
            return onlinePlayers.iterator().next();
        }
        
        return bestPlayer;
    }
}
