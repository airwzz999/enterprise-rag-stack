package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.document.entity.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * File management service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface FileManagementService extends IService<FileMetadata> {

    /**
     * Uploads a file and saves its metadata
     *
     * @param file     file
     * @param userId   user ID
     * @param isPublic whether public
     * @return file metadata
     */
    FileMetadata uploadFile(MultipartFile file, Long userId, Boolean isPublic);

    /**
     * Gets the file list
     *
     * @param userId user ID
     * @return file list
     */
    List<FileMetadata> getFileList(Long userId);

    /**
     * Gets the file list filtered by category
     *
     * @param userId       user ID
     * @param fileCategory file category
     * @return file list
     */
    List<FileMetadata> getFileListByCategory(Long userId, String fileCategory);

    /**
     * Gets file details
     *
     * @param fileId file ID
     * @param userId requesting user ID
     * @return file metadata
     */
    FileMetadata getFileDetail(Long fileId, Long userId);

    /**
     * Renames a file
     *
     * @param fileId     file ID
     * @param newFileName new file name
     * @param userId     user ID
     * @return whether successful
     */
    Boolean renameFile(Long fileId, String newFileName, Long userId);

    /**
     * Deletes a file
     *
     * @param fileId file ID
     * @param userId user ID
     * @return whether successful
     */
    Boolean deleteFile(Long fileId, Long userId);

    /**
     * Batch-deletes files
     *
     * @param fileIds file ID list
     * @param userId  user ID
     * @return delete count
     */
    Integer batchDeleteFiles(List<Long> fileIds, Long userId);

    /**
     * Updates the file access permission
     *
     * @param fileId   file ID
     * @param isPublic whether public
     * @param userId   user ID
     * @return whether successful
     */
    Boolean updateFilePermission(Long fileId, Boolean isPublic, Long userId);

    /**
     * Increments the download count
     *
     * @param fileId file ID
     * @param userId requesting user ID
     */
    void incrementDownloadCount(Long fileId, Long userId);

    /**
     * Updates the last access time
     *
     * @param fileId file ID
     */
    void updateLastAccessTime(Long fileId);

    /**
     * Gets file statistics
     *
     * @param userId user ID
     * @return statistics Map
     */
    Map<String, Object> getFileStatistics(Long userId);

    /**
     * Copies a file
     *
     * @param fileId file ID
     * @param userId user ID
     * @return new file metadata
     */
    FileMetadata copyFile(Long fileId, Long userId);

    /**
     * Searches files
     *
     * @param userId  user ID
     * @param keyword search keyword
     * @return file list
     */
    List<FileMetadata> searchFiles(Long userId, String keyword);

    /**
     * Streams file content (proxied from RUSTFS to the browser)
     * Supports HTTP Range requests, returning 206 Partial Content for browser audio/video element playback
     *
     * @param fileId   file ID
     * @param userId   requesting user ID
     * @param request  HTTP request (used to read the Range header)
     * @param response HTTP response
     */
    void streamFile(Long fileId, Long userId, HttpServletRequest request, HttpServletResponse response);

    /**
     * Renders each slide of a PPTX file into a PNG image (Base64-encoded)
     *
     * @param fileId file ID
     * @param userId requesting user ID
     * @return list of data:image/png;base64 strings, one per slide
     */
    List<String> getPptxSlideImages(Long fileId, Long userId);
}
