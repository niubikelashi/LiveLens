package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CameraManager {
    private final LiveLens plugin;
    private final PlayerTracker playerTracker;

    public enum CameraMode { GOD_VIEW, FIRST_PERSON, ORBIT_VIEW }

    private CameraMode currentMode = CameraMode.GOD_VIEW;
    private Player focusPlayer = null;

    // 平滑移动相关变量
    private Location cameraLocation;
    private double orbitAngle = 0.0; // 用于轨道视角旋转的角度

    public CameraManager(LiveLens plugin, PlayerTracker playerTracker) {
        this.plugin = plugin; 
        this.playerTracker = playerTracker;
    }

    /**
     * 切换摄像机模式。仅对调用者广播，避免全服刷屏。
     */
    public void setMode(CameraMode mode, Player sender) { 
        this.currentMode = mode; 
        if (sender != null) {
            sender.sendMessage("\u00a7aCamera switched to: " + mode.name());
        }
    }

    private double lerp(double start, double end, double factor) {
        return start + (end - start) * Math.min(Math.max(factor, 0.0), 1.0);
    }

    public void update() {
        focusPlayer = playerTracker.getMostActivePlayer();
        if (focusPlayer == null) return;

        // 从配置中获取参数
        double smoothSpeed = plugin.getConfig().getDouble("camera.smooth-speed", 0.1);
        double orbitRadius = plugin.getConfig().getDouble("camera.orbit-view.radius", 10.0);
        float orbitHeightOffset = (float) plugin.getConfig().getDouble("camera.orbit-view.height", 5.0);

        Location targetLoc;
        
        if (currentMode == CameraMode.GOD_VIEW) {
            double godViewHeight = plugin.getConfig().getDouble("camera.god-view.height", 15.0);
            double backOffset = plugin.getConfig().getDouble("camera.god-view.back-offset", 8.0);
            
            Location loc = focusPlayer.getLocation();
            Vector offset = new Vector(0, godViewHeight, -backOffset); 
            targetLoc = loc.add(offset.rotateAroundY(Math.toRadians(-180 + loc.getYaw())));
        } else if (currentMode == CameraMode.FIRST_PERSON) {
            // 直接使用 getEyeLocation()，返回的就是眼睛位置（含 Y）
            targetLoc = focusPlayer.getEyeLocation().clone();
        } else if (currentMode == CameraMode.ORBIT_VIEW) {
            double rotationSpeed = plugin.getConfig().getDouble("camera.orbit-view.rotation-speed", 0.02);

            // 更新旋转角度
            orbitAngle += rotationSpeed;

            // 计算轨道位置 (基于三角函数)，以玩家为中心，高度在玩家头顶上方
            Location loc = focusPlayer.getLocation();
            double x = Math.cos(orbitAngle) * orbitRadius;
            double z = Math.sin(orbitAngle) * orbitRadius;
            
            targetLoc = new Location(loc.getWorld(), 
                loc.getX() + x, 
                loc.getY() + orbitHeightOffset, 
                loc.getZ() + z,
                loc.getYaw(), 
                loc.getPitch());
        } else {
            targetLoc = focusPlayer.getLocation().clone();
        }

        // 首次调用时初始化 cameraLocation
        if (cameraLocation == null) {
            cameraLocation = targetLoc.clone();
        }

        // 平滑移动逻辑 (Lerp)
        double newX = lerp(cameraLocation.getX(), targetLoc.getX(), smoothSpeed);
        double newY = lerp(cameraLocation.getY(), targetLoc.getY(), smoothSpeed);
        double newZ = lerp(cameraLocation.getZ(), targetLoc.getZ(), smoothSpeed);
        
        cameraLocation.setX(newX);
        cameraLocation.setY(newY);
        cameraLocation.setZ(newZ);

        // 平滑旋转 (Yaw/Pitch) — pitch 不做 360° 翻转，只钳制范围
        float newYaw = lerpAngle(cameraLocation.getYaw(), targetLoc.getYaw());
        float newPitch = lerp(cameraLocation.getPitch(), targetLoc.getPitch(), smoothSpeed);
        // pitch 限制在 [-90, 90]，避免翻转
        if (newPitch > 90.0f) newPitch = 90.0f;
        if (newPitch < -90.0f) newPitch = -90.0f;
        
        cameraLocation.setYaw(newYaw);
        cameraLocation.setPitch(newPitch);

        // 【关键】把计算好的摄像机位置投射到被追踪玩家的视角上
        // setCamera 是 Paper 1.20+ API，让目标玩家看到自定义摄像机画面
        Player viewer = focusPlayer;
        if (viewer != null && cameraLocation.getWorld() != null) {
            viewer.setCamera(cameraLocation);
        }
    }

    /**
     * 角度插值：使用最短路径（±180° 内），因子固定为 smoothSpeed。
     */
    private float lerpAngle(float start, float end) {
        double smoothSpeed = plugin.getConfig().getDouble("camera.smooth-speed", 0.1);
        
        // 计算最短角差 (-180 ~ +180)
        float diff = end - start;
        while (diff > 180.0f) diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;
        
        return start + diff * (float) Math.min(Math.max(smoothSpeed, 0.0), 1.0);
    }

    public CameraMode getCurrentMode() { 
        return currentMode; 
    }

    public Location getCameraLocation() {
        return cameraLocation;
    }
}