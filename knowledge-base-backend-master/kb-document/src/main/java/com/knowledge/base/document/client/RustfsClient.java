package com.knowledge.base.document.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * Rustfs file server client
 *
 * <p>Responsible for communicating with the rustfs file server</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class RustfsClient {

    @Value("${file.server.rustfs.base-url:}")
    private String baseUrl;

    @Value("${file.server.rustfs.upload-path:/api/upload}")
    private String uploadPath;

    @Value("${file.server.rustfs.enabled:false}")
    private boolean enabled;

    @Value("${file.server.rustfs.auth-token:}")
    private String authToken;

    private final RestTemplate restTemplate;

    public RustfsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Uploads a file to rustfs
     *
     * @param file file
     * @return file access URL
     */
    public String uploadFile(File file) {
        if (!enabled) {
            throw new UnsupportedOperationException("The rustfs file server is not enabled");
        }

        try {
            String url = baseUrl + uploadPath;

            // Build the request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + authToken);
            }

            // Build the request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Upload file to rustfs: fileName={}, url={}", file.getName(), url);

            // Send the request
            ResponseEntity<RustfsUploadResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    RustfsUploadResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String fileUrl = response.getBody().getUrl();
                log.info("rustfs upload successful: fileUrl={}", fileUrl);
                return fileUrl;
            } else {
                throw new RuntimeException("rustfs upload failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to upload to rustfs", e);
            throw new RuntimeException("Failed to upload to rustfs: " + e.getMessage());
        }
    }

    /**
     * rustfs upload response
     */
    private static class RustfsUploadResponse {
        private String url;
        private String fileName;
        private Long fileSize;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
