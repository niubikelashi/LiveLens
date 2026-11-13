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

    // Smooth movement related variables
    private Location cameraLocation;
    private double orbitAngle = 0.0; // Angle for orbit view rotation

    public CameraManager(LiveLens plugin, PlayerTracker playerTracker) {
        this.plugin = plugin; 
        this.playerTracker = playerTracker;
    }

    /**
     * Switch camera mode. Only broadcast to the caller to avoid server-wide spam.
     */
    public void setMode(CameraMode mode, Player sender) { 
        this.currentMode = mode; 
        if (sender != null) {
            sender.sendMessage("§aCamera switched to: " + mode.name());
        }
    }

    private double lerp(double start, double end, double factor) {
        return start + (end - start) * Math.min(Math.max(factor, 0.0), 1.0);
    }

    public void update() {
        focusPlayer = playerTracker.getMostActivePlayer();
        if (focusPlayer == null) return;

        // Get parameters from config
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
            // Use getEyeLocation() directly — returns the eye position (including Y)
            targetLoc = focusPlayer.getEyeLocation().clone();
        } else if (currentMode == CameraMode.ORBIT_VIEW) {
            double rotationSpeed = plugin.getConfig().getDouble("camera.orbit-view.rotation-speed", 0.02);

            // Update rotation angle
            orbitAngle += rotationSpeed;

            // Calculate orbital position (using trigonometry), centered on player, height above head
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
            // Fallback to player's location
            targetLoc = focusPlayer.getLocation().clone();
        }

        // Initialize cameraLocation on first call
        if (cameraLocation == null) {
            cameraLocation = targetLoc.clone();
        }

        // Smooth movement logic (Lerp)
        double newX = lerp(cameraLocation.getX(), targetLoc.getX(), smoothSpeed);
        double newY = lerp(cameraLocation.getY(), targetLoc.getY(), smoothSpeed);
        double newZ = lerp(cameraLocation.getZ(), targetLoc.getZ(), smoothSpeed);
        
        cameraLocation.setX(newX);
        cameraLocation.setY(newY);
        cameraLocation.setZ(newZ);

        // Smooth rotation (Yaw/Pitch) — pitch doesn't flip at 360°, only clamped to range
        float newYaw = lerpAngle(cameraLocation.getYaw(), targetLoc.getYaw());
        float newPitch = (float) lerp(cameraLocation.getPitch(), targetLoc.getPitch(), smoothSpeed);
        // Clamp pitch to [-90, 90] to avoid flipping
        if (newPitch > 90.0f) newPitch = 90.0f;
        if (newPitch < -90.0f) newPitch = -90.0f;
        
        cameraLocation.setYaw(newYaw);
        cameraLocation.setPitch(newPitch);

        // 【关键】Project the calculated camera position to the tracked player's view
        // setCamera is Paper API — lets the target player see custom camera view
        Player viewer = focusPlayer;
        if (viewer != null && cameraLocation.getWorld() != null) {
            try {
                viewer.setCamera(cameraLocation);
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                // setCamera not available on this server version — fallback to player's eye location
                viewer.setEyeOffset(new org.bukkit.util.Vector(0, 0, 0));
            }
        }
    }

    /**
     * Angle interpolation: uses shortest path (±180°), fixed factor as smoothSpeed.
     */
    private float lerpAngle(float start, float end) {
        double smoothSpeed = plugin.getConfig().getDouble("camera.smooth-speed", 0.1);
        
        // Calculate shortest angle difference (-180 ~ +180)
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