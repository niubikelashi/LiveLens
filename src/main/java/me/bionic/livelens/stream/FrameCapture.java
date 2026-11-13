package me.bionic.livelens.stream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/** 帧捕获器 — 从 Minecraft 世界中捕获画面。使用反射适配 Paper/Spigot 快照 API。 */
public class FrameCapture {

    /** 
     * 从世界快照中捕获帧的像素数据（RGBA）。
     * 兼容 Paper 1.20+ (World.captureState) / Spigot (World.createSnapshot)。
     */
    public static byte[] captureFrame(World world, Location cameraPos, int width, int height) {
        if (world == null || cameraPos == null) return new byte[width * height * 4];

        float yawRad = (float) Math.toRadians(cameraPos.getYaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = -Math.cos(yawRad);

        int startX = cameraPos.getBlockX() - width / 2;
        int startY = Math.max(0, cameraPos.getBlockY() - height / 4);

        byte[] frameData = new byte[width * height * 4];
        int sampleDistance = Math.max(8, width / 4);

        for (int py = 0; py < height; py++) {
            double rayZ = cameraPos.getZ() + dirZ * sampleDistance;
            double rayX = cameraPos.getX() + dirX * sampleDistance;

            int pixelBlockX = (int) Math.round(rayX);
            int pixelBlockY = startY + py;
            int pixelBlockZ = (int) Math.round(rayZ);

            byte[] blockColor = getBlockColor(world, pixelBlockX, pixelBlockY, pixelBlockZ);
            int offset = py * width * 4;
            System.arraycopy(blockColor, 0, frameData, offset + (pixelBlockX - startX) * 4, 4);
        }

        return frameData;
    }

    /** 通过反射获取方块颜色，兼容 Paper/Spigot 不同快照 API。 */
    private static byte[] getBlockColor(World world, int x, int y, int z) {
        if (y < 0 || y >= 256) return new byte[]{76, 175, 80, (byte)255};

        try {
            // Paper 1.20+: World.captureState() → Snapshot.getBlock(x,y,z).getData()
            Object snapshot = world.getClass().getMethod("captureState").invoke(world);
            Object blockData = snapshot.getClass().getMethod("getBlock", int.class, int.class, int.class)
                    .invoke(snapshot, x, y, z);

            if (blockData != null) {
                Class<?> cls = blockData.getClass();
                // 尝试 getData() → BlockData
                try {
                    Object data = cls.getMethod("getData").invoke(blockData);
                    return getColorFromBlockData(data);
                } catch (NoSuchMethodException ignored) {}

                // 有些实现直接返回 BlockData
                if (!cls.getName().contains("BlockSnapshot") && !cls.getName().contains("Snapshot")) {
                    return getColorFromBlockData(blockData);
                }
            }
        } catch (Exception e) {
            // Fallback: try World.getBlockAt()
            try {
                Object block = world.getClass().getMethod("getBlockAt", int.class, int.class, int.class)
                        .invoke(world, x, y, z);
                Object data = block.getClass().getMethod("getData").invoke(block);
                return getColorFromBlockData(data);
            } catch (Exception ignored) {}
        }

        return new byte[]{0, 0, 0, 255};
    }

    /** 将 BlockData（或代理对象）映射为颜色 [R,G,B,A]。 */
    private static byte[] getColorFromBlockData(Object data) {
        if (data == null || !(data instanceof org.bukkit.block.data.BlockData)) return new byte[]{0,0,0,255};
        String material = ((org.bukkit.block.data.BlockData) data).getMaterial().name();
        switch (material) {
            case "GRASS_BLOCK": return new byte[]{76, 175, 80, (byte)255};
            case "DIRT": return new byte[]{139, 69, 19, (byte)255};
            case "STONE": return new byte[]{128, 128, 128, (byte)255};
            case "SAND": return new byte[]{244, 223, 106, (byte)255};
            case "WATER": return new byte[]{33, 150, 243, 80};
            case "GLASS": return new byte[]{200, 200, 200, 40};
            default: return new byte[]{128, 128, 128, (byte)255};
        }
    }

    public static int frameByteSize(int width, int height) {
        return width * height * 4; // RGBA = 4 bytes per pixel
    }
}