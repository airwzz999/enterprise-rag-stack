package com.knowledge.base.file.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

/**
 * RustFS S3 client configuration class
 * Uses the AWS SDK to connect to the RustFS distributed file system via its S3-compatible API
 */
@Configuration
@ConditionalOnProperty(name = "file.storage.type", havingValue = "rustfs")
public class RustFsS3Config {

    private final FileStorageProperties storageProperties;

    public RustFsS3Config(FileStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean
    public S3Client s3Client() {
        FileStorageProperties.Rustfs config = storageProperties.getRustfs();

        // Build the endpoint URI
        String protocol = config.isSecure() ? "https" : "http";
        URI endpointUri = URI.create(protocol + "://" + config.getEndpoint() + ":" + config.getPort());

        // Timeout configuration
        Duration connectTimeout = Duration.ofMillis(config.getConnectTimeout());
        Duration readTimeout = Duration.ofMillis(config.getReadTimeout());
        Duration writeTimeout = Duration.ofMillis(config.getWriteTimeout());

        // S3 client configuration
        S3Configuration s3Config = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();

        // API call timeout (avoids indefinite blocking on large file uploads)
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(readTimeout.plus(writeTimeout))
                .apiCallAttemptTimeout(readTimeout)
                .build();

        return S3Client.builder()
                // Set a custom endpoint (pointing to the RustFS server)
                .endpointOverride(endpointUri)
                // Configure credentials
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
                ))
                // Set the region (any value works for S3-compatible storage)
                .region(Region.of("us-east-1"))
                // S3-specific configuration
                .serviceConfiguration(s3Config)
                // API timeout configuration
                .overrideConfiguration(overrideConfig)
                .build();
    }
}
