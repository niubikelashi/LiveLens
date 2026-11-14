package me.bionic.livelens.stream;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/** H264 编码器 — 使用 Javacv FFmpegFrameRecorder 进行服务器端视频编码。 */
public class FFmpegEncoder {

    private final int width, height, fps;
    private String rtmpUrl;
    private long maxDurationMs = 3600_000L;

    private FFmpegFrameRecorder recorder;
    private volatile boolean running = false;
    private Thread captureThread;
    private int tickCounter = 0;

    public FFmpegEncoder(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public void setRtmpUrl(String url) { this.rtmpUrl = url; }
    public String getRtmpUrl() { return rtmpUrl; }
    
    /** 设置最大运行时长（毫秒），0=不限制 */
    public void setMaxDurationMs(long ms) {
        this.maxDurationMs = Math.max(ms, 0);
    }

    public synchronized void start() throws Exception {
        if (running) return;
        
        String url = rtmpUrl != null ? rtmpUrl : "rtmp://localhost/live/stream";
        recorder = new FFmpegFrameRecorder(url, width, height);
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(org.bytedeco.javacv.FFmpegFrameRecorder.PIX_FMT_YUV420P);
        recorder.setFrameRate(fps);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setGopSize(fps);
        int bitrate = width * height / 16; // ~5.4Mbps for 1280x720
        recorder.setVideoBitrate(bitrate);

        recorder.start();
        running = true;
    }

    /** 捕获一帧并写入编码器。 */
    public synchronized void captureFrame(byte[] frameData, int tickInterval) {
        if (!running || recorder == null || frameData == null) return;

        tickCounter++;
        if (tickCounter % tickInterval != 0) return;

        try {
            Frame rawFrame = new Frame(width, height);
            rawFrame.image = new byte[][]{frameData};
            rawFrame.imageWidth = width;
            rawFrame.imageHeight = height;
            recorder.record(rawFrame);
        } catch (Exception e) {
            stop();
        }

        tickCounter = 0;
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try { if (recorder != null) recorder.close(); } catch (Exception ignored) {}
        captureThread = null;
        recorder = null;
    }

    public boolean isRunning() { return running; }
}