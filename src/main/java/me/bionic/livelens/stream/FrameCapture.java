package me.bionic.livelens.stream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockSnapshot;
import org.bukkit.block.data.BlockData;

/** 帧捕获器 — 从 Minecraft 世界中捕获画面。 */
public class FrameCapture {

    /**
     * 从世界快照中捕获帧的像素数据（RGBA）。
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

    private static byte[] getBlockColor(World world, int x, int y, int z) {
        if (y < 0 || y >= 256) return new byte[]{76, 175, 80, (byte)255}; // grass color fallback

        BlockSnapshot snapshot = world.captureState().getBlock(x, y, z);
        if (snapshot == null) {
            return new byte[]{0, 0, 0, 255};
        }

        BlockData data = snapshot.getData();
        if (data != null && !data.getMaterial().isAir()) {
            return getBlockMaterialColor(data);
        }
        return new byte[]{0, 0, 0, 255};
    }

    /** 根据方块材质映射到近似颜色 [R, G, B, A]。 */
    private static byte[] getBlockMaterialColor(BlockData data) {
        String material = data.getMaterial().name();
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