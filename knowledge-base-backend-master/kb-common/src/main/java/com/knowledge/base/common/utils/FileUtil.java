package com.knowledge.base.common.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * File utility class
 *
 * <p>Provides utility methods related to file operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class FileUtil {

    /**
     * Allowed image extensions
     */
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    );

    /**
     * Allowed document extensions
     */
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "html", "htm"
    );

    /**
     * Allowed video extensions
     */
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"
    );

    /**
     * Allowed audio extensions
     */
    private static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma"
    );

    /**
     * Get a file's extension
     *
     * @param filename the filename
     * @return the extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Check whether it is an image file
     *
     * @param filename the filename
     * @return whether it is an image
     */
    public static boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * Check whether it is a document file
     *
     * @param filename the filename
     * @return whether it is a document
     */
    public static boolean isDocumentFile(String filename) {
        String extension = getFileExtension(filename);
        return DOCUMENT_EXTENSIONS.contains(extension);
    }

    /**
     * Check whether it is a video file
     *
     * @param filename the filename
     * @return whether it is a video
     */
    public static boolean isVideoFile(String filename) {
        String extension = getFileExtension(filename);
        return VIDEO_EXTENSIONS.contains(extension);
    }

    /**
     * Check whether it is an audio file
     *
     * @param filename the filename
     * @return whether it is audio
     */
    public static boolean isAudioFile(String filename) {
        String extension = getFileExtension(filename);
        return AUDIO_EXTENSIONS.contains(extension);
    }

    /**
     * Detect the file type
     *
     * @param filename the filename
     * @param mimeType the MIME type
     * @return the file type
     */
    public static String detectFileType(String filename, String mimeType) {
        if (isImageFile(filename)) {
            return "IMAGE";
        } else if (isDocumentFile(filename)) {
            return "DOCUMENT";
        } else if (isVideoFile(filename)) {
            return "VIDEO";
        } else if (isAudioFile(filename)) {
            return "AUDIO";
        } else {
            return "OTHER";
        }
    }

    /**
     * Generate a unique filename
     *
     * @param originalFilename the original filename
     * @return the unique filename
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    /**
     * Ensure a directory exists
     *
     * @param dirPath the directory path
     * @throws IOException on IO error
     */
    public static void ensureDirExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Get a relative path
     *
     * @param fullPath the full path
     * @param basePath the base path
     * @return the relative path
     */
    public static String getRelativePath(String fullPath, String basePath) {
        Path full = Paths.get(fullPath).normalize();
        Path base = Paths.get(basePath).normalize();
        return base.relativize(full).toString();
    }

    /**
     * Get a human-readable file size
     *
     * @param size the file size (bytes)
     * @return the human-readable format
     */
    public static String getReadableFileSize(long size) {
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
     * Validate whether a filename is valid
     *
     * @param filename the filename
     * @return whether it is valid
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        // Check for illegal characters
        String[] illegalChars = {"/", "\\", ":", "*", "?", "\"", "<", ">", "|", "\0"};
        for (String illegalChar : illegalChars) {
            if (filename.contains(illegalChar)) {
                return false;
            }
        }

        // Check for reserved names
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
                "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4",
                "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;
        for (String reserved : reservedNames) {
            if (reserved.equalsIgnoreCase(nameWithoutExt)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Read a byte array from a MultipartFile
     *
     * @param file the MultipartFile
     * @return the byte array
     * @throws IOException on IO error
     */
    public static byte[] readBytes(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}
