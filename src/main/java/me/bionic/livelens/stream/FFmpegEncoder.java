package me.bionic.livelens.stream;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/**
 * H264 编码器 — 使用 Javacv FFmpegFrameRecorder 进行服务器端视频编码。
 * 
 * 工作流程：
 *   1. 接收 RGBA 原始帧数据
 *   2. 转换为 YUV420P（FFmpeg 标准格式）
 *   3. 通过 RTMP 推流到 Bilibili/Douyin/自定义服务器
 */
public class FFmpegEncoder {

    private final String rtmpUrl;
    private final int width, height, fps;
    private long maxDurationMs; // 最大运行时长（毫秒）

    private FFmpegFrameRecorder recorder;
    private volatile boolean running = false;
    private Thread captureThread;
    private int tickCounter = 0;

    public FFmpegEncoder(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.maxDurationMs = 3600_000L; // 默认1小时超时
    }

    /** 设置 RTMP 推流地址 */
    public void setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
    }

    /** 获取当前推流 URL（用于 status） */
    public String getRtmpUrl() { return rtmpUrl; }
    
    /** 设置最大运行时长（毫秒），0 表示不限制 */
    public void setMaxDurationMs(long ms) {
        this.maxDurationMs = Math.max(ms, 0);
    }

    /** 开始录制并推流 */
    public void start() throws Exception {
        if (running) return; // already running
        
        String url = rtmpUrl != null ? rtmpUrl : "rtmp://localhost/live/stream";
        recorder = new FFmpegFrameRecorder(url, width, height);
        
        // H264 编码配置 — YUV420P 兼容性最好，适合 RTMP 推流
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(org.bytedeco.javacv.FFmpegFrameRecorder.PIX_FMT_YUV420P);
        recorder.setFrameRate(fps); // 目标帧率
        
        // FFmpeg 视频参数 — preset=medium, tune=zerolatency（低延迟推流）
        recorder.setVideoOption("preset", "ultrafast");     // 最快编码速度
        recorder.setVideoOption("tune", "zerolatency");      // 实时推流优化
        
        // 关键帧间隔 = fps (每秒一个 I 帧，RTMP 要求)
        recorder.setGopSize(fps);
        
        // 比特率 — 720p@30fps 推荐 2-4 Mbps（B站/Douyin 接受范围）
        int bitrate = width * height / 16; // ~5.4Mbps for 1280x720
        recorder.setVideoBitrate(bitrate);

        recorder.start();
        running = true;
    }

    /** 
     * 捕获一帧并写入编码器。
     * @param frameData RGBA 字节数组 (每像素4字节: R,G,B,A)
     */
    public synchronized void captureFrame(byte[] frameData, int tickInterval) {
        if (!running || recorder == null || frameData == null) return;

        // 帧率控制 — 使用 tick 计数器决定何时捕获一帧
        tickCounter++;
        if (tickCounter % tickInterval != 0) return;

        try {
            Frame rawFrame = new Frame(width, height);
            rawFrame.image = new byte[][]{frameData}; // Y plane = RGBA data
            rawFrame.imageWidth = width;
            rawFrame.imageHeight = height;
            
            recorder.record(rawFrame); // 自动转换为 YUV420P
            
        } catch (Exception e) {
            stop(); // encoder error → stop stream
        }

        tickCounter = 0;
    }

    /** 停止编码并释放资源 */
    public void stop() {
        if (!running) return;
        running = false;
        
        try {
            if (recorder != null) recorder.close();
        } catch (Exception ignored) {}
        
        captureThread = null;
        recorder = null;
    }

    /** 是否在运行 */
    public boolean isRunning() { return running; }
}
