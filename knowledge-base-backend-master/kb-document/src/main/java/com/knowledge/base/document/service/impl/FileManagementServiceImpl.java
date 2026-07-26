package com.knowledge.base.document.service.impl;

import cn.hutool.crypto.digest.DigestUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.entity.FileMetadata;
import com.knowledge.base.document.mapper.FileMetadataMapper;
import com.knowledge.base.document.service.FileManagementService;
import com.knowledge.base.document.service.FileUploadService;
import com.knowledge.base.document.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.knowledge.base.common.config.SystemConfigCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * File management service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileManagementServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata> implements FileManagementService {

    @Resource
    private FileMetadataMapper fileMetadataMapper;

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private SystemConfigCache systemConfigCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata uploadFile(MultipartFile file, Long userId, Boolean isPublic) {
        log.info("Upload file: fileName={}, userId={}, isPublic={}", file.getOriginalFilename(), userId, isPublic);

        try {
            // Check the file size
            if (file.isEmpty()) {
                throw new BusinessException("File must not be empty");
            }

            // Get the original file name
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new BusinessException("File name must not be empty");
            }

            // Check the file size (read from system configuration)
            long maxSize = getMaxFileSizeFromConfig();
            if (file.getSize() > maxSize) {
                throw new BusinessException("File size exceeds the limit: maximum " + (maxSize / 1048576) + "MB");
            }

            // Get the file extension
            String fileExtension = getFileExtension(originalFileName);

            // Check the file type (read from system configuration)
            List<String> allowedTypes = getAllowedFileTypesFromConfig();
            if (!allowedTypes.contains(fileExtension.toLowerCase())) {
                throw new BusinessException("Unsupported file type: " + fileExtension + ", supported types: " + String.join(", ", allowedTypes));
            }

            // Upload the file to the file server
            String fileUrl = fileUploadService.uploadFile(file);
            log.info("File uploaded successfully: fileUrl={}", fileUrl);

            // Create the file metadata
            FileMetadata metadata = new FileMetadata();
            metadata.setId(SnowflakeIdGenerator.getInstance().nextId());
            metadata.setFileName(getFileName(originalFileName));
            metadata.setOriginalFileName(originalFileName);
            metadata.setFileExtension(fileExtension);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setStoragePath(fileUrl);
            metadata.setAccessUrl(fileUrl);
            metadata.setFileCategory(determineFileCategory(fileExtension));
            metadata.setUploaderId(userId);
            metadata.setUploaderName(UserContext.getCurrentUserName());
            metadata.setIsPublic(isPublic != null ? isPublic : false);
            metadata.setDownloadCount(0);
            metadata.setUploadStatus("completed");

            // Compute the file hash
            try {
                byte[] fileBytes = file.getBytes();
                metadata.setFileMd5(DigestUtil.md5Hex(fileBytes));
                metadata.setFileSha256(DigestUtil.sha256Hex(fileBytes));
            } catch (Exception e) {
                log.warn("Failed to compute file hash", e);
            }

            // If it's an image, get its dimensions
            if (isImage(fileExtension)) {
                try {
                    BufferedImage image = ImageIO.read(file.getInputStream());
                    if (image != null) {
                        metadata.setWidth(image.getWidth());
                        metadata.setHeight(image.getHeight());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get image dimensions", e);
                }
            }

            // Save to the database
            fileMetadataMapper.insert(metadata);
            log.info("File metadata saved successfully: fileId={}", metadata.getId());

            return metadata;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public List<FileMetadata> getFileList(Long userId) {
        log.info("Get file list: userId={}", userId);
        return fileMetadataMapper.findByUploaderId(userId);
    }

    @Override
    public List<FileMetadata> getFileListByCategory(Long userId, String fileCategory) {
        log.info("Get file list by category: userId={}, fileCategory={}", userId, fileCategory);
        return fileMetadataMapper.findByUploaderIdAndCategory(userId, fileCategory);
    }

    @Override
    public FileMetadata getFileDetail(Long fileId) {
        log.info("Get file details: fileId={}", fileId);
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        // Update the last access time
        updateLastAccessTime(fileId);
        return metadata;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean renameFile(Long fileId, String newFileName, Long userId) {
        log.info("Rename file: fileId={}, newFileName={}, userId={}", fileId, newFileName, userId);

        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        // Check permission
        if (!metadata.getUploaderId().equals(userId)) {
            throw new BusinessException("No permission to operate on this file");
        }

        // Update the file name
        metadata.setFileName(newFileName);
        int result = fileMetadataMapper.updateById(metadata);

        log.info("Rename file complete: result={}", result);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFile(Long fileId, Long userId) {
        log.info("Delete file: fileId={}, userId={}", fileId, userId);

        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        // Check permission
        if (!metadata.getUploaderId().equals(userId)) {
            throw new BusinessException("No permission to operate on this file");
        }

        // Logical delete
        int result = fileMetadataMapper.deleteById(fileId);
        log.info("Delete file complete: result={}", result);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchDeleteFiles(List<Long> fileIds, Long userId) {
        log.info("Batch delete files: fileIds={}, userId={}", fileIds, userId);

        int count = 0;
        for (Long fileId : fileIds) {
            try {
                if (deleteFile(fileId, userId)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to delete file: fileId={}", fileId, e);
            }
        }

        log.info("Batch delete files complete: count={}", count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFilePermission(Long fileId, Boolean isPublic, Long userId) {
        log.info("Update file permission: fileId={}, isPublic={}, userId={}", fileId, isPublic, userId);

        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        // Check permission
        if (!metadata.getUploaderId().equals(userId)) {
            throw new BusinessException("No permission to operate on this file");
        }

        metadata.setIsPublic(isPublic);
        int result = fileMetadataMapper.updateById(metadata);

        log.info("Update file permission complete: result={}", result);
        return result > 0;
    }

    @Override
    public void incrementDownloadCount(Long fileId) {
        log.info("Increment download count: fileId={}", fileId);
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata != null) {
            Integer count = metadata.getDownloadCount();
            metadata.setDownloadCount(count == null ? 1 : count + 1);
            fileMetadataMapper.updateById(metadata);
        }
    }

    @Override
    public void updateLastAccessTime(Long fileId) {
        log.info("Update last access time: fileId={}", fileId);
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata != null) {
            metadata.setLastAccessTime(LocalDateTime.now());
            fileMetadataMapper.updateById(metadata);
        }
    }

    @Override
    public Map<String, Object> getFileStatistics(Long userId) {
        log.info("Get file statistics: userId={}", userId);

        Map<String, Object> statistics = new HashMap<>();

        // Total file count
        Integer totalCount = fileMetadataMapper.countByUploaderId(userId);
        statistics.put("totalCount", totalCount);

        // Total file size
        Long totalSize = fileMetadataMapper.sumFileSizeByUploaderId(userId);
        statistics.put("totalSize", totalSize);
        statistics.put("totalSizeReadable", formatFileSize(totalSize));

        // Count by category
        Map<String, Integer> categoryCount = new HashMap<>();
        List<FileMetadata> allFiles = fileMetadataMapper.findByUploaderId(userId);
        for (FileMetadata file : allFiles) {
            String category = file.getFileCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }
        statistics.put("categoryCount", categoryCount);

        // Uploaded today
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayCount = allFiles.stream()
                .filter(f -> f.getCreatedAt() != null && f.getCreatedAt().isAfter(today))
                .count();
        statistics.put("todayCount", todayCount);

        log.info("File statistics: statistics={}", statistics);
        return statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata copyFile(Long fileId, Long userId) {
        log.info("Copy file: fileId={}, userId={}", fileId, userId);

        FileMetadata original = fileMetadataMapper.selectById(fileId);
        if (original == null) {
            throw new BusinessException("File does not exist");
        }

        // Create the copy
        FileMetadata copy = new FileMetadata();
        copy.setId(SnowflakeIdGenerator.getInstance().nextId());
        copy.setFileName("Copy_" + original.getFileName());
        copy.setOriginalFileName(original.getOriginalFileName());
        copy.setFileExtension(original.getFileExtension());
        copy.setFileSize(original.getFileSize());
        copy.setContentType(original.getContentType());
        copy.setStoragePath(original.getStoragePath());
        copy.setAccessUrl(original.getAccessUrl());
        copy.setFileCategory(original.getFileCategory());
        copy.setUploaderId(userId);
        copy.setUploaderName(UserContext.getCurrentUserName());
        copy.setIsPublic(original.getIsPublic());
        copy.setDownloadCount(0);
        copy.setUploadStatus("completed");
        copy.setWidth(original.getWidth());
        copy.setHeight(original.getHeight());
        copy.setThumbnailUrl(original.getThumbnailUrl());
        copy.setFileMd5(original.getFileMd5());
        copy.setFileSha256(original.getFileSha256());

        fileMetadataMapper.insert(copy);
        log.info("File copy complete: newFileId={}", copy.getId());

        return copy;
    }

    @Override
    public List<FileMetadata> searchFiles(Long userId, String keyword) {
        log.info("Search files: userId={}, keyword={}", userId, keyword);

        LambdaQueryWrapper<FileMetadata> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileMetadata::getUploaderId, userId)
                .and(wrapper -> wrapper
                        .like(FileMetadata::getFileName, keyword)
                        .or()
                        .like(FileMetadata::getOriginalFileName, keyword))
                .orderByDesc(FileMetadata::getCreatedAt);

        return fileMetadataMapper.selectList(queryWrapper);
    }

    /**
     * Gets the file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    /**
     * Gets the file name (keeps the original file name)
     */
    private String getFileName(String originalFileName) {
        return originalFileName != null ? originalFileName : "Untitled File";
    }

    /**
     * Determines the file category
     */
    private String determineFileCategory(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "other";
        }

        String ext = extension.toLowerCase();

        // Image
        if (ext.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            return "image";
        }

        // Document
        if (ext.matches("doc|docx|pdf|txt|xls|xlsx|ppt|pptx|md|markdown")) {
            return "document";
        }

        // Video
        if (ext.matches("mp4|avi|mkv|mov|wmv|flv|webm")) {
            return "video";
        }

        // Audio
        if (ext.matches("mp3|wav|flac|aac|ogg|wma")) {
            return "audio";
        }

        // Archive
        if (ext.matches("zip|rar|7z|tar|gz")) {
            return "archive";
        }

        return "other";
    }

    /**
     * Determines whether it is an image
     */
    private boolean isImage(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String ext = extension.toLowerCase();
        return ext.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    @Override
    public void streamFile(Long fileId, HttpServletRequest request, HttpServletResponse response) {
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        String accessUrl = metadata.getAccessUrl();
        if (accessUrl == null || accessUrl.isEmpty()) {
            throw new BusinessException("File storage path does not exist");
        }

        log.info("Stream file: fileId={}, accessUrl={}, contentType={}", fileId, accessUrl, metadata.getContentType());

        try {
            long fileSize = metadata.getFileSize() != null ? metadata.getFileSize() : 0;
            String contentType = metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream";

            // Set common response headers
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", buildContentDisposition(metadata.getOriginalFileName()));
            response.setHeader("Accept-Ranges", "bytes");

            // Parse the Range request header
            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Handle a Range request -> return 206 Partial Content
                handleRangeRequest(rangeHeader, fileSize, accessUrl, response, fileId);
            } else {
                // Non-Range request -> return the full file 200 OK
                handleFullRequest(fileSize, accessUrl, response, fileId);
            }

            log.info("File streaming complete: fileId={}, size={}", fileId, fileSize);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to stream file: fileId={}", fileId, e);
            throw new BusinessException("File transfer failed: " + e.getMessage());
        }
    }

    /**
     * Handles an HTTP Range request, returning 206 Partial Content.
     * Browser audio/video elements require Range support to play correctly
     */
    private void handleRangeRequest(String rangeHeader, long fileSize, String accessUrl,
                                     HttpServletResponse response, Long fileId) throws IOException {
        // Parse Range: bytes=start-end
        long start = 0;
        long end = fileSize - 1;

        String rangeValue = rangeHeader.substring("bytes=".length()).trim();
        int dashIndex = rangeValue.indexOf('-');
        if (dashIndex > 0) {
            start = Long.parseLong(rangeValue.substring(0, dashIndex));
        }
        if (dashIndex < rangeValue.length() - 1) {
            end = Long.parseLong(rangeValue.substring(dashIndex + 1));
        }

        // Bounds check
        if (start >= fileSize) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileSize);
            return;
        }
        if (end >= fileSize) {
            end = fileSize - 1;
        }

        long contentLength = end - start + 1;
        log.debug("Range request: bytes={}-{}, contentLength={}, fileSize={}", start, end, contentLength, fileSize);

        // Set a 206 response
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setContentLengthLong(contentLength);

        // Connect to the source file and request the specified range
        URLConnection conn = new URL(accessUrl).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);

        try (InputStream inputStream = conn.getInputStream();
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long bytesWritten = 0;

            while (bytesWritten < contentLength && (bytesRead = inputStream.read(buffer, 0,
                    (int) Math.min(buffer.length, contentLength - bytesWritten))) != -1) {
                os.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;
            }
            os.flush();
        }

        updateLastAccessTime(fileId);
    }

    /**
     * Handles a full file request, returning 200 OK
     */
    private void handleFullRequest(long fileSize, String accessUrl,
                                    HttpServletResponse response, Long fileId) throws IOException {
        response.setContentLengthLong(fileSize);

        URLConnection conn = new URL(accessUrl).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        try (InputStream inputStream = conn.getInputStream();
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }

        updateLastAccessTime(fileId);
    }

    /**
     * Builds a Content-Disposition header value compliant with RFC 5987.
     * Non-ASCII file names use the filename*=UTF-8''url-encoded format to avoid Tomcat exceptions
     */
    private String buildContentDisposition(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "inline; filename=\"file\"";
        }

        // Determine whether the file name is pure ASCII
        boolean isAscii = fileName.chars().allMatch(c -> c < 128);

        if (isAscii) {
            return "inline; filename=\"" + fileName + "\"";
        }

        // Non-ASCII: encode with RFC 5987 filename*, while also providing an ASCII fallback
        String asciiFallback = fileName.replaceAll("[^\\x00-\\x7F]", "_");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedName;
    }

    /**
     * Formats a file size
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
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public List<String> getPptxSlideImages(Long fileId) {
        log.info("Render PPTX slides: fileId={}", fileId);

        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File does not exist");
        }

        String accessUrl = metadata.getAccessUrl();
        if (accessUrl == null || accessUrl.isEmpty()) {
            throw new BusinessException("File storage path does not exist");
        }

        // Render resolution: 960x540 (16:9, matching the frontend's ~852px preview window)
        Dimension pgsize = new Dimension(960, 540);

        try {
            byte[] pptxBytes = readUrlBytes(accessUrl);

            // Try parsing directly first; retry after repair if the XML format is malformed
            try (XMLSlideShow ppt = createSlideShow(pptxBytes)) {
                return renderSlides(ppt, pgsize, fileId);
            } catch (Exception firstAttempt) {
                log.warn("PPTX direct parsing failed, retrying after repairing the XML: fileId={}, error={}",
                        fileId, firstAttempt.getMessage());

                byte[] repairedBytes = repairPptxSlideXml(pptxBytes);
                try (XMLSlideShow ppt = createSlideShow(repairedBytes)) {
                    log.info("PPTX XML repaired successfully: fileId={}", fileId);
                    return renderSlides(ppt, pgsize, fileId);
                } catch (Exception repairFailed) {
                    log.error("PPTX still failed to parse after XML repair: fileId={}", fileId, repairFailed);
                    throw firstAttempt;
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PPTX slide rendering failed: fileId={}", fileId, e);
            throw new BusinessException("PPTX slide rendering failed: " + e.getMessage());
        }
    }

    /**
     * Reads all bytes from a URL
     */
    private byte[] readUrlBytes(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return is.readAllBytes();
        }
    }

    /**
     * Creates an XMLSlideShow from a byte array
     */
    private XMLSlideShow createSlideShow(byte[] data) throws IOException {
        return new XMLSlideShow(new ByteArrayInputStream(data));
    }

    /**
     * Renders all slides into a list of Base64 PNG images
     */
    private List<String> renderSlides(XMLSlideShow ppt, Dimension pgsize, Long fileId) throws IOException {
        List<XSLFSlide> slides = ppt.getSlides();
        if (slides.isEmpty()) {
            throw new BusinessException("The PPTX file contains no slides");
        }

        List<String> images = new ArrayList<>(slides.size());
        for (XSLFSlide slide : slides) {
            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, pgsize.width, pgsize.height);

            slide.draw(graphics);
            graphics.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
            images.add(base64);
        }

        log.info("PPTX slide rendering complete: fileId={}, slideCount={}", fileId, images.size());
        return images;
    }

    /**
     * Repairs missing tags in the XML files within a PPTX package.
     *
     * <p>In PPTX files exported by some editing tools, the XML may be missing closing tags
     * (such as {@code </p:txBody>}, {@code </p:nvSpPr>}, etc.), causing SAX parsing to throw
     * a SAXParseException. This method iterates over all ppt XML files, using a tag balancer
     * to fill in any missing closing tags.</p>
     */
    private byte[] repairPptxSlideXml(byte[] pptxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(pptxBytes));
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] entryData = zis.readAllBytes();

                if (isPptXmlFile(name)) {
                    String xml = new String(entryData, StandardCharsets.UTF_8);
                    String balanced = balanceXmlTags(xml);
                    if (!balanced.equals(xml)) {
                        log.info("PPTX XML repaired: entry={} (original {} bytes, repaired {} bytes)",
                                name, xml.length(), balanced.length());
                    }
                    entryData = balanced.getBytes(StandardCharsets.UTF_8);
                }

                ZipEntry outEntry = new ZipEntry(name);
                zos.putNextEntry(outEntry);
                zos.write(entryData);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /**
     * Determines whether a ZIP entry is an XML file within a PPTX package (slides, layouts, masters)
     */
    private boolean isPptXmlFile(String name) {
        return name.startsWith("ppt/") && name.endsWith(".xml");
    }

    /**
     * Balances XML tags: detects and fills in missing closing tags.
     *
     * <p>In PPTX files exported by some editing tools, the XML may be missing closing tags
     * (commonly on elements such as {@code p:txBody}, {@code p:nvSpPr}, {@code a:p}, etc.).
     * This method uses a stack to track open tags, automatically filling in any unclosed
     * tags when a parent element closes or the document ends.</p>
     */
    private String balanceXmlTags(String xml) {
        List<String> openTags = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = xml.length();

        while (i < len) {
            if (xml.charAt(i) == '<') {
                int tagEnd = xml.indexOf('>', i);
                if (tagEnd < 0) {
                    // Truncated XML, append the remaining content directly
                    result.append(xml, i, len);
                    break;
                }
                String tagContent = xml.substring(i + 1, tagEnd);
                boolean isClosing = tagContent.startsWith("/");
                boolean isSelfClosing = tagContent.endsWith("/");

                if (isSelfClosing || tagContent.startsWith("?") || tagContent.startsWith("!")) {
                    // Self-closing tags, processing instructions, comments, CDATA: append directly
                    result.append(xml, i, tagEnd + 1);
                    i = tagEnd + 1;
                } else if (isClosing) {
                    String tagName = extractTagName(tagContent.substring(1));
                    // Before closing, fill in all unclosed tags above this tag on the stack
                    int lastIdx = openTags.lastIndexOf(tagName);
                    if (lastIdx >= 0) {
                        for (int j = openTags.size() - 1; j > lastIdx; j--) {
                            result.append("</").append(openTags.get(j)).append(">");
                        }
                        openTags.subList(lastIdx, openTags.size()).clear();
                    }
                    result.append(xml, i, tagEnd + 1);
                    i = tagEnd + 1;
                } else {
                    String tagName = extractTagName(tagContent);
                    openTags.add(tagName);
                    result.append(xml, i, tagEnd + 1);
                    i = tagEnd + 1;
                }
            } else {
                result.append(xml.charAt(i));
                i++;
            }
        }

        // Fill in all remaining unclosed tags at the end of the document (closed in reverse order)
        for (int j = openTags.size() - 1; j >= 0; j--) {
            result.append("</").append(openTags.get(j)).append(">");
        }

        return result.toString();
    }

    /**
     * Extracts the tag name from tag content (the full name including the namespace prefix).
     *
     * <p>For example {@code "p:txBody"} -> {@code "p:txBody"}, the whole prefix and local name serve as the tag name.</p>
     */
    private String extractTagName(String tagStr) {
        // Take the token before the first whitespace character, e.g. "p:txBody" or "a:bodyPr wrap=\"square\""
        // -> "p:txBody" or "a:bodyPr"
        int spaceIdx = tagStr.indexOf(' ');
        if (spaceIdx >= 0) {
            return tagStr.substring(0, spaceIdx).trim();
        }
        return tagStr.trim();
    }

    /**
     * Reads the maximum file size from kb_system_config
     */
    private long getMaxFileSizeFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.max.size");
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 20971520L; // Default 20MB
    }

    /**
     * Reads the allowed file type list from kb_system_config
     */
    private List<String> getAllowedFileTypesFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.allowed.types");
        if (value != null && !value.isBlank()) {
            return List.of(value.toLowerCase().split(","));
        }
        return List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma");
    }
}
