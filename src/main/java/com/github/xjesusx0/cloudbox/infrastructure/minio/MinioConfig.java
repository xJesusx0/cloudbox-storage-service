package com.github.xjesusx0.cloudbox.infrastructure.minio;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.username}")
    private String username;

    @Value("${storage.minio.password}")
    private String password;

    @Value("${storage.minio.region}")
    private String region;


    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(username, password)
                .region(region)
                .build();
    }
}
