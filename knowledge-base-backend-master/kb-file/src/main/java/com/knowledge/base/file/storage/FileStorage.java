package com.knowledge.base.file.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * File storage abstraction interface
 *
 * <p>Defines the standard file storage operations, supporting multiple storage backends
 * (local file system, cloud storage, etc.)</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface FileStorage {

    /**
     * Upload a file
     *
     * @param inputStream file input stream
     * @param relativePath relative storage path
     * @param fileSize file size
     * @return whether the upload succeeded
     */
    boolean upload(InputStream inputStream, String relativePath, long fileSize);

    /**
     * Download a file
     *
     * @param relativePath relative storage path
     * @param outputStream output stream
     * @return file size
     */
    long download(String relativePath, OutputStream outputStream);

    /**
     * Get a file input stream
     *
     * @param relativePath relative storage path
     * @return file input stream
     */
    InputStream getInputStream(String relativePath);

    /**
     * Delete a file
     *
     * @param relativePath relative storage path
     * @return whether the deletion succeeded
     */
    boolean delete(String relativePath);

    /**
     * Check whether a file exists
     *
     * @param relativePath relative storage path
     * @return whether the file exists
     */
    boolean exists(String relativePath);

    /**
     * Get the file size
     *
     * @param relativePath relative storage path
     * @return file size
     */
    long getFileSize(String relativePath);

    /**
     * Get the storage type
     *
     * @return storage type identifier
     */
    String getStorageType();
}
