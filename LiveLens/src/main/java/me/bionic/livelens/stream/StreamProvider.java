package me.bionic.livelens.stream;

/**
 * 支持的直播平台推流端点。
 */
public enum StreamProvider {

    BILIBILI("bilibili", "rtmp://live-push.bilivideo.com/live-bvc/", "B站直播推流"),
    DOUYIN("douyin", "rtmp://push-l3-c.douyinvod.com/?stream_key=", "抖音直播推流"),
    CUSTOM("custom", "${rtmp_url}/?key=${stream_key}", "自定义 RTMP 服务器");

    private final String configKey;
    private final String rtmpEndpointTemplate;
    private final String displayName;

    StreamProvider(String configKey, String endpoint, String displayName) {
        this.configKey = configKey;
        this.rtmpEndpointTemplate = endpoint;
        this.displayName = displayName;
    }

    public String getConfigKey() { return configKey; }

    public String getDisplayName() { return displayName; }

    /** 构建完整的推流地址。 */
    public String buildUrl(String streamKey, String customRtmpUrl) {
        if (this == CUSTOM && customRtmpUrl != null && !customRtmpUrl.isEmpty()) {
            return customRtmpUrl.replace("${stream_key}", streamKey);
        }
        return rtmpEndpointTemplate + streamKey;
    }

    public static StreamProvider fromConfig(String value) {
        for (StreamProvider p : values()) {
            if (p.configKey.equalsIgnoreCase(value)) {
                return p;
            }
        }
        return BILIBILI;
    }
}