package me.bionic.livelens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CameraManager {
    private final LiveLens plugin;
    private final PlayerTracker playerTracker;

    public enum CameraMode { GOD_VIEW, FIRST_PERSON }

    private CameraMode currentMode = CameraMode.GOD_VIEW;
    private Player focusPlayer = null;

    public CameraManager(LiveLens plugin, PlayerTracker playerTracker) {
        this.plugin = plugin; this.playerTracker = playerTracker;
    }

    public void setMode(CameraMode mode) { this.currentMode = mode; }

    public void update() {
        focusPlayer = playerTracker.getMostActivePlayer();
        if (focusPlayer == null) return;
        if (currentMode == CameraMode.GOD_VIEW) {
            double height = 10.0;
            Vector position = focusPlayer.getLocation().clone().add(0, height, 0);
            // God view logic: Position above player
        } else {
            // First person logic: At eye level of the player
        }
    }

    public CameraMode getCurrentMode() { return currentMode; }
}