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

    public void setMode(CameraMode mode) {
        this.currentMode = mode;
        Bukkit.broadcastMessage("§aCamera switched to: " + mode.name());
    }

    private double lerp(double start, double end, double factor) {
        return start + (end - start) * Math.min(factor, 1.0);
    }

    public void update() {
        focusPlayer = playerTracker.getMostActivePlayer();
        if (focusPlayer == null || cameraLocation == null) return;

        double smoothSpeed = plugin.getConfig().getDouble("camera.smooth-speed", 0.1);
        Location targetLoc;

        if (currentMode == CameraMode.GOD_VIEW) {
            double godViewHeight = plugin.getConfig().getDouble("camera.god-view.height", 15.0);
            double backOffset = plugin.getConfig().getDouble("camera.god-view.back-offset", 8.0);
            double yaw = focusPlayer.getLocation().getYaw();
            Vector offset = new Vector(0, godViewHeight, -backOffset);
            targetLoc = focusPlayer.getLocation().add(offset.rotateAroundY(Math.toRadians(-180 + yaw)));
        } else if (currentMode == CameraMode.FIRST_PERSON) {
            double eyeHeight = plugin.getConfig().getDouble("camera.first-person.eye-height", 1.6);
            targetLoc = focusPlayer.getEyeLocation().add(0, eyeHeight - focusPlayer.getLocation().getY(), 0);
        } else if (currentMode == CameraMode.ORBIT_VIEW) {
            double radius = plugin.getConfig().getDouble("camera.orbit-view.radius", 10.0);
            double height = plugin.getConfig().getDouble("camera.orbit-view.height", 5.0);
            double rotationSpeed = plugin.getConfig().getDouble("camera.orbit-view.rotation-speed", 0.02);
            orbitAngle += rotationSpeed;
            double x = Math.cos(orbitAngle) * radius;
            double z = Math.sin(orbitAngle) * radius;
            targetLoc = focusPlayer.getLocation().add(x, height, z);
        } else {
            targetLoc = focusPlayer.getLocation();
        }

        if (cameraLocation == null) cameraLocation = targetLoc.clone();

        double newX = lerp(cameraLocation.getX(), targetLoc.getX(), smoothSpeed);
        double newY = lerp(cameraLocation.getY(), targetLoc.getY(), smoothSpeed);
        double newZ = lerp(cameraLocation.getZ(), targetLoc.getZ(), smoothSpeed);
        cameraLocation.setX(newX);
        cameraLocation.setY(newY);
        cameraLocation.setZ(newZ);

        float newYaw = lerpAngle(cameraLocation.getYaw(), targetLoc.getYaw());
        float newPitch = lerpAngle(cameraLocation.getPitch(), targetLoc.getPitch());
        cameraLocation.setYaw(newYaw);
        cameraLocation.setPitch(newPitch);
    }

    private float lerpAngle(float start, float end) {
        float diff = Math.abs(end - start);
        if (diff > 180.0f) {
            if (end > start) { diff -= 360.0f; } else { diff += 360.0f; }
        }
        return (float) lerp(start, end, Math.abs(diff) * 0.1);
    }

    public CameraMode getCurrentMode() {
        return currentMode;
    }

    public Location getCameraLocation() {
        return cameraLocation;
    }
}