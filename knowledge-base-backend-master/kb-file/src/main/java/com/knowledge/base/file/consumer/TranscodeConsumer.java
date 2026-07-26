package com.knowledge.base.file.consumer;

import com.knowledge.base.file.message.TranscodeMessage;
import com.knowledge.base.file.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Transcode message consumer
 * Asynchronously consumes the transcode queue, running FFmpeg transcoding and thumbnail generation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodeConsumer {

    private final MediaService mediaService;

    /**
     * Consume a transcode message
     */
    @RabbitListener(queues = "#{@transcodeQueue.name}")
    public void handleTranscodeMessage(TranscodeMessage message) {
        log.info("Received transcode message: fileId={}, targetFormat={}", message.getFileId(), message.getTargetFormat());

        try {
            // Update status to processing
            mediaService.updateTranscodeStatus(message.getFileId(), "PROCESSING");

            // Perform HLS transcoding
            mediaService.transcodeToHls(message.getFileId());

            // Generate the thumbnail
            mediaService.generateThumbnail(message.getFileId());

            // Update status to done
            mediaService.updateTranscodeStatus(message.getFileId(), "DONE");

            log.info("Transcoding completed: fileId={}", message.getFileId());
        } catch (Exception e) {
            log.error("Transcoding failed: fileId={}, error={}", message.getFileId(), e.getMessage(), e);
            mediaService.updateTranscodeStatus(message.getFileId(), "FAILED");
        }
    }
}
