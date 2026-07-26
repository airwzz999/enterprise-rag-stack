package com.knowledge.base.file.service;

import com.knowledge.base.file.vo.MediaMetadata;

/**
 * Media processing service interface
 * Responsible for metadata extraction, HLS transcoding, and thumbnail generation for audio/video files
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface MediaService {

    /**
     * Extract media file metadata (duration/resolution/bitrate/codec)
     *
     * @param fileId file ID
     * @return media metadata
     */
    MediaMetadata probeMediaInfo(Long fileId);

    /**
     * Transcode a video file to multi-bitrate HLS (360p + 720p)
     * Executed asynchronously, invoked by the RabbitMQ consumer
     *
     * @param fileId file ID
     * @return relative path of the HLS playlist
     */
    String transcodeToHls(Long fileId);

    /**
     * Generate a thumbnail from a video file (keyframe snapshot)
     *
     * @param fileId file ID
     * @return relative path of the thumbnail
     */
    String generateThumbnail(Long fileId);

    /**
     * Update the transcode status of a file
     *
     * @param fileId file ID
     * @param status transcode status: PENDING/PROCESSING/DONE/FAILED
     */
    void updateTranscodeStatus(Long fileId, String status);
}
