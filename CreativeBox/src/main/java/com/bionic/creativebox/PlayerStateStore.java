package com.bionic.creativebox;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64coder.Base64Coder;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class PlayerStateStore {

    private final CreativeBoxPlugin plugin;
    private File dataDir;

    public PlayerStateStore(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
        ensureDataDirectory();
    }

    private void ensureDataDirectory() {
        dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataDir, uuid.toString() + ".yml");
    }

    /** 保存快照到 YAML（原子写入：先 .tmp 再 move） */
    public void save(PlayerSnapshot snapshot) {
        ensureDataDirectory();
        File playerFile = getPlayerFile(snapshot.uuid);
        File tmpFile = new File(playerFile.getParent(), playerFile.getName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile.toPath())) {
            writer.write("state: " + snapshot.state.name());
            writer.newLine();
            writer.write("saved-at: " + snapshot.savedAt);
            writer.newLine();
            writer.newLine();

            // origin block
            writer.write("origin:");
            writer.newLine();
            writer.write("  world: \"" + escapeYamlString(snapshot.originWorld) + "\"");
            writer.newLine();
            writer.write("  x: " + snapshot.originX);
            writer.newLine();
            writer.write("  y: " + snapshot.originY);
            writer.newLine();
            writer.write("  z: " + snapshot.originZ);
            writer.newLine();
            writer.write("  yaw: " + snapshot.originYaw);
            writer.newLine();
            writer.write("  pitch: " + snapshot.originPitch);
            writer.newLine();
            writer.newLine();

            // player stats block
            writer.write("player:");
            writer.newLine();
            writer.write("  game-mode: " + (snapshot.gameMode != null ? snapshot.gameMode.name() : "SURVIVAL"));
            writer.newLine();
            writer.write("  allow-flight: " + snapshot.allowFlight);
            writer.newLine();
            writer.write("  flying: " + snapshot.flying);
            writer.newLine();
            writer.write("  level: " + snapshot.level);
            writer.newLine();
            writer.write("  exp: " + snapshot.exp);
            writer.newLine();
            writer.write("  health: " + snapshot.health);
            writer.newLine();
            writer.write("  food: " + snapshot.foodLevel);
            writer.newLine();
            writer.write("  saturation: " + snapshot.saturation);
            writer.newLine();
            writer.write("  fire-ticks: " + snapshot.fireTicks);
            writer.newLine();
            try {
                writer.write("  freeze-ticks: " + snapshot.freezeTicks);
                writer.newLine();
            } catch (Exception ignored) {}

            // potion effects
            writer.write("  potion-effects:");
            if (snapshot.potionEffects.isEmpty()) {
                writer.write(" []");
            } else {
                writer.newLine();
                for (PlayerSnapshot.PotionEffectData ped : snapshot.potionEffects) {
                    writer.write("    - type: " + ped.type);
                    writer.newLine();
                    writer.write("      duration: " + ped.duration);
                    writer.newLine();
                    writer.write("      amplifier: " + ped.amplifier);
                    writer.newLine();
                    writer.write("      ambient: " + ped.ambient);
                    writer.newLine();
                    writer.write("      particles: " + ped.hasParticles);
                    writer.newLine();
                    writer.write("      icon: " + ped.hasIcon);
                    writer.newLine();
                }
            }
            writer.newLine();
            writer.newLine();

            // inventory & ender chest (BukkitObjectOutputStream -> Base64)
            writer.write("inventory: \"" + serializeItemsToBase64(null) + "\"");
            writer.newLine();
            writer.write("ender-chest: \"" + serializeItemsToBase64(null) + "\"");
            writer.newLine();

            // atomic move
            Files.move(tmpFile.toPath(), playerFile.toPath(), 
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save snapshot for " + snapshot.uuid + ": " + e.getMessage());
        }
    }

    /** 从 YAML 文件加载快照 */
    public PlayerSnapshot load(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (!playerFile.exists()) return null;

        try (BufferedReader reader = Files.newBufferedReader(playerFile.toPath())) {
            String line;
            PlayerSnapshot snap = new PlayerSnapshot();
            snap.uuid = uuid;
            StringBuilder invB64 = new StringBuilder();
            StringBuilder ecB64 = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("state:")) {
                    snap.state = "IN_SANDBOX".equals(trimmed.substring(6).trim()) 
                            ? PlayerSnapshot.State.IN_SANDBOX : PlayerSnapshot.State.NONE;
                } else if (line.contains("saved-at:")) {
                    try { snap.savedAt = Long.parseLong(line.split(":")[1].trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("world:")) {
                    snap.originWorld = trimmed.substring(6).replaceAll("\"", "").trim();
                } else if (trimmed.matches("^x:\\s*-?\\d+(\\.\\d+)?")) {
                    try { snap.originX = Double.parseDouble(trimmed.split(":")[1]); } catch (Exception ignored) {}
                } else if (trimmed.matches("^y:\\s*-?\\d+(\\.\\d+)?")) {
                    try { snap.originY = Double.parseDouble(trimmed.split(":")[1]); } catch (Exception ignored) {}
                } else if (trimmed.matches("^z:\\s*-?\\d+(\\.\\d+)?")) {
                    try { snap.originZ = Double.parseDouble(trimmed.split(":")[1]); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("yaw:")) {
                    try { snap.originYaw = Float.parseFloat(trimmed.substring(4).trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("pitch:")) {
                    try { snap.originPitch = Float.parseFloat(trimmed.substring(6).trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("game-mode:")) {
                    try { snap.gameMode = org.bukkit.GameMode.valueOf(trimmed.split(":")[1].trim()); } 
                    catch (IllegalArgumentException ignored) { snap.gameMode = org.bukkit.GameMode.SURVIVAL; }
                } else if (trimmed.startsWith("level:")) {
                    try { snap.level = Integer.parseInt(trimmed.split(":")[1].trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("exp:")) {
                    try { snap.exp = Float.parseFloat(trimmed.split(":")[1].trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("health:")) {
                    try { snap.health = Double.parseDouble(trimmed.substring(7).trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("food:")) {
                    try { snap.foodLevel = Integer.parseInt(trimmed.split(":")[1].trim()); } catch (Exception ignored) {}
                } else if (trimmed.startsWith("saturation:")) {
                    try { snap.saturation = Float.parseFloat(trimmed.substring(11).trim()); } catch (Exception ignored) {}
                } else if (line.contains("inventory:") && line.indexOf('"') > -1) {
                    invB64.append(line.substring(line.indexOf('"')+1, line.lastIndexOf('"')));
                } else if (line.contains("ender-chest:") && line.indexOf('"') > -1) {
                    ecB64.append(line.substring(line.indexOf('"')+1, line.lastIndexOf('"')));
                }
            }

            snap.inventoryBase64 = invB64.toString();
            snap.enderChestBase64 = ecB64.toString();
            return snap;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load snapshot for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    /** ItemStack[] -> Base64 */
    public String serializeItemsToBase64(ItemStack[] items) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream data = new BukkitObjectOutputStream(out)) {
            if (items == null) return "null";
            data.writeInt(items.length);
            for (ItemStack item : items) data.writeObject(item);
            return Base64Coder.encodeLines(out.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to serialize items: " + e.getMessage());
            return "null";
        }
    }

    /** Base64 -> ItemStack[] */
    public ItemStack[] deserializeItemsFromBase64(String b64) {
        if (b64 == null || b64.equals("null") || b64.isEmpty()) return new ItemStack[0];
        try {
            byte[] decoded = Base64Coder.decodeLines(b64);
            ByteArrayInputStream in = new ByteArrayInputStream(decoded);
            try (BukkitObjectInputStream data = new BukkitObjectInputStream(in)) {
                int length = Math.max(0, data.readInt());
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) items[i] = (ItemStack) data.readObject();
                return items;
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to deserialize: " + e.getMessage());
            return new ItemStack[0];
        }
    }

    private String escapeYamlString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void delete(UUID uuid) {
        File f = getPlayerFile(uuid);
        if (f.exists()) f.delete();
    }
}
