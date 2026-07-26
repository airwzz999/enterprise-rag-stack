package com.knowledge.base.document.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * File upload service interface
 *
 * <p>Supports file upload via the rustfs file server</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface FileUploadService {

    /**
     * Uploads a file to the rustfs server
     *
     * @param file file
     * @return file access URL
     */
    String uploadFile(MultipartFile file);

    /**
     * Uploads a byte array to the rustfs server
     *
     * @param bytes      file byte array
     * @param fileName   file name
     * @param contentType content type
     * @return file access URL
     */
    String uploadBytes(byte[] bytes, String fileName, String contentType);

    /**
     * Downloads an image from a URL and uploads it to rustfs
     *
     * @param imageUrl original image URL
     * @return new image access URL
     */
    String uploadImageFromUrl(String imageUrl);

    /**
     * Determines whether a URL is an external image URL
     *
     * @param url URL address
     * @return whether it is an external URL
     */
    boolean isExternalImageUrl(String url);
}
