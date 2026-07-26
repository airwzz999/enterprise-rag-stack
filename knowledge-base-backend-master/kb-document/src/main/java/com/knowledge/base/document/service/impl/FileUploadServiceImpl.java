package com.knowledge.base.document.service.impl;

import com.knowledge.base.document.dto.FileUploadResponse;
import com.knowledge.base.document.feign.FileServiceFeignClient;
import com.knowledge.base.document.service.FileUploadService;
import com.knowledge.base.document.util.CustomMultipartFile;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * File upload service implementation class
 *
 * <p>Calls the file service via a Feign client</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Resource
    private FileServiceFeignClient fileServiceFeignClient;

    /**
     * Internal domains (domains that do not need to be uploaded)
     */
    private static final Set<String> INTERNAL_DOMAINS = new HashSet<>(
            Arrays.asList("rustfs", "localhost", "127.0.0.1"));

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();

        try {
            // Call the kb-file file service to upload the file to RUSTFS storage
            var response = fileServiceFeignClient.uploadFile(
                    file,
                    "document",  // fileType
                    1,           // accessLevel: team visible
                    null         // teamId
            );

            if (response != null && response.getCode() == 200) {
                var uploadResponse = response.getData();
                if (uploadResponse != null) {
                    String fileUrl = uploadResponse.getFileUrl();
                    log.info("File uploaded to kb-file RUSTFS successfully: fileName={}, fileUrl={}", originalFilename, fileUrl);
                    return fileUrl;
                }
            }

            // Feign call returned a non-200 status
            log.error("File service Feign call returned a non-200 status (code={}), fileName={}",
                    response != null ? response.getCode() : "null", originalFilename);
            throw new RuntimeException("Failed to upload file to the file server, response code: " +
                    (response != null ? response.getCode() : "null"));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file to kb-file: fileName={}", originalFilename, e);
            throw new RuntimeException("Failed to upload file to the file server: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadBytes(byte[] bytes, String fileName, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File content must not be empty");
        }

        try {
            // Convert the byte array into a MultipartFile
            MultipartFile file = new CustomMultipartFile(
                    bytes,
                    "file",
                    fileName,
                    contentType != null ? contentType : "application/octet-stream"
            );

            return uploadFile(file);
        } catch (Exception e) {
            log.error("Byte array upload failed", e);
            throw new RuntimeException("Byte array upload failed: " + e.getMessage());
        }
    }

    @Override
    public String uploadImageFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("Image URL must not be empty");
        }

        // If it's an internal URL, return it directly
        if (!isExternalImageUrl(imageUrl)) {
            return imageUrl;
        }

        try {
            log.info("Starting to download image from external URL: imageUrl={}", imageUrl);

            // Call the file service to convert the URL
            var response = fileServiceFeignClient.convertImageUrl(imageUrl);

            if (response != null && response.getCode() == 200) {
                var convertResponse = response.getData();
                if (convertResponse != null) {
                    // Prefer the newUrl field (the field returned by the file service's UrlConvertResponse)
                    // If newUrl is blank, try the convertedUrl field instead
                    String newUrl = convertResponse.getNewUrl();
                    if (!StringUtils.hasText(newUrl)) {
                        newUrl = convertResponse.getConvertedUrl();
                    }

                    // If a valid URL was obtained, return it directly
                    if (StringUtils.hasText(newUrl)) {
                        log.info("External image uploaded successfully: originalUrl={}, newUrl={}", imageUrl, newUrl);
                        return newUrl;
                    }
                }
            }

            // The Feign call failed or returned an empty URL
            log.error("File service URL conversion failed or returned an empty URL: imageUrl={}", imageUrl);
            throw new RuntimeException("Failed to upload external image to the file server");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload image from URL: imageUrl={}", imageUrl, e);
            throw new RuntimeException("Failed to upload image from URL: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isExternalImageUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        // Check whether it is an HTTP(S) link
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Check whether it is an internal domain
        for (String domain : INTERNAL_DOMAINS) {
            if (url.contains(domain)) {
                return false;
            }
        }

        return true;
    }

}
