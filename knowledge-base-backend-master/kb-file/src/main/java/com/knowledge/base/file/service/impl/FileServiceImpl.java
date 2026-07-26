package com.knowledge.base.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.config.InstanceIdentifier;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.file.config.FileStorageProperties;
import com.knowledge.base.file.config.RabbitMQConfig;
import com.knowledge.base.file.dto.FileQueryDTO;
import com.knowledge.base.file.dto.FileUploadDTO;
import com.knowledge.base.file.entity.FileInfo;
import com.knowledge.base.file.mapper.FileMapper;
import com.knowledge.base.file.message.TranscodeMessage;
import com.knowledge.base.file.service.FileService;
import com.knowledge.base.file.service.MediaService;
import com.knowledge.base.file.storage.FileStorage;
import com.knowledge.base.file.storage.FileStorageFactory;
import com.knowledge.base.file.storage.RustFileStorage;
import com.knowledge.base.file.vo.FileInfoVO;
import com.knowledge.base.file.vo.MediaMetadata;
import com.knowledge.base.file.vo.UrlConvertResponse;
import com.knowledge.base.file.vo.BatchConvertResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.knowledge.base.common.config.SystemConfigCache;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * File Service implementation
 *
 * <p>Designed following the Alibaba Java Development Guidelines, implementing file-related business logic</p>
 * <p>Supports multiple storage backends, with instant upload (hash dedupe) and resumable upload capabilities</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileService {

    @Resource
    private FileMapper fileMapper;

    @Resource
    private SystemConfigCache systemConfigCache;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    @Resource
    private InstanceIdentifier instanceIdentifier;

    private final FileStorageFactory storageFactory;
    private final FileStorageProperties storageProperties;
    private final MediaService mediaService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Upload a file
     *
     * @param file the file
     * @param dto  upload parameters
     * @return file info
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO uploadFile(MultipartFile file, FileUploadDTO dto) {
        log.info("Starting file upload: originalName={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 1. Validate the file
        validateFile(file);

        String fileHash;
        try {
            // 2. Compute the file hash (used for instant upload)
            fileHash = storageProperties.getUpload().isCalculateHash()
                    ? calculateFileHash(file.getInputStream())
                    : UUID.randomUUID().toString();

            // 3. Check whether the file already exists (instant upload)
            if (storageProperties.getUpload().isEnableFastUpload()) {
                FileInfo existFile = fileMapper.selectOne(
                        new LambdaQueryWrapper<FileInfo>()
                                .eq(FileInfo::getFileHash, fileHash)
                                .eq(FileInfo::getStatus, 1)
                );

                if (existFile != null) {
                    log.info("File already exists, using instant upload: fileId={}, hash={}", existFile.getId(), fileHash);
                    return convertToVO(existFile);
                }
            }

            // 4. Generate the storage path
            String relativePath = generateRelativePath(fileHash, file.getOriginalFilename());

            // 5. Upload the file to the storage backend
            FileStorage storage = storageFactory.getStorage();
            boolean uploadSuccess = storage.upload(file.getInputStream(), relativePath, file.getSize());

            if (!uploadSuccess) {
                throw new BusinessException("Failed to upload file");
            }

            // 6. Detect the file type
            String fileType = detectFileType(FileUtil.extName(file.getOriginalFilename()), file.getContentType());

            // 7. Build the file info entity
            FileInfo fileInfo = buildFileInfo(file, fileHash, relativePath, fileType, dto);

            // 8. Save the file info to the database
            int count = fileMapper.insert(fileInfo);
            if (count <= 0) {
                // Roll back: delete the already-uploaded file
                storage.delete(relativePath);
                throw new BusinessException("Failed to save file info");
            }

            // 9. Audio/video files: asynchronously extract metadata
            if (isMediaFile(fileInfo)) {
                try {
                    MediaMetadata metadata = mediaService.probeMediaInfo(fileInfo.getId());
                    if (metadata.getDuration() != null) {
                        fileInfo.setDuration(metadata.getDuration());
                    }
                    if (metadata.getResolution() != null) {
                        fileInfo.setResolution(metadata.getResolution());
                    }
                    if (metadata.getBitrate() != null) {
                        fileInfo.setBitrate(metadata.getBitrate());
                    }
                    fileMapper.updateById(fileInfo);
                    log.info("Media metadata extraction completed: fileId={}, duration={}, resolution={}, bitrate={}",
                            fileInfo.getId(), metadata.getDuration(), metadata.getResolution(), metadata.getBitrate());
                } catch (Exception e) {
                    log.warn("Media metadata extraction failed (does not affect the upload): fileId={}, error={}", fileInfo.getId(), e.getMessage());
                }
            }

            log.info("File uploaded successfully: fileId={}, path={}", fileInfo.getId(), relativePath);
            return convertToVO(fileInfo);

        } catch (IOException e) {
            log.error("Failed to upload file: originalName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Upload files in batch
     *
     * @param files list of files
     * @param dto   upload parameters
     * @return list of file info
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileInfoVO> uploadFiles(MultipartFile[] files, FileUploadDTO dto) {
        log.info("Starting batch file upload: fileCount={}", files.length);

        List<FileInfoVO> results = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                FileInfoVO fileInfoVO = uploadFile(file, dto);
                results.add(fileInfoVO);
            } catch (Exception e) {
                log.error("Failed to upload file: originalName={}, error={}", file.getOriginalFilename(), e.getMessage());
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Batch upload completed: success={}, failed={}", results.size(), errors.size());
        }

        return results;
    }

    /**
     * Download a file
     *
     * @param fileId     file ID
     * @param response   HTTP response
     */
    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) throws IOException {
        log.info("Starting file download: fileId={}", fileId);

        // 1. Parameter validation
        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        // 2. Query the file info
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        if (fileInfo.getStatus() == 0) {
            throw new BusinessException("File has been deleted");
        }

        // 3. Get the storage implementation
        FileStorage storage = storageFactory.getStorage();

        // 4. Check whether the file exists
        if (!storage.exists(fileInfo.getFilePath())) {
            throw new BusinessException("File does not exist");
        }

        // 5. Set response headers
        response.setContentType(fileInfo.getMimeType());
        response.setHeader("Content-Disposition", buildContentDisposition("attachment", fileInfo.getOriginalName()));
        response.setHeader("Content-Length", String.valueOf(fileInfo.getFileSize()));
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 6. Stream the file download
        try (OutputStream outputStream = response.getOutputStream()) {
            storage.download(fileInfo.getFilePath(), outputStream);
        }

        // 7. Update the download count
        updateDownloadCount(fileId);

        log.info("File downloaded successfully: fileId={}, fileName={}", fileId, fileInfo.getOriginalName());
    }

    /**
     * Get a file stream
     *
     * @param fileId file ID
     * @return file stream
     */
    @Override
    public InputStream getFileStream(Long fileId) throws IOException {
        log.info("Getting file stream: fileId={}", fileId);

        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        FileStorage storage = storageFactory.getStorage();
        return storage.getInputStream(fileInfo.getFilePath());
    }

    /**
     * Delete a file
     *
     * @param fileId file ID
     * @return whether the deletion succeeded
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFile(Long fileId) {
        log.info("Deleting file: fileId={}", fileId);

        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        // Check whether the file is referenced by other documents (business-layer validation)
        // TODO: Implement file reference check logic

        // Soft-delete the file info
        fileInfo.setStatus(0);
        int count = fileMapper.updateById(fileInfo);

        // Delete the physical file
        if (count > 0) {
            try {
                FileStorage storage = storageFactory.getStorage();
                storage.delete(fileInfo.getFilePath());
                log.info("Physical file deleted successfully: path={}", fileInfo.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete physical file: path={}, error={}", fileInfo.getFilePath(), e.getMessage());
            }
        }

        return count > 0;
    }

    /**
     * Delete files in batch
     *
     * @param fileIds list of file IDs
     * @return whether the deletion succeeded
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteFiles(List<Long> fileIds) {
        log.info("Batch deleting files: fileCount={}", fileIds.size());

        if (fileIds == null || fileIds.isEmpty()) {
            return true;
        }

        int count = 0;
        for (Long fileId : fileIds) {
            try {
                deleteFile(fileId);
                count++;
            } catch (Exception e) {
                log.error("Failed to delete file: fileId={}, error={}", fileId, e.getMessage());
            }
        }

        return count > 0;
    }

    /**
     * Get file details
     *
     * @param fileId file ID
     * @return file info
     */
    @Override
    public FileInfoVO getFileInfo(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        return convertToVO(fileInfo);
    }

    /**
     * Query the file list with pagination
     *
     * @param dto query parameters
     * @return paginated result
     */
    @Override
    public PageResult<FileInfoVO> pageFiles(FileQueryDTO dto) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getStatus, 1);

        if (StringUtils.hasText(dto.getOriginalName())) {
            wrapper.like(FileInfo::getOriginalName, dto.getOriginalName());
        }
        if (StringUtils.hasText(dto.getFileType())) {
            wrapper.eq(FileInfo::getFileType, dto.getFileType());
        }
        if (dto.getUploaderId() != null) {
            wrapper.eq(FileInfo::getUploaderId, dto.getUploaderId());
        }
        if (dto.getAccessLevel() != null) {
            wrapper.eq(FileInfo::getAccessLevel, dto.getAccessLevel());
        }

        wrapper.orderByDesc(FileInfo::getCreatedAt);

        Page<FileInfo> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<FileInfo> filePage = fileMapper.selectPage(page, wrapper);

        IPage<FileInfoVO> voPage = filePage.convert(this::convertToVO);

        return PageResult.<FileInfoVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    /**
     * Get the file preview URL
     *
     * @param fileId file ID
     * @return preview URL
     */
    @Override
    public String getPreviewUrl(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        return storageProperties.getUrl().getPreviewPrefix() + "/" + fileId;
    }

    /**
     * Preview a file (returns the file content directly)
     *
     * @param fileId file ID
     * @param response HTTP response
     */
    @Override
    public void previewFile(Long fileId, HttpServletResponse response) throws IOException {
        log.info("Previewing file: fileId={}", fileId);

        // 1. Parameter validation
        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        // 2. Query the file info
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        if (fileInfo.getStatus() == 0) {
            throw new BusinessException("File has been deleted");
        }

        // 3. Get the storage implementation
        FileStorage storage = storageFactory.getStorage();

        // 4. Check whether the file exists
        if (!storage.exists(fileInfo.getFilePath())) {
            throw new BusinessException("File does not exist");
        }

        // 5. Set response headers (for preview, not download)
        response.setContentType(fileInfo.getMimeType());
        response.setHeader("Content-Length", String.valueOf(fileInfo.getFileSize()));
        response.setHeader("Cache-Control", "max-age=31536000, public"); // Cache for 1 year
        response.setHeader("Pragma", "public");

        // For image files, do not set Content-Disposition to attachment; use inline instead
        if ("IMAGE".equals(fileInfo.getFileType())) {
            response.setHeader("Content-Disposition", "inline");
        } else {
            response.setHeader("Content-Disposition", buildContentDisposition("inline", fileInfo.getOriginalName()));
        }

        // 6. Stream the file
        try (OutputStream outputStream = response.getOutputStream()) {
            storage.download(fileInfo.getFilePath(), outputStream);
        }

        log.info("File previewed successfully: fileId={}, fileName={}", fileId, fileInfo.getOriginalName());
    }

    /**
     * Convert file format (triggers asynchronous HLS transcoding for audio/video)
     *
     * @param fileId       file ID
     * @param targetFormat target format
     * @return converted file ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertFileFormat(Long fileId, String targetFormat) {
        log.info("Converting file format: fileId={}, targetFormat={}", fileId, targetFormat);

        if (fileId == null) {
            throw new BusinessException("File ID must not be empty");
        }

        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        // Only audio/video files support HLS transcoding
        if (!isMediaFile(fileInfo)) {
            throw new BusinessException("Only audio/video files support format conversion");
        }

        // Send an asynchronous transcode message to RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRANSCODE_EXCHANGE,
                "transcode." + instanceIdentifier.getId(),
                new TranscodeMessage(fileId, targetFormat)
        );

        // Update the transcode status to pending
        fileInfo.setTranscodeStatus("PENDING");
        fileMapper.updateById(fileInfo);

        log.info("Transcode task submitted: fileId={}, targetFormat={}", fileId, targetFormat);
        return fileId;
    }

    /**
     * Initialize a resumable upload
     */
    @Override
    public String initResumableUpload(String fileHash, String fileName, long totalSize, int chunkCount) {
        log.info("Initializing resumable upload: fileHash={}, fileName={}, totalSize={}, chunkCount={}",
                fileHash, fileName, totalSize, chunkCount);

        // Generate the session ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // Generate the storage path
        String relativePath = generateRelativePath(fileHash, fileName);

        // Get the RustFileStorage instance
        RustFileStorage rustStorage = (RustFileStorage) storageFactory.getStorage();
        rustStorage.initResumableUpload(sessionId, relativePath, totalSize, chunkCount);

        return sessionId;
    }

    /**
     * Upload a chunk
     */
    @Override
    public Boolean uploadChunk(String sessionId, int chunkIndex, MultipartFile chunkFile) {
        log.debug("Uploading chunk: sessionId={}, chunkIndex={}, size={}",
                sessionId, chunkIndex, chunkFile.getSize());

        try {
            RustFileStorage rustStorage = (RustFileStorage) storageFactory.getStorage();
            return rustStorage.uploadChunk(sessionId, chunkIndex, chunkFile.getInputStream(), chunkFile.getSize());
        } catch (IOException e) {
            log.error("Failed to upload chunk: {}", e.getMessage(), e);
            throw new BusinessException("Failed to upload chunk: " + e.getMessage());
        }
    }

    /**
     * Get the uploaded chunks
     */
    @Override
    public int[] getUploadedChunks(String sessionId) {
        RustFileStorage rustStorage = (RustFileStorage) storageFactory.getStorage();
        return rustStorage.getUploadedChunks(sessionId);
    }

    /**
     * Merge chunks
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO mergeChunks(String sessionId, FileUploadDTO dto) {
        log.info("Merging chunks: sessionId={}", sessionId);

        // Get the RustFileStorage instance
        RustFileStorage rustStorage = (RustFileStorage) storageFactory.getStorage();

        // Get the session info (via reflection or other means)
        // Simplified here: merge directly
        boolean success = rustStorage.mergeChunks(sessionId);

        if (!success) {
            throw new BusinessException("Failed to merge chunks");
        }

        // Since the session info is cleared after merging, we need to rebuild the file info from the request parameters
        // In a real application, the file info should be obtained from the session
        String fileHash = sessionId.substring(0, 8); // Simplified handling
        String fileName = dto.getFileType() != null ? "uploaded." + dto.getFileType() : "uploaded.bin";

        // Generate the storage path (must match the path generated in initResumableUpload)
        String relativePath = generateRelativePath(fileHash, fileName);

        // Get the file size
        long fileSize = rustStorage.getFileSize(relativePath);

        // Detect the file type
        String fileType = detectFileType(FileUtil.extName(fileName), null);

        // Build the file info entity
        FileInfo fileInfo = buildFileInfoForResumable(fileHash, fileName, relativePath, fileSize, fileType, dto);

        // Save the file info to the database
        int count = fileMapper.insert(fileInfo);
        if (count <= 0) {
            // Roll back: delete the already-uploaded file
            rustStorage.delete(relativePath);
            throw new BusinessException("Failed to save file info");
        }

        log.info("Chunks merged successfully: fileId={}", fileInfo.getId());
        return convertToVO(fileInfo);
    }

    // ==================== Private methods ====================

    /**
     * Validate the file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        // Check the file size (read from system configuration)
        long maxSize = getMaxFileSizeFromConfig();
        if (file.getSize() > maxSize) {
            throw new BusinessException("File size exceeds the limit: maximum " + formatFileSize(maxSize));
        }

        // Check the file type (read from system configuration)
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("File name must not be empty");
        }

        String extension = FileUtil.extName(originalFilename).toLowerCase();
        List<String> allowedTypes = getAllowedFileTypesFromConfig();

        if (!allowedTypes.contains(extension)) {
            throw new BusinessException("Unsupported file type: " + extension + ". Supported types: " + String.join(", ", allowedTypes));
        }
    }

    /**
     * Read the maximum file size from kb_system_config
     */
    private long getMaxFileSizeFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.max.size");
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 20971520L; // Defaults to 20MB
    }

    /**
     * Image type extensions (always allowed, used for scenarios like convertFromUrl)
     */
    private static final List<String> IMAGE_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"
    );

    /**
     * Read the list of allowed file types from kb_system_config
     * Note: image types are always included, ensuring that image download/upload scenarios like convertFromUrl are not restricted by configuration
     */
    private List<String> getAllowedFileTypesFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.allowed.types");
        if (value != null && !value.isBlank()) {
            // Merge the configured types with image types, deduplicating while preserving order
            List<String> configTypes = new java.util.ArrayList<>(List.of(value.toLowerCase().split(",")));
            Set<String> configSet = new LinkedHashSet<>(configTypes);
            configSet.addAll(IMAGE_EXTENSIONS);
            return List.copyOf(configSet);
        }
        // Support all common types by default
        return List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md",
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",
                "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
                "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma");
    }

    /**
     * Compute the file hash (streamed to avoid OOM on large files)
     */
    private String calculateFileHash(InputStream inputStream) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexUtil.encodeHexStr(digest.digest());
        } catch (Exception e) {
            log.error("Failed to compute file hash: {}", e.getMessage());
            throw new BusinessException("Failed to compute file hash");
        }
    }

    /**
     * Generate the relative storage path
     */
    private String generateRelativePath(String fileHash, String originalFilename) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String hashPrefix = fileHash.substring(0, 2);
        String extension = FileUtil.extName(originalFilename);
        String storedName = fileHash + (StringUtils.hasText(extension) ? "." + extension : "");

        return datePath + "/" + hashPrefix + "/" + storedName;
    }

    /**
     * Build the file info entity
     */
    private FileInfo buildFileInfo(MultipartFile file, String fileHash, String relativePath,
                                    String fileType, FileUploadDTO dto) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(SnowflakeIdGenerator.getInstance().nextId());
        fileInfo.setOriginalName(file.getOriginalFilename());
        fileInfo.setStoredName(relativePath.substring(relativePath.lastIndexOf("/") + 1));
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(fileType);
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setFileHash(fileHash);
        fileInfo.setStorageType(storageFactory.getStorageType().toUpperCase());
        fileInfo.setUploaderId(dto.getUploaderId() != null ? dto.getUploaderId() : 1L);
        fileInfo.setAccessLevel(dto.getAccessLevel() != null ? dto.getAccessLevel() : 0);
        fileInfo.setDownloadCount(0);
        fileInfo.setStatus(1);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        return fileInfo;
    }

    /**
     * Detect the file type
     */
    private String detectFileType(String extension, String mimeType) {
        if (extension == null) {
            return "OTHER";
        }

        return switch (extension.toLowerCase()) {
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> "DOCUMENT";
            case "png", "jpg", "jpeg", "gif", "bmp", "svg" -> "IMAGE";
            case "mp4", "avi", "mov", "wmv", "mkv", "webm", "flv" -> "VIDEO";
            case "mp3", "wav", "flac", "aac", "ogg" -> "AUDIO";
            default -> "OTHER";
        };
    }

    /**
     * Update the download count
     */
    private void updateDownloadCount(Long fileId) {
        try {
            FileInfo fileInfo = fileMapper.selectById(fileId);
            if (fileInfo != null) {
                fileInfo.setDownloadCount(fileInfo.getDownloadCount() + 1);
                fileMapper.updateById(fileInfo);
            }
        } catch (Exception e) {
            log.warn("Failed to update download count: fileId={}, error={}", fileId, e.getMessage());
        }
    }

    /**
     * Convert to a VO
     */
    private FileInfoVO convertToVO(FileInfo fileInfo) {
        String rustfsDirectUrl = null;

        // Try to build a direct RustFS access URL; fall back to a file-ID-based download URL if it fails
        if (fileInfo.getFilePath() != null && !fileInfo.getFilePath().isEmpty()) {
            try {
                rustfsDirectUrl = buildRustfsDirectUrl(fileInfo.getFilePath());
            } catch (Exception e) {
                log.error("Failed to build RustFS URL: fileId={}, filePath={}", fileInfo.getId(), fileInfo.getFilePath(), e);
            }
        }

        // If building the RustFS URL failed or the file path is empty, use the file ID to build a download URL
        if (rustfsDirectUrl == null && fileInfo.getId() != null) {
            rustfsDirectUrl = String.format("/files/download/%d", fileInfo.getId());
            log.warn("Using file ID to build the download URL: fileId={}", fileInfo.getId());
        }

        // Build the playback URL (only for audio/video files that have finished transcoding)
        String playUrl = null;
        if ("DONE".equals(fileInfo.getTranscodeStatus()) && fileInfo.getHlsPath() != null) {
            playUrl = String.format("/files/stream/%d/master.m3u8", fileInfo.getId());
        }

        // Build the thumbnail URL
        String thumbnailUrl = null;
        if (fileInfo.getThumbnailPath() != null) {
            thumbnailUrl = String.format("/files/thumbnail/%d", fileInfo.getId());
        }

        return FileInfoVO.builder()
                .id(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .fileSize(fileInfo.getFileSize())
                .fileSizeReadable(formatFileSize(fileInfo.getFileSize()))
                .fileType(fileInfo.getFileType())
                .mimeType(fileInfo.getMimeType())
                .fileUrl(rustfsDirectUrl)
                .previewUrl(rustfsDirectUrl)
                .uploaderId(fileInfo.getUploaderId())
                .uploaderName(null) // TODO: look up the uploader's name
                .accessLevel(fileInfo.getAccessLevel())
                .downloadCount(fileInfo.getDownloadCount())
                .storageType(fileInfo.getStorageType())
                .createdAt(fileInfo.getCreatedAt())
                .duration(fileInfo.getDuration())
                .resolution(fileInfo.getResolution())
                .bitrate(fileInfo.getBitrate())
                .transcodeStatus(fileInfo.getTranscodeStatus())
                .playUrl(playUrl)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    /**
     * Build a direct RustFS access URL
     *
     * @param filePath relative file path
     * @return direct RustFS access URL
     */
    private String buildRustfsDirectUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty");
        }

        // Build a direct RustFS access URL
        // Format: http://{endpoint}:{port}/{bucketName}/{filePath}
        String endpoint = storageProperties.getRustfs().getEndpoint();
        int port = storageProperties.getRustfs().getPort();
        String bucketName = storageProperties.getRustfs().getBucketName();
        boolean secure = storageProperties.getRustfs().isSecure();

        String protocol = secure ? "https" : "http";
        // Strip leading/trailing slashes from the file path to ensure a well-formed URL
        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

        return String.format("%s://%s:%d/%s/%s", protocol, endpoint, port, bucketName, normalizedPath);
    }

    /**
     * Build a Content-Disposition header value compliant with RFC 5987
     * For non-ASCII file names, uses the filename*=UTF-8''url-encoded format to avoid Tomcat exceptions
     */
    private String buildContentDisposition(String disposition, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return disposition + "; filename=\"file\"";
        }

        boolean isAscii = fileName.chars().allMatch(c -> c < 128);

        if (isAscii) {
            return disposition + "; filename=\"" + fileName + "\"";
        }

        String asciiFallback = fileName.replaceAll("[^\\x00-\\x7F]", "_");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return disposition + "; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedName;
    }

    /**
     * Format the file size
     */
    private String formatFileSize(Long size) {
        if (size == null) {
            return "0 B";
        }

        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / 1024.0 / 1024);
        } else {
            return String.format("%.2f GB", size / 1024.0 / 1024 / 1024);
        }
    }

    /**
     * Build the file info entity for a resumable upload
     */
    private FileInfo buildFileInfoForResumable(String fileHash, String fileName, String relativePath,
                                                long fileSize, String fileType, FileUploadDTO dto) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(SnowflakeIdGenerator.getInstance().nextId());
        fileInfo.setOriginalName(fileName);
        fileInfo.setStoredName(relativePath.substring(relativePath.lastIndexOf("/") + 1));
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileSize(fileSize);
        fileInfo.setFileType(fileType);
        fileInfo.setMimeType(null); // MIME type cannot be determined for resumable uploads
        fileInfo.setFileHash(fileHash);
        fileInfo.setStorageType(storageFactory.getStorageType().toUpperCase());
        fileInfo.setUploaderId(dto.getUploaderId() != null ? dto.getUploaderId() : 1L);
        fileInfo.setAccessLevel(dto.getAccessLevel() != null ? dto.getAccessLevel() : 0);
        fileInfo.setDownloadCount(0);
        fileInfo.setStatus(1);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        return fileInfo;
    }

    /**
     * Convert an image from a URL (download it and upload it into the system)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UrlConvertResponse convertFromUrl(String imageUrl) {
        log.info("Converting image from URL: imageUrl={}", imageUrl);

        try {
            // Download the image
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return UrlConvertResponse.builder()
                        .originalUrl(imageUrl)
                        .newUrl(null)
                        .success(false)
                        .errorMessage("Download failed, HTTP status code: " + responseCode)
                        .build();
            }

            // Get the file name and extension
            String fileName = extractFileNameFromUrl(imageUrl);
            String extension = FileUtil.extName(fileName);

            // Read the image data
            try (InputStream inputStream = connection.getInputStream()) {
                // Create the MultipartFile object
                MockMultipartFile mockFile = new MockMultipartFile(
                        "file",
                        fileName,
                        "image/" + extension,
                        inputStream
                );

                // Upload the file
                FileUploadDTO dto = new FileUploadDTO();
                dto.setUploaderId(1L);
                dto.setAccessLevel(0);

                FileInfoVO fileInfo = uploadFile(mockFile, dto);

                return UrlConvertResponse.builder()
                        .originalUrl(imageUrl)
                        .newUrl(fileInfo.getFileUrl())
                        .success(true)
                        .errorMessage(null)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to convert image from URL: imageUrl={}, error={}", imageUrl, e.getMessage(), e);
            return UrlConvertResponse.builder()
                    .originalUrl(imageUrl)
                    .newUrl(null)
                    .success(false)
                    .errorMessage("Conversion failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Batch convert image URLs (processed concurrently)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchConvertResponse batchConvertUrls(List<String> imageUrls) {
        log.info("Batch converting image URLs: urlCount={}", imageUrls.size());

        Map<String, String> urlMappings = new ConcurrentHashMap<>();
        Map<String, String> errorMappings = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Build the list of concurrent tasks
        List<CompletableFuture<Void>> futures = imageUrls.stream()
                .map(imageUrl -> CompletableFuture.runAsync(() -> {
                    try {
                        UrlConvertResponse response = convertFromUrl(imageUrl);
                        if (response.getSuccess()) {
                            urlMappings.put(imageUrl, response.getNewUrl());
                            successCount.incrementAndGet();
                        } else {
                            errorMappings.put(imageUrl, response.getErrorMessage());
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("Failed to convert image URL: imageUrl={}, error={}", imageUrl, e.getMessage(), e);
                        errorMappings.put(imageUrl, "Conversion failed: " + e.getMessage());
                        failureCount.incrementAndGet();
                    }
                }, asyncTaskExecutor))
                .toList();

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Batch conversion completed: success={}, failed={}", successCount.get(), failureCount.get());

        return BatchConvertResponse.builder()
                .urlMappings(urlMappings)
                .errorMappings(errorMappings)
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .build();
    }

    /**
     * Stream the HLS master playlist
     */
    @Override
    public void streamMasterPlaylist(Long fileId, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null || fileInfo.getHlsPath() == null) {
            throw new BusinessException("HLS playlist does not exist");
        }

        String hlsKey = fileInfo.getHlsPath() + "/master.m3u8";
        FileStorage storage = storageFactory.getStorage();
        response.setContentType("application/vnd.apple.mpegurl");
        response.setHeader("Cache-Control", "no-cache");

        try (OutputStream os = response.getOutputStream()) {
            storage.download(hlsKey, os);
        }
    }

    /**
     * Stream an HLS TS segment
     */
    @Override
    public void streamSegment(Long fileId, String segment, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null || fileInfo.getHlsPath() == null) {
            throw new BusinessException("HLS segment does not exist");
        }

        // segment format: "360p/000.ts" or "720p/000.ts"
        String hlsKey = fileInfo.getHlsPath() + "/" + segment;
        FileStorage storage = storageFactory.getStorage();
        response.setContentType("video/mp2t");
        response.setHeader("Cache-Control", "max-age=86400, public");

        try (OutputStream os = response.getOutputStream()) {
            storage.download(hlsKey, os);
        }
    }

    /**
     * Get a thumbnail
     */
    @Override
    public void getThumbnail(Long fileId, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null || fileInfo.getThumbnailPath() == null) {
            throw new BusinessException("Thumbnail does not exist");
        }

        FileStorage storage = storageFactory.getStorage();
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "max-age=86400, public");

        try (OutputStream os = response.getOutputStream()) {
            storage.download(fileInfo.getThumbnailPath(), os);
        }
    }

    /**
     * Determine whether the file is an audio/video file
     */
    private boolean isMediaFile(FileInfo fileInfo) {
        if (fileInfo.getFileType() == null) {
            return false;
        }
        return "VIDEO".equals(fileInfo.getFileType()) || "AUDIO".equals(fileInfo.getFileType());
    }

    /**
     * Extract the file name from a URL
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = new URL(url).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (fileName.isEmpty() || !fileName.contains(".")) {
                return "image_" + System.currentTimeMillis() + ".jpg";
            }
            return fileName;
        } catch (Exception e) {
            return "image_" + System.currentTimeMillis() + ".jpg";
        }
    }

    /**
     * MockMultipartFile wraps a file downloaded from a URL
     */
    private static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public MockMultipartFile(String name, String originalFilename, String contentType, InputStream inputStream) throws IOException {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = inputStream.readAllBytes();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}
