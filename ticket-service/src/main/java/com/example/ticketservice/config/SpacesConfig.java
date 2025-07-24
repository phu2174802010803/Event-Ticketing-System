package com.example.ticketservice.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
public class SpacesConfig {

    @Bean
    public S3Client s3Client() {
        String accessKey = "DO00MDETJAHU2AL77MWR"; // Thay bằng access key
        String secretKey = "SxnoaLHq2AzV6mYLIXbegXW2eCvigu6BK795lrYD+nE"; // Thay bằng secret key
        String endpoint = "https://sgp1.digitaloceanspaces.com";

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .region(Region.of("sgp1")) // Thay bằng khu vực
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}