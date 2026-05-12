package com.bsu.cvbuilder.configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class S3Configuration {

    private final ApplicationProperties applicationProperties;

    @Bean
    MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(applicationProperties.getMinio().getUrl())
                .credentials(applicationProperties.getMinio().getAccessKey(), applicationProperties.getMinio().getSecretKey())
                .build();
    }
}
