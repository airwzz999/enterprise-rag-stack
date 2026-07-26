package com.knowledge.base.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.file.dto.FileQueryDTO;
import com.knowledge.base.file.dto.FileUploadDTO;
import com.knowledge.base.file.entity.FileInfo;
import com.knowledge.base.file.vo.FileInfoVO;
import com.knowledge.base.file.vo.UrlConvertResponse;
import com.knowledge.base.file.vo.BatchConvertResponse;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * File Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface FileService extends IService<FileInfo> {

    /**
     * Upload a file
     *
     * @param file the file
     * @param dto  upload parameters
     * @return file info
     */
    FileInfoVO uploadFile(MultipartFile file, FileUploadDTO dto);

    /**
     * Upload files in batch
     *
     * @param files list of files
     * @param dto   upload parameters
     * @return list of file info
     */
    List<FileInfoVO> uploadFiles(MultipartFile[] files, FileUploadDTO dto);

    /**
     * Download a file
     *
     * @param fileId     file ID
     * @param response   HTTP response
     */
    void downloadFile(Long fileId, HttpServletResponse response) throws IOException;

    /**
     * Get a file stream
     *
     * @param fileId file ID
     * @return file stream
     */
    InputStream getFileStream(Long fileId) throws IOException;

    /**
     * Delete a file
     *
     * @param fileId file ID
     * @return whether the deletion succeeded
     */
    Boolean deleteFile(Long fileId);

    /**
     * Delete files in batch
     *
     * @param fileIds list of file IDs
     * @return whether the deletion succeeded
     */
    Boolean batchDeleteFiles(List<Long> fileIds);

    /**
     * Get file details
     *
     * @param fileId file ID
     * @return file info
     */
    FileInfoVO getFileInfo(Long fileId);

    /**
     * Query files with pagination
     *
     * @param dto query parameters
     * @return paginated result
     */
    PageResult<FileInfoVO> pageFiles(FileQueryDTO dto);

    /**
     * Get the file preview URL
     *
     * @param fileId file ID
     * @return preview URL
     */
    String getPreviewUrl(Long fileId);

    /**
     * Preview a file (returns the file content directly)
     *
     * @param fileId file ID
     * @param response HTTP response
     */
    void previewFile(Long fileId, HttpServletResponse response) throws IOException;

    /**
     * Convert file format
     *
     * @param fileId      file ID
     * @param targetFormat target format
     * @return converted file ID
     */
    Long convertFileFormat(Long fileId, String targetFormat);

    /**
     * Initialize a resumable upload
     *
     * @param fileHash file hash
     * @param fileName file name
     * @param totalSize total size
     * @param chunkCount chunk count
     * @return session ID
     */
    String initResumableUpload(String fileHash, String fileName, long totalSize, int chunkCount);

    /**
     * Upload a chunk
     *
     * @param sessionId session ID
     * @param chunkIndex chunk index
     * @param chunkFile chunk file
     * @return whether the upload succeeded
     */
    Boolean uploadChunk(String sessionId, int chunkIndex, MultipartFile chunkFile);

    /**
     * Get the uploaded chunks
     *
     * @param sessionId session ID
     * @return list of uploaded chunk indexes
     */
    int[] getUploadedChunks(String sessionId);

    /**
     * Merge chunks
     *
     * @param sessionId session ID
     * @param dto upload parameters
     * @return file info
     */
    FileInfoVO mergeChunks(String sessionId, FileUploadDTO dto);

    /**
     * Convert an image from a URL (download it and upload it into the system)
     *
     * @param imageUrl external image URL
     * @return conversion result
     */
    UrlConvertResponse convertFromUrl(String imageUrl);

    /**
     * Batch convert image URLs
     *
     * @param imageUrls list of external image URLs
     * @return batch conversion result
     */
    BatchConvertResponse batchConvertUrls(List<String> imageUrls);

    /**
     * Stream the HLS master playlist
     *
     * @param fileId file ID
     * @param response HTTP response
     */
    void streamMasterPlaylist(Long fileId, HttpServletResponse response) throws IOException;

    /**
     * Stream an HLS TS segment
     *
     * @param fileId file ID
     * @param segment TS segment file name
     * @param response HTTP response
     */
    void streamSegment(Long fileId, String segment, HttpServletResponse response) throws IOException;

    /**
     * Get a thumbnail
     *
     * @param fileId file ID
     * @param response HTTP response
     */
    void getThumbnail(Long fileId, HttpServletResponse response) throws IOException;
}
