package com.community.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private boolean enabled = true;
    private String type = "minio";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
