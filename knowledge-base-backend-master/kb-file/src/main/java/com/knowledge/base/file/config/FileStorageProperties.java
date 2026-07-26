package com.knowledge.base.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * File storage configuration properties class
 *
 * <p>Centralizes RustFS distributed file storage configuration</p>
 *
 * @author knowledge-base-team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * Storage type: rustfs
     */
    private String type = "rustfs";

    /**
     * RustFS distributed storage configuration
     */
    private Rustfs rustfs = new Rustfs();

    /**
     * Upload limit configuration
     */
    private Upload upload = new Upload();

    /**
     * URL configuration
     */
    private Url url = new Url();

    @Data
    public static class Rustfs {
        /**
         * RustFS service endpoint address
         */
        private String endpoint = "localhost";

        /**
         * Service port
         */
        private int port = 9091;

        /**
         * Access key ID
         */
        private String accessKey = "";

        /**
         * Secret access key
         */
        private String secretKey = "";

        /**
         * Whether to use HTTPS
         */
        private boolean secure = false;

        /**
         * Default bucket name
         */
        private String bucketName = "mall-dev";

        /**
         * Connection timeout (ms)
         */
        private int connectTimeout = 30000;

        /**
         * Read timeout (ms)
         */
        private int readTimeout = 60000;

        /**
         * Write timeout (ms)
         */
        private int writeTimeout = 60000;

        /**
         * Maximum number of connections
         */
        private int maxConnections = 50;

        /**
         * Whether to enable RustFS
         */
        private boolean enabled = true;
    }

    /**
     * FFmpeg configuration
     */
    private Ffmpeg ffmpeg = new Ffmpeg();

    @Data
    public static class Upload {
        /**
         * Maximum file size (bytes), defaults to 2GB
         */
        private long maxSize = 2147483648L;

        /**
         * Allowed file types
         */
        private List<String> allowedTypes = Arrays.asList(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "md",
                "png", "jpg", "jpeg", "gif", "bmp", "svg",
                "mp4", "avi", "mov", "wmv", "mkv", "webm", "flv",
                "mp3", "wav", "flac", "aac", "ogg"
        );

        /**
         * Whether to enable instant upload (dedupe via hash)
         */
        private boolean enableFastUpload = true;

        /**
         * Whether to compute the file hash
         */
        private boolean calculateHash = true;

        /**
         * Whether to enable resumable upload
         */
        private boolean enableResumableUpload = true;
    }

    @Data
    public static class Ffmpeg {
        /**
         * FFmpeg executable path
         */
        private String path = "/usr/bin/ffmpeg";

        /**
         * FFprobe executable path
         */
        private String ffprobePath = "/usr/bin/ffprobe";

        /**
         * HLS segment duration (seconds)
         */
        private int hlsSegmentTime = 10;

        /**
         * Thumbnail capture timestamp (seconds)
         */
        private int thumbnailTime = 5;
    }

    @Data
    public static class Url {
        /**
         * File access URL prefix
         */
        private String prefix = "http://localhost:8084/file";

        /**
         * Preview URL prefix
         */
        private String previewPrefix = "http://localhost:8084/file/preview";
    }
}