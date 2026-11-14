package com.bionic.creativebox;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerSnapshot {

    public enum State { NONE, IN_SANDBOX }

    // 元数据
    public UUID uuid;
    public long savedAt;
    
    // 原世界位置（退出时传送回去）
    public String originWorld;
    public double originX, originY, originZ;
    public float originYaw, originPitch;

    // 玩家状态
    public org.bukkit.GameMode gameMode;
    public boolean allowFlight, flying;
    public int level;
    public float exp;       // [0,1) progress within current level
    
    // 生命/饥饿/氧气
    public double health;
    public int foodLevel;
    public float saturation;
    
    // 火焰/冰冻 ticks (1.17+)
    public int fireTicks, freezeTicks;

    // 背包（41格：36主+4盔甲+1副手）- Base64 编码
    public String inventoryBase64;
    
    // 末影箱（27格）
    public String enderChestBase64;

    // 药水效果列表
    public List<PotionEffectData> potionEffects = new ArrayList<>();

    // 沙盒状态标记
    public State state = State.NONE;

    /** 简化的药水效果数据 */
    public static class PotionEffectData {
        public String type;       // PotionType enum name
        public int duration, amplifier;
        public boolean ambient, hasParticles, hasIcon;

        public PotionEffectData() {}

        public PotionEffectData(org.bukkit.potion.PotionEffect pe) {
            this.type = pe.getType().getTypeName();
            this.duration = pe.getDuration();
            this.amplifier = pe.getAmplifier();
            this.ambient = pe.isAmbient();
            this.hasParticles = pe.hasParticles();
            this.hasIcon = pe.hasIcon();
        }

        public org.bukkit.potion.PotionEffect toPotionEffect() {
            try {
                return new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionType.valueOf(type.toUpperCase()), 
                    duration, amplifier, ambient, hasParticles, hasIcon);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /** 从 Player 构造快照 */
    public static PlayerSnapshot fromPlayer(org.bukkit.entity.Player player) {
        PlayerSnapshot snap = new PlayerSnapshot();
        snap.uuid = player.getUniqueId();
        snap.savedAt = System.currentTimeMillis();

        org.bukkit.Location loc = player.getLocation();
        snap.originWorld = loc.getWorld().getName();
        snap.originX = loc.getX();
        snap.originY = loc.getY();
        snap.originZ = loc.getZ();
        snap.originYaw = loc.getYaw();
        snap.originPitch = loc.getPitch();

        snap.gameMode = player.getGameMode();
        snap.allowFlight = player.getAllowFlight();
        snap.flying = player.isFlying();
        snap.level = player.getLevel();
        snap.exp = player.getExp();
        snap.health = player.getHealth();
        snap.foodLevel = player.getFoodLevel();
        snap.saturation = player.getSaturation();
        snap.fireTicks = player.getFireTicks();

        try { snap.freezeTicks = player.getFreezeTicks(); } catch (Exception ignored) {}

        for (org.bukkit.potion.PotionEffect pe : player.getActivePotionEffects()) {
            snap.potionEffects.add(new PotionEffectData(pe));
        }

        return snap;
    }

    /** 是否为空快照 */
    public boolean isEmpty() { return uuid == null; }
}
