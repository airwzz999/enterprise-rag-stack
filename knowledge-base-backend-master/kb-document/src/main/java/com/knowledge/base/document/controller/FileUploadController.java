package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.response.BatchConvertResponse;
import com.knowledge.base.document.dto.response.BatchUploadResponse;
import com.knowledge.base.document.dto.response.FileUploadResponse;
import com.knowledge.base.document.dto.response.ImageConvertResponse;
import com.knowledge.base.document.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File upload Controller
 *
 * <p>Provides file upload related endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/files")
@Tag(name = "File Upload", description = "File upload management endpoints")
public class FileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    /**
     * Uploads a single file
     *
     * @param file file
     * @return file access URL
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Uploads a file and returns its access URL")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<FileUploadResponse> uploadFile(
            @Parameter(description = "File", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("Upload file request: fileName={}", file.getOriginalFilename());

        String fileUrl = fileUploadService.uploadFile(file);

        FileUploadResponse response = FileUploadResponse.builder()
                .url(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileSizeReadable(formatFileSize(file.getSize()))
                .build();

        return Result.success("Upload successful", response);
    }

    /**
     * Uploads an image from a URL
     *
     * @param imageUrl image URL
     * @return new image access URL
     */
    @PostMapping("/upload-from-url")
    @Operation(summary = "Upload image from URL", description = "Downloads an external image and uploads it to the file server")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<ImageConvertResponse> uploadFromUrl(
            @Parameter(description = "Image URL", required = true)
            @RequestParam("imageUrl") String imageUrl) {
        log.info("Upload image from URL: imageUrl={}", imageUrl);

        String newUrl = fileUploadService.uploadImageFromUrl(imageUrl);

        ImageConvertResponse response = ImageConvertResponse.builder()
                .originalUrl(imageUrl)
                .convertedUrl(newUrl)
                .build();

        return Result.success("Upload successful", response);
    }

    /**
     * Batch-uploads images
     *
     * @param files file list
     * @return list of file URLs
     */
    @PostMapping("/batch-upload")
    @Operation(summary = "Batch upload files", description = "Uploads multiple files in a batch")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<BatchUploadResponse> batchUpload(
            @Parameter(description = "File list", required = true)
            @RequestParam("files") MultipartFile[] files) {
        log.info("Batch upload files request: fileCount={}", files.length);

        Map<String, String> fileUrls = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (MultipartFile file : files) {
            try {
                String url = fileUploadService.uploadFile(file);
                fileUrls.put(file.getOriginalFilename(), url);
                successCount++;
            } catch (Exception e) {
                log.error("File upload failed: fileName={}", file.getOriginalFilename(), e);
                fileUrls.put(file.getOriginalFilename(), null);
                failureCount++;
            }
        }

        BatchUploadResponse response = BatchUploadResponse.builder()
                .fileUrls(fileUrls)
                .successCount(successCount)
                .failureCount(failureCount)
                .build();

        return Result.success("Batch upload complete", response);
    }

    /**
     * Converts an image URL
     *
     * @param imageUrl image URL
     * @return converted URL
     */
    @PostMapping("/convert-url")
    @Operation(summary = "Convert image URL", description = "Converts an external image URL to a local URL")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<ImageConvertResponse> convertImageUrl(
            @Parameter(description = "Image URL", required = true)
            @RequestParam("imageUrl") String imageUrl) {
        log.info("Convert image URL request: imageUrl={}", imageUrl);

        String newUrl = fileUploadService.uploadImageFromUrl(imageUrl);

        ImageConvertResponse response = ImageConvertResponse.builder()
                .originalUrl(imageUrl)
                .convertedUrl(newUrl)
                .build();

        return Result.success("URL converted successfully", response);
    }

    /**
     * Batch-converts image URLs
     *
     * @param imageUrls list of image URLs
     * @return conversion result
     */
    @PostMapping("/batch-convert")
    @Operation(summary = "Batch convert image URLs", description = "Converts multiple external image URLs in a batch")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<BatchConvertResponse> batchConvertUrls(
            @Parameter(description = "List of image URLs", required = true)
            @RequestBody List<String> imageUrls) {
        log.info("Batch convert image URLs request: urlCount={}", imageUrls.size());

        Map<String, String> urlMappings = new HashMap<>();
        Map<String, String> errorMappings = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (String imageUrl : imageUrls) {
            try {
                String newUrl = fileUploadService.uploadImageFromUrl(imageUrl);
                urlMappings.put(imageUrl, newUrl);
                successCount++;
            } catch (Exception e) {
                log.error("Image URL conversion failed: imageUrl={}", imageUrl, e);
                errorMappings.put(imageUrl, imageUrl); // Keep the original URL on failure
                failureCount++;
            }
        }

        BatchConvertResponse response = BatchConvertResponse.builder()
                .urlMappings(urlMappings)
                .errorMappings(errorMappings)
                .successCount(successCount)
                .failureCount(failureCount)
                .build();

        return Result.success("Batch conversion complete", response);
    }

    /**
     * Formats a file size
     *
     * @param size file size (bytes)
     * @return formatted file size
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
