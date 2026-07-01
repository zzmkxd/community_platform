package com.community.file.config;

import com.community.common.config.OssProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfiguration {

    @Bean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true", matchIfMissing = true)
    public MinioClient minioClient(OssProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .region("us-east-1")
                .build();
    }

    /** 用于生成预签名 URL 的 client，使用外部可访问端点（浏览器直接可达，无需 replaceEndpoint） */
    @Bean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true", matchIfMissing = true)
    public MinioClient minioPresignedClient(OssProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEffectivePublicEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .region("us-east-1")
                .build();
    }

    @Bean
    public String initBucket(MinioClient minioClient, OssProperties props) {
        try {
            boolean exists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(props.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder().bucket(props.getBucketName()).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: " + props.getBucketName(), e);
        }
        return "OK";
    }
}
