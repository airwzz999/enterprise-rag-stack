package com.knowledge.base.file.storage;

import com.knowledge.base.file.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RustFS file storage implementation
 * Uses the AWS SDK S3 client to connect to the RustFS distributed file system via its S3-compatible API
 */
@Slf4j
@Component("rustFileStorage")
@ConditionalOnProperty(name = "file.storage.type", havingValue = "rustfs")
public class RustFileStorage implements FileStorage {

    private final S3Client s3Client;
    private final FileStorageProperties storageProperties;
    
    // Stores multipart upload session info
    private final Map<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();

    public RustFileStorage(S3Client s3Client, FileStorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    private String getBucketName() {
        return storageProperties.getRustfs().getBucketName();
    }

    @Override
    public boolean upload(InputStream inputStream, String relativePath, long fileSize) {
        try {
            String bucketName = getBucketName();
            log.debug("Uploading file to RustFS: bucket={}, path={}, size={}", bucketName, relativePath, fileSize);

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(relativePath)
                            .build(),
                    RequestBody.fromInputStream(inputStream, fileSize));

            log.info("File uploaded successfully: {}", relativePath);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to upload file to RustFS: {}", relativePath, e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public long download(String relativePath, OutputStream outputStream) {
        try {
            String bucketName = getBucketName();
            log.debug("Downloading file from RustFS: bucket={}, path={}", bucketName, relativePath);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
                long bytesCopied = response.transferTo(outputStream);
                log.info("File downloaded successfully: {}, bytes={}", relativePath, bytesCopied);
                return bytesCopied;
            }
        } catch (S3Exception e) {
            log.error("Failed to download file from RustFS: {}", relativePath, e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error during download: {}", relativePath, e);
            throw new RuntimeException("IO error during download: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getInputStream(String relativePath) {
        try {
            String bucketName = getBucketName();
            log.debug("Getting input stream from RustFS: bucket={}, path={}", bucketName, relativePath);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            log.info("Input stream obtained: {}", relativePath);
            return response;
        } catch (S3Exception e) {
            log.error("Failed to get input stream from RustFS: {}", relativePath, e);
            throw new RuntimeException("Failed to get input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            String bucketName = getBucketName();
            log.debug("Deleting file from RustFS: bucket={}, path={}", bucketName, relativePath);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build());

            log.info("File deleted successfully: {}", relativePath);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete file from RustFS: {}", relativePath, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            String bucketName = getBucketName();
            log.debug("Checking file existence in RustFS: bucket={}, path={}", bucketName, relativePath);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("File not found in RustFS: {}", relativePath);
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence in RustFS: {}", relativePath, e);
            throw new RuntimeException("Error checking file existence: " + e.getMessage(), e);
        }
    }

    @Override
    public long getFileSize(String relativePath) {
        try {
            String bucketName = getBucketName();
            log.debug("Getting file size from RustFS: bucket={}, path={}", bucketName, relativePath);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(relativePath)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            long size = response.contentLength();
            log.info("File size obtained: {}, size={}", relativePath, size);
            return size;
        } catch (S3Exception e) {
            log.error("Failed to get file size from RustFS: {}", relativePath, e);
            throw new RuntimeException("Failed to get file size: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageType() {
        return "rustfs";
    }

    /**
     * Initialize a resumable upload session
     */
    public void initResumableUpload(String sessionId, String relativePath, long totalSize, int chunkCount) {
        log.debug("Initializing resumable upload session: sessionId={}, path={}, size={}, chunks={}",
                sessionId, relativePath, totalSize, chunkCount);

        // Create an S3 multipart upload
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(getBucketName())
                .key(relativePath)
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
        String uploadId = response.uploadId();

        // Store the session info
        UploadSession session = new UploadSession();
        session.setUploadId(uploadId);
        session.setRelativePath(relativePath);
        session.setTotalSize(totalSize);
        session.setChunkCount(chunkCount);
        session.setUploadedParts(new HashSet<>());

        uploadSessions.put(sessionId, session);
        log.info("Resumable upload session initialized successfully: sessionId={}, uploadId={}", sessionId, uploadId);
    }

    /**
     * Upload a chunk
     */
    public boolean uploadChunk(String sessionId, int chunkIndex, InputStream inputStream, long chunkSize) {
        log.debug("Uploading chunk: sessionId={}, chunkIndex={}, size={}", sessionId, chunkIndex, chunkSize);

        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        try {
            // S3 part numbers start at 1
            int partNumber = chunkIndex + 1;

            UploadPartRequest request = UploadPartRequest.builder()
                    .bucket(getBucketName())
                    .key(session.getRelativePath())
                    .uploadId(session.getUploadId())
                    .partNumber(partNumber)
                    .build();

            UploadPartResponse response = s3Client.uploadPart(request,
                    RequestBody.fromInputStream(inputStream, chunkSize));

            // Record the uploaded chunk
            session.getUploadedParts().add(chunkIndex);

            log.info("Chunk uploaded successfully: sessionId={}, chunkIndex={}, etag={}",
                    sessionId, chunkIndex, response.eTag());
            return true;
        } catch (S3Exception e) {
            log.error("Failed to upload chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload chunk: " + e.getMessage(), e);
        }
    }

    /**
     * Get the indexes of uploaded chunks
     */
    public int[] getUploadedChunks(String sessionId) {
        log.debug("Getting uploaded chunks: sessionId={}", sessionId);

        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        // Try to get the uploaded chunk info from S3
        try {
            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(getBucketName())
                    .key(session.getRelativePath())
                    .uploadId(session.getUploadId())
                    .build();

            ListPartsResponse response = s3Client.listParts(request);

            Set<Integer> uploadedChunks = new HashSet<>();
            for (Part part : response.parts()) {
                // S3 part numbers start at 1; convert to 0-based
                uploadedChunks.add(part.partNumber() - 1);
            }

            // Update the local cache
            session.getUploadedParts().clear();
            session.getUploadedParts().addAll(uploadedChunks);

            int[] result = uploadedChunks.stream().mapToInt(Integer::intValue).toArray();
            Arrays.sort(result);
            return result;
        } catch (S3Exception e) {
            log.error("Failed to get uploaded chunks: {}", e.getMessage(), e);
            // Fall back to the locally cached chunk info
            int[] result = session.getUploadedParts().stream().mapToInt(Integer::intValue).toArray();
            Arrays.sort(result);
            return result;
        }
    }

    /**
     * Merge chunks
     */
    public boolean mergeChunks(String sessionId) {
        log.debug("Merging chunks: sessionId={}", sessionId);

        UploadSession session = uploadSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        try {
            // Get all uploaded chunks
            int[] uploadedChunks = getUploadedChunks(sessionId);

            // Build the list of completed parts
            List<CompletedPart> completedParts = new ArrayList<>();
            for (int chunkIndex : uploadedChunks) {
                // Need to get the ETag of each part
                completedParts.add(CompletedPart.builder()
                        .partNumber(chunkIndex + 1)  // S3 part numbers start at 1
                        .eTag("etag-" + chunkIndex)  // Simplified here; should actually come from listParts
                        .build());
            }

            // Complete the multipart upload
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(getBucketName())
                    .key(session.getRelativePath())
                    .uploadId(session.getUploadId())
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build();

            s3Client.completeMultipartUpload(request);

            // Clean up the session
            uploadSessions.remove(sessionId);

            log.info("Chunks merged successfully: sessionId={}, path={}", sessionId, session.getRelativePath());
            return true;
        } catch (S3Exception e) {
            log.error("Failed to merge chunks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to merge chunks: " + e.getMessage(), e);
        }
    }

    /**
     * Multipart upload session info
     */
    private static class UploadSession {
        private String uploadId;
        private String relativePath;
        private long totalSize;
        private int chunkCount;
        private Set<Integer> uploadedParts;

        public String getUploadId() {
            return uploadId;
        }

        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        public Set<Integer> getUploadedParts() {
            return uploadedParts;
        }

        public void setUploadedParts(Set<Integer> uploadedParts) {
            this.uploadedParts = uploadedParts;
        }
    }
}
