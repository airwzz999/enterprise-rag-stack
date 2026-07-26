package com.knowledge.base.file.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.file.dto.FileQueryDTO;
import com.knowledge.base.file.dto.FileUploadDTO;
import com.knowledge.base.file.service.FileService;
import com.knowledge.base.file.vo.BatchConvertResponse;
import com.knowledge.base.file.vo.FileInfoVO;
import com.knowledge.base.file.vo.UrlConvertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * File management controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "File management endpoints")
public class FileController {

    private final FileService fileService;

    /**
     * Upload a file
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Upload a single file")
    @OperationLog(module = "File Management", operation = "Upload File", description = "Upload file")
    public Result<FileInfoVO> uploadFile(
            @RequestPart("file") MultipartFile file,
            FileUploadDTO dto) {
        FileInfoVO fileInfo = fileService.uploadFile(file, dto);
        return Result.success(fileInfo);
    }

    /**
     * Upload files in batch
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "Batch upload files", description = "Upload multiple files in a batch")
    @OperationLog(module = "File Management", operation = "Batch Upload", description = "Batch upload files")
    public Result<List<FileInfoVO>> uploadFiles(
            @RequestPart("files") MultipartFile[] files,
            FileUploadDTO dto) {
        List<FileInfoVO> fileInfos = fileService.uploadFiles(files, dto);
        return Result.success(fileInfos);
    }

    /**
     * Download a file
     */
    @GetMapping("/download/{fileId}/**")
    @Operation(summary = "Download file", description = "Download the specified file")
    @OperationLog(module = "File Management", operation = "Download File", description = "Download file")
    public void downloadFile(
            @PathVariable String fileId,
            HttpServletResponse response) throws IOException {
        // Extract the real file ID from fileId (strip the extension)
        Long realFileId = extractFileId(fileId);
        fileService.downloadFile(realFileId, response);
    }

    /**
     * Delete a file
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete file", description = "Delete the specified file")
    @OperationLog(module = "File Management", operation = "Delete File", description = "Delete file")
    public Result<Boolean> deleteFile(@PathVariable Long fileId) {
        Boolean result = fileService.deleteFile(fileId);
        return Result.success(result);
    }

    /**
     * Delete files in batch
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete files", description = "Delete multiple files in a batch")
    @OperationLog(module = "File Management", operation = "Batch Delete", description = "Batch delete files")
    public Result<Boolean> batchDeleteFiles(@RequestBody List<Long> fileIds) {
        Boolean result = fileService.batchDeleteFiles(fileIds);
        return Result.success(result);
    }

    /**
     * Get file details
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "Get file details", description = "Get file details by ID")
    public Result<FileInfoVO> getFileInfo(@PathVariable Long fileId) {
        FileInfoVO fileInfo = fileService.getFileInfo(fileId);
        return Result.success(fileInfo);
    }

    /**
     * Query files with pagination
     */
    @PostMapping("/page")
    @Operation(summary = "Query files with pagination", description = "Query the file list with pagination")
    public Result<PageResult<FileInfoVO>> pageFiles(@RequestBody FileQueryDTO dto) {
        PageResult<FileInfoVO> pageResult = fileService.pageFiles(dto);
        return Result.success(pageResult);
    }

    /**
     * Get the file preview URL
     */
    @GetMapping("/preview/{fileId}")
    @Operation(summary = "Get file preview URL", description = "Get the file preview URL")
    public Result<String> getPreviewUrl(@PathVariable String fileId) {
        // Extract the real file ID from fileId (strip the extension)
        Long realFileId = extractFileId(fileId);
        String previewUrl = fileService.getPreviewUrl(realFileId);
        return Result.success(previewUrl);
    }

    /**
     * Preview a file (returns the file content directly)
     */
    @GetMapping("/preview/{fileId}/**")
    @Operation(summary = "Preview file", description = "Preview the content of the specified file")
    public void previewFile(
            @PathVariable String fileId,
            HttpServletResponse response) throws IOException {
        // Extract the real file ID from fileId (strip the extension)
        Long realFileId = extractFileId(fileId);
        fileService.previewFile(realFileId, response);
    }

    /**
     * Convert an image from a URL (download it and upload it into the system)
     */
    @PostMapping("/convert-url")
    @Operation(summary = "Convert image from URL", description = "Download an external image and upload it into the system")
    @OperationLog(module = "File Management", operation = "URL Conversion", description = "Convert image from URL")
    public Result<UrlConvertResponse> convertFromUrl(@RequestParam String imageUrl) {
        UrlConvertResponse response = fileService.convertFromUrl(imageUrl);
        return Result.success(response);
    }

    /**
     * Batch convert image URLs
     */
    @PostMapping("/batch-convert")
    @Operation(summary = "Batch convert image URLs", description = "Batch download external images and upload them into the system")
    @OperationLog(module = "File Management", operation = "Batch Conversion", description = "Batch convert image URLs")
    public Result<BatchConvertResponse> batchConvertUrls(@RequestBody List<String> imageUrls) {
        BatchConvertResponse response = fileService.batchConvertUrls(imageUrls);
        return Result.success(response);
    }

    /**
     * Convert file format
     */
    @PostMapping("/convert/{fileId}")
    @Operation(summary = "Convert file format", description = "Convert the format of a file")
    @OperationLog(module = "File Management", operation = "Format Conversion", description = "Convert file format")
    public Result<Long> convertFileFormat(
            @PathVariable Long fileId,
            @RequestParam String targetFormat) {
        Long convertedFileId = fileService.convertFileFormat(fileId, targetFormat);
        return Result.success(convertedFileId);
    }

    /**
     * Stream the HLS master playlist
     */
    @GetMapping("/stream/{fileId}/master.m3u8")
    @Operation(summary = "HLS playlist", description = "Get the HLS master playlist")
    public void streamMasterPlaylist(
            @PathVariable Long fileId,
            HttpServletResponse response) throws IOException {
        fileService.streamMasterPlaylist(fileId, response);
    }

    /**
     * Stream an HLS TS segment
     */
    @GetMapping("/stream/{fileId}/{segment:.+\\.ts}")
    @Operation(summary = "HLS segment", description = "Get an HLS TS segment")
    public void streamSegment(
            @PathVariable Long fileId,
            @PathVariable String segment,
            HttpServletResponse response) throws IOException {
        fileService.streamSegment(fileId, segment, response);
    }

    /**
     * Get a thumbnail
     */
    @GetMapping("/thumbnail/{fileId}")
    @Operation(summary = "Get thumbnail", description = "Get a video thumbnail")
    public void getThumbnail(
            @PathVariable Long fileId,
            HttpServletResponse response) throws IOException {
        fileService.getThumbnail(fileId, response);
    }

    /**
     * Extract the real file ID from fileId (strip the extension)
     *
     * @param fileId file ID that may include an extension
     * @return the real file ID
     */
    private Long extractFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            throw new IllegalArgumentException("File ID must not be empty");
        }

        // Find the first dot and take the part before it as the real file ID
        int dotIndex = fileId.indexOf('.');
        String realIdStr = dotIndex > 0 ? fileId.substring(0, dotIndex) : fileId;

        try {
            return Long.parseLong(realIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid file ID format: " + fileId);
        }
    }
}
