package com.knowledge.base.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.knowledge.base.ai.client.DocumentFeignClient;
import com.knowledge.base.ai.config.RagProperties;
import com.knowledge.base.ai.rag.entity.DocumentChunk;
import com.knowledge.base.ai.dto.kag.DocumentRecordDTO;
import com.knowledge.base.ai.rag.service.ChunkingService;
import com.knowledge.base.ai.rag.service.EmbeddingService;
import com.knowledge.base.ai.rag.service.VectorIndexService;
import com.knowledge.base.ai.vo.ReindexProgressVO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Reindex consumer
 *
 * <p>Consumes indexing tasks from RabbitMQ, running the full document → chunk →
 * embed → index pipeline. Supports full rebuild (ALL) and rebuilding specified
 * documents (BY_DOC_IDS). Uses manual ACK to ensure message reliability.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReindexConsumer {

    private final DocumentFeignClient documentFeignClient;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorIndexService vectorIndexService;
    private final RagProperties ragProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String PROGRESS_KEY_PREFIX = "rag:reindex:progress:";
    private static final int PAGE_SIZE = 50;

    @RabbitListener(queues = "#{@ragReindexQueue.name}", ackMode = "MANUAL")
    public void handleReindex(ReindexMessage message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received reindex task: taskId={}, type={}", message.getTaskId(), message.getType());

            updateProgress(message.getTaskId(), "RUNNING", 0, 0, 0);

            // DELETE_BY_DOC_IDS: only delete the vector index, do not reindex
            if (message.getType() == ReindexMessage.ReindexType.DELETE_BY_DOC_IDS
                    && message.getDocumentIds() != null) {
                int deleted = 0;
                for (Long docId : message.getDocumentIds()) {
                    try {
                        vectorIndexService.deleteByDocId(docId);
                        deleted++;
                        log.info("Deleted document vector index: documentId={}", docId);
                    } catch (Exception e) {
                        log.warn("Failed to delete document vector index: documentId={}, error={}", docId, e.getMessage());
                    }
                }
                updateProgressFinal(message.getTaskId(), "COMPLETED", message.getDocumentIds().size(), deleted, 0);
                log.info("Vector index deletion completed: taskId={}, deleted={}", message.getTaskId(), deleted);
                channel.basicAck(deliveryTag, false);
                return;
            }

            List<DocumentRecordDTO> documents;
            if (message.getType() == ReindexMessage.ReindexType.BY_DOC_IDS && message.getDocumentIds() != null) {
                documents = loadDocumentsByIds(message.getDocumentIds());
            } else {
                documents = loadAllPublishedDocuments();
            }

            int total = documents.size();
            updateProgress(message.getTaskId(), "RUNNING", total, 0, 0);
            log.info("Starting document indexing: taskId={}, totalDocuments={}", message.getTaskId(), total);

            int completed = 0;
            int failed = 0;
            List<Long> failedDocIds = new ArrayList<>();

            for (int i = 0; i < documents.size(); i += ragProperties.getAsync().getReindexBatchSize()) {
                int end = Math.min(i + ragProperties.getAsync().getReindexBatchSize(), documents.size());
                List<DocumentRecordDTO> batch = documents.subList(i, end);

                for (DocumentRecordDTO doc : batch) {
                    try {
                        Long docId = doc.getId();
                        String title = doc.getTitle() != null ? doc.getTitle() : "Untitled Document";
                        String content = doc.getContent();
                        Long categoryId = doc.getCategoryId();
                        Long authorId = doc.getAuthorId();
                        Long teamId = doc.getTeamId();
                        Integer status = doc.getStatus();
                        Integer isPublic = doc.getIsPublic();

                        if (content == null || content.isEmpty()) {
                            log.warn("Document content is empty, skipping: documentId={}", docId);
                            completed++;
                            continue;
                        }

                        // Delete old chunks
                        vectorIndexService.deleteByDocId(docId);

                        // Chunk + embed + index
                        List<DocumentChunk> chunks = chunkingService.chunk(
                                content, docId, title, categoryId, authorId, teamId, status,
                                isPublic, doc.getPublishTime());

                        if (!chunks.isEmpty()) {
                            List<String> texts = chunks.stream().map(DocumentChunk::getContent).toList();
                            List<float[]> embeddings = embeddingService.embedBatch(texts);
                            for (int j = 0; j < chunks.size(); j++) {
                                chunks.get(j).setEmbedding(embeddings.get(j));
                                chunks.get(j).setIndexedAt(LocalDateTime.now());
                            }
                            vectorIndexService.indexChunks(chunks);
                        }

                        completed++;
                    } catch (Exception e) {
                        log.error("Document indexing failed: {}", e.getMessage(), e);
                        failed++;
                        failedDocIds.add(doc.getId());
                    }
                }

                updateProgress(message.getTaskId(), "RUNNING", total, completed, failed);
                log.debug("Reindex progress: taskId={}, completed={}/{}, failed={}",
                        message.getTaskId(), completed, total, failed);
            }

            // Completed
            updateProgressFinal(message.getTaskId(), "COMPLETED", total, completed, failed);
            log.info("Reindex completed: taskId={}, completed={}, failed={}", message.getTaskId(), completed, failed);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Reindex task failed: taskId={}, error={}", message.getTaskId(), e.getMessage(), e);
            try {
                updateProgress(message.getTaskId(), "FAILED", 0, 0, 0);
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("Message acknowledgment failed", ex);
            }
        }
    }

    private List<DocumentRecordDTO> loadAllPublishedDocuments() {
        List<DocumentRecordDTO> allDocs = new ArrayList<>();
        long current = 1;
        while (true) {
            Map<String, Object> page = documentFeignClient.pageDocuments(current, (long) PAGE_SIZE, 1);
            if (page == null) break;

            Object dataObj = page.get("data");
            if (!(dataObj instanceof Map)) break;

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object recordsObj = data.get("records");
            if (!(recordsObj instanceof List)) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) recordsObj;
            if (records.isEmpty()) break;

            // Each record's content must be fetched individually (the pagination API doesn't return content)
            for (Map<String, Object> record : records) {
                Long docId = toLong(record.get("id"));
                if (docId != null) {
                    try {
                        DocumentRecordDTO detail = loadDocumentDetail(docId);
                        if (detail != null) {
                            allDocs.add(detail);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get document details: documentId={}, error={}", docId, e.getMessage());
                    }
                }
            }

            if (records.size() < PAGE_SIZE) break;
            current++;
        }
        return allDocs;
    }

    private List<DocumentRecordDTO> loadDocumentsByIds(List<Long> docIds) {
        List<DocumentRecordDTO> docs = new ArrayList<>();
        for (Long docId : docIds) {
            try {
                DocumentRecordDTO detail = loadDocumentDetail(docId);
                if (detail != null) {
                    docs.add(detail);
                }
            } catch (Exception e) {
                log.warn("Failed to get document details: documentId={}, error={}", docId, e.getMessage());
            }
        }
        return docs;
    }

    @SuppressWarnings("unchecked")
    private DocumentRecordDTO loadDocumentDetail(Long docId) {
        Map<String, Object> response = documentFeignClient.getDocument(docId);
        if (response == null) return null;
        Object dataObj = response.get("data");
        if (!(dataObj instanceof Map)) return null;
        return toDocumentRecord((Map<String, Object>) dataObj);
    }

    /**
     * Convert the Map returned by Feign into a DocumentRecordDTO
     */
    private DocumentRecordDTO toDocumentRecord(Map<String, Object> raw) {
        if (raw == null) return null;
        return DocumentRecordDTO.builder()
                .id(toLong(raw.get("id")))
                .title((String) raw.getOrDefault("title", "Untitled Document"))
                .content((String) raw.get("content"))
                .categoryId(toLong(raw.get("categoryId")))
                .authorId(toLong(raw.get("authorId")))
                .authorName((String) raw.getOrDefault("authorName", ""))
                .teamId(toLong(raw.get("teamId")))
                .status(toInt(raw.get("status")))
                .isPublic(toInt(raw.get("isPublic")))
                .summary((String) raw.get("summary"))
                .publishTime(getString(raw, "publishTime"))
                .build();
    }

    private void updateProgress(String taskId, String status, int total, int completed, int failed) {
        ReindexProgressVO progress = ReindexProgressVO.builder()
                .taskId(taskId)
                .status(status)
                .totalDocuments(total)
                .completedDocuments(completed)
                .failedDocuments(failed)
                .startTime(LocalDateTime.now())
                .build();
        try {
            redisTemplate.opsForValue().set(PROGRESS_KEY_PREFIX + taskId,
                    com.alibaba.fastjson2.JSON.toJSONString(progress), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update indexing progress: {}", e.getMessage());
        }
    }

    private void updateProgressFinal(String taskId, String status, int total, int completed, int failed) {
        ReindexProgressVO progress = ReindexProgressVO.builder()
                .taskId(taskId)
                .status(status)
                .totalDocuments(total)
                .completedDocuments(completed)
                .failedDocuments(failed)
                .endTime(LocalDateTime.now())
                .build();
        try {
            redisTemplate.opsForValue().set(PROGRESS_KEY_PREFIX + taskId,
                    com.alibaba.fastjson2.JSON.toJSONString(progress), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update indexing progress: {}", e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
