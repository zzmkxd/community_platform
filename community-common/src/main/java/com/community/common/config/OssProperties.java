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
    /** MinIO 服务端点（Docker 内部网络用，如 http://minio:9000） */
    private String endpoint;
    /** MinIO 外部访问端点（浏览器用，如 http://localhost:9004）。为空时回退到 endpoint */
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /** 获取用于生成 presigned URL 的外部端点 */
    public String getEffectivePublicEndpoint() {
        return (publicEndpoint != null && !publicEndpoint.isBlank())
                ? publicEndpoint : endpoint;
    }
}
