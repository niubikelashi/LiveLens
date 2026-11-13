package me.bionic.livelens.stream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockSnapshot;
import org.bukkit.block.data.BlockData;

/**
 * 帧捕获器 — 从 Minecraft 世界中捕获画面。
 * 
 * Paper/Leaves 1.20+ BlockSnapshot API:
 *   World.captureState() → 获取快照 (Paper 1.20+)
 *   Snapshot.getBlock(x, y, z) → BlockSnapshot
 *   snapshot.getData().getMaterial() / color mapping
 */
public class FrameCapture {

    /**
     * 从世界快照中捕获帧的像素数据（RGBA）。
     * 
     * @param world 要截取的 Minecraft 世界
     * @param cameraPos 相机位置（用于确定视角方向）
     * @param width 输出帧宽度
     * @param height 输出帧高度
     * @return RGBA 格式的字节数组，每像素4字节 (R, G, B, A=255)
     */
    public static byte[] captureFrame(World world, Location cameraPos, int width, int height) {
        if (world == null || cameraPos == null) return null;

        // 计算采样区域的起始位置（在相机前方）
        float yawRad = (float) Math.toRadians(cameraPos.getYaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = -Math.cos(yawRad);

        int startX = cameraPos.getBlockX() - width / 2;
        int startZ = cameraPos.getBlockZ();
        int startY = Math.max(0, cameraPos.getBlockY() - height / 4);

        // 初始化输出缓冲区（RGBA: R=0, G=1, B=2, A=3）
        byte[] frameData = new byte[width * height * 4];

        // 逐像素采样（从前方一定距离开始，避免贴着玩家自身）
        int sampleDistance = Math.max(8, width / 4); // 至少隔 8 格采样
        
        for (int py = 0; py < height; py++) {
            double rayZ = cameraPos.getZ() + dirZ * sampleDistance;
            double rayX = cameraPos.getX() + dirX * sampleDistance;

            int pixelBlockX = (int) Math.round(rayX);
            int pixelBlockY = startY + py;
            int pixelBlockZ = (int) Math.round(rayZ);

            // 获取方块数据并映射颜色
            byte[] blockColor = getBlockColor(world, pixelBlockX, pixelBlockY, pixelBlockZ);

            // 填充当前像素的 RGBA（A=255）
            int offset = py * width * 4;
            System.arraycopy(blockColor, 0, frameData, offset + (pixelBlockX - startX) * 4, 4);
        }

        return frameData;
    }

    /**
     * 获取方块的颜色（RGBA），返回 [R, G, B, A]。
     */
    private static byte[] getBlockColor(World world, int x, int y, int z) {
        if (y < 0 || y >= 256) return new byte[]{(byte) 139, (byte) 195, (byte) 72, (byte) 255};

        BlockSnapshot snapshot = world.captureState().getBlock(x, y, z);
        if (snapshot == null) {
            return new byte[]{0, 0, 0, 255}; // 透明黑色（未加载区块）
        }

        BlockData data = snapshot.getData();
        if (data == null || !data.getMaterial().isAir()) {
            return getBlockMaterialColor(data);
        }
        return new byte[]{0, 0, 0, 255}; // 透明（空气）
    }

    /**
     * 根据方块材质映射到近似颜色。
     */
    private static byte[] getBlockMaterialColor(BlockData data) {
        String material = data.getMaterial().name();

        switch (material) {
            case "GRASS_BLOCK": return new byte[]{76, (byte)175, 80, (byte)255};
            case "DIRT": return new byte[]{139, 69, 19, (byte)255];
            case "STONE": return new byte[]{128, 128, 128, (byte)255];
            case "SAND": return new byte[]{(byte)244, (byte)223, 106, (byte)255];
            case "WATER": return new byte[]{33, 150, 243, 80}; // 半透明蓝
            case "GLASS": return new byte[]{(byte)200, (byte)200, (byte)200, 40}; // 高透明
            case "WOOD": return new byte[]{139, 69, 19, (byte)255];
            case "LEAVES": return new byte[]{76, (byte)175, 80, (byte)255};
            case "IRON_BLOCK": return new byte{192, 192, 192, (byte)255];
            case "GOLD_BLOCK": return new byte[]{(byte)255, 215, 0, (byte)255};
            case "DIAMOND_BLOCK": return new byte[]{64, 224, 208, (byte)255];
            default: return new byte[]{128, 128, 128, (byte)255}; // 灰色默认
        }
    }

    /**
     * 获取帧的字节大小。
     */
    public static int frameByteSize(int width, int height) {
        return width * height * 4; // RGBA = 4 bytes per pixel
    }
}
