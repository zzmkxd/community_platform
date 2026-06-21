package com.community.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "wx.mp")
public class WxMpProperties {

    private boolean useRedis;

    private RedisConfig redisConfig;

    private List<MpConfig> configs;

    @Data
    public static class MpConfig {
        private String appId;
        private String secret;
        private String token;
        private String aesKey;
    }

    @Data
    public static class RedisConfig {
        private String host;
        private Integer port;
        private String password;
        private Integer timeout;
    }
}
