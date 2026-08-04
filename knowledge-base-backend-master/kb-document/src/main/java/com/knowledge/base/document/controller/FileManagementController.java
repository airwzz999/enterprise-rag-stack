package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.entity.FileMetadata;
import com.knowledge.base.document.service.FileManagementService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.FileMetadataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/file-management")
@Tag(name = "File Management", description = "File management center endpoints")
public class FileManagementController {

    @Resource
    private FileManagementService fileManagementService;

    /**
     * Uploads a file
     *
     * @param file     file
     * @param isPublic whether public
     * @return file metadata
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Uploads a file and saves its metadata")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<FileMetadataVO> uploadFile(
            @Parameter(description = "File", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Whether public")
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Upload file request: userId={}, fileName={}", userId, file.getOriginalFilename());

        FileMetadata metadata = fileManagementService.uploadFile(file, userId, isPublic);
        return Result.success("Upload successful", convertToVO(metadata));
    }

    /**
     * Gets the file list
     *
     * @return file list
     */
    @GetMapping("/list")
    @Operation(summary = "Get file list", description = "Gets the current user's file list")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<FileMetadataVO>> getFileList() {
        Long userId = UserContext.getCurrentUserId();
        log.info("Get file list request: userId={}", userId);

        List<FileMetadata> files = fileManagementService.getFileList(userId);
        List<FileMetadataVO> vos = files.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * Gets the file list filtered by category
     *
     * @param category file category
     * @return file list
     */
    @GetMapping("/list/{category}")
    @Operation(summary = "Get file list by category", description = "Gets the file list filtered by file category")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<FileMetadataVO>> getFileListByCategory(
            @Parameter(description = "File category", required = true)
            @PathVariable("category") String category) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Get file list by category request: userId={}, category={}", userId, category);

        List<FileMetadata> files = fileManagementService.getFileListByCategory(userId, category);
        List<FileMetadataVO> vos = files.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * Gets file details
     *
     * @param fileId file ID
     * @return file details
     */
    @GetMapping("/detail/{fileId}")
    @Operation(summary = "Get file details", description = "Gets detailed file information")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<FileMetadataVO> getFileDetail(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Get file details request: fileId={}, userId={}", fileId, userId);
        FileMetadata metadata = fileManagementService.getFileDetail(fileId, userId);
        return Result.success(convertToVO(metadata));
    }

    /**
     * Renames a file
     *
     * @param fileId     file ID
     * @param newFileName new file name
     * @return whether successful
     */
    @PutMapping("/rename/{fileId}")
    @Operation(summary = "Rename file", description = "Renames a file")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> renameFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId,
            @Parameter(description = "New file name", required = true)
            @RequestParam("newFileName") String newFileName) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Rename file request: userId={}, fileId={}, newFileName={}", userId, fileId, newFileName);

        Boolean result = fileManagementService.renameFile(fileId, newFileName, userId);
        return Result.success("Renamed successfully", result);
    }

    /**
     * Deletes a file
     *
     * @param fileId file ID
     * @return whether successful
     */
    @DeleteMapping("/delete/{fileId}")
    @Operation(summary = "Delete file", description = "Deletes a file")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> deleteFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Delete file request: userId={}, fileId={}", userId, fileId);

        Boolean result = fileManagementService.deleteFile(fileId, userId);
        return Result.success("Deleted successfully", result);
    }

    /**
     * Batch-deletes files
     *
     * @param fileIds file ID list
     * @return delete count
     */
    @DeleteMapping("/batch-delete")
    @Operation(summary = "Batch delete files", description = "Deletes multiple files in a batch")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Integer> batchDeleteFiles(
            @Parameter(description = "File ID list", required = true)
            @RequestBody List<Long> fileIds) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Batch delete files request: userId={}, fileIds={}", userId, fileIds);

        Integer count = fileManagementService.batchDeleteFiles(fileIds, userId);
        return Result.success("Delete complete", count);
    }

    /**
     * Updates file permission
     *
     * @param fileId   file ID
     * @param isPublic whether public
     * @return whether successful
     */
    @PutMapping("/permission/{fileId}")
    @Operation(summary = "Update file permission", description = "Updates a file's access permission")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> updateFilePermission(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId,
            @Parameter(description = "Whether public", required = true)
            @RequestParam("isPublic") Boolean isPublic) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Update file permission request: userId={}, fileId={}, isPublic={}", userId, fileId, isPublic);

        Boolean result = fileManagementService.updateFilePermission(fileId, isPublic, userId);
        return Result.success("Permission updated successfully", result);
    }

    /**
     * Increments the download count
     *
     * @param fileId file ID
     * @return whether successful
     */
    @PostMapping("/download/{fileId}")
    @Operation(summary = "Download file", description = "Downloads a file and increments its download count")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<Boolean> downloadFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Download file request: fileId={}, userId={}", fileId, userId);
        fileManagementService.incrementDownloadCount(fileId, userId);
        return Result.success("Downloaded successfully", true);
    }

    /**
     * Gets file statistics
     *
     * @return statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get file statistics", description = "Gets file statistics")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<Map<String, Object>> getFileStatistics() {
        Long userId = UserContext.getCurrentUserId();
        log.info("Get file statistics request: userId={}", userId);

        Map<String, Object> statistics = fileManagementService.getFileStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * Copies a file
     *
     * @param fileId file ID
     * @return new file metadata
     */
    @PostMapping("/copy/{fileId}")
    @Operation(summary = "Copy file", description = "Copies a file")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<FileMetadataVO> copyFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Copy file request: userId={}, fileId={}", userId, fileId);

        FileMetadata newFile = fileManagementService.copyFile(fileId, userId);
        return Result.success("Copied successfully", convertToVO(newFile));
    }

    /**
     * Searches files
     *
     * @param keyword search keyword
     * @return file list
     */
    @GetMapping("/stream/{fileId}")
    @Operation(summary = "Stream file", description = "Streams audio/video file content (proxying RUSTFS), supports HTTP Range requests")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public void streamFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Stream file request: fileId={}, userId={}", fileId, userId);
        fileManagementService.streamFile(fileId, userId, request, response);
    }

    @GetMapping("/preview/{fileId}/slides")
    @Operation(summary = "Get PPTX slide images", description = "Renders a PPTX file into a PNG image (Base64) for each slide")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<String>> getPptxSlideImages(
            @Parameter(description = "File ID", required = true)
            @PathVariable("fileId") Long fileId) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Get PPTX slide images request: fileId={}, userId={}", fileId, userId);
        List<String> slideImages = fileManagementService.getPptxSlideImages(fileId, userId);
        return Result.success(slideImages);
    }

    @GetMapping("/search")
    @Operation(summary = "Search files", description = "Searches files by keyword")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<FileMetadataVO>> searchFiles(
            @Parameter(description = "Search keyword", required = true)
            @RequestParam("keyword") String keyword) {

        Long userId = UserContext.getCurrentUserId();
        log.info("Search files request: userId={}, keyword={}", userId, keyword);

        List<FileMetadata> files = fileManagementService.searchFiles(userId, keyword);
        List<FileMetadataVO> vos = files.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * Converts to a VO object
     */
    private FileMetadataVO convertToVO(FileMetadata metadata) {
        FileMetadataVO vo = new FileMetadataVO();
        vo.setId(metadata.getId());
        vo.setFileName(metadata.getFileName());
        vo.setOriginalFileName(metadata.getOriginalFileName());
        vo.setFileExtension(metadata.getFileExtension());
        vo.setFileSize(metadata.getFileSize());
        vo.setFileSizeReadable(formatFileSize(metadata.getFileSize()));
        vo.setContentType(metadata.getContentType());
        vo.setAccessUrl(metadata.getAccessUrl());
        vo.setFileCategory(metadata.getFileCategory());
        vo.setUploaderId(metadata.getUploaderId());
        vo.setUploaderName(metadata.getUploaderName());
        vo.setIsPublic(metadata.getIsPublic());
        vo.setDownloadCount(metadata.getDownloadCount());
        vo.setCreatedAt(metadata.getCreatedAt());
        vo.setUpdatedAt(metadata.getUpdatedAt());
        vo.setLastAccessTime(metadata.getLastAccessTime());
        vo.setWidth(metadata.getWidth());
        vo.setHeight(metadata.getHeight());
        vo.setThumbnailUrl(metadata.getThumbnailUrl());
        return vo;
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
}
