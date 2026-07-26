package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.enums.DocumentStatus;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.result.ResultCode;
import com.knowledge.base.document.service.LikeService;

import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.AutoSaveDTO;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.dto.FileUploadResponse;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.Category;
import com.knowledge.base.document.entity.mongodb.DocumentContent;
import com.knowledge.base.document.event.StatisticsEventPublisher;
import com.knowledge.base.document.feign.FileServiceFeignClient;
import com.knowledge.base.document.feign.GraphFeignClient;
import com.knowledge.base.document.feign.KAGFeignClient;
import com.knowledge.base.document.feign.RagFeignClient;
import com.knowledge.base.document.feign.SearchIndexFeignClient;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.CategoryMapper;
import com.knowledge.base.document.service.DocumentAccessService;
import com.knowledge.base.document.service.DocumentContentService;
import com.knowledge.base.document.service.DocumentReviewService;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.service.FileParserService;
import com.knowledge.base.document.service.AutoSaveHistoryService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.client.UserServiceClient;
import com.knowledge.base.document.vo.AuthorVO;
import com.knowledge.base.document.vo.DocumentNeighborVO;
import com.knowledge.base.document.vo.DocumentVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.knowledge.base.common.config.SystemConfigCache;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Document Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements document related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private DocumentContentService documentContentService;

    @Resource
    private FileServiceFeignClient fileServiceFeignClient;

    @Resource
    private RagFeignClient ragFeignClient;

    @Resource
    private KAGFeignClient kagFeignClient;

    @Resource
    private SearchIndexFeignClient searchIndexFeignClient;

    @Resource
    private GraphFeignClient graphFeignClient;

    @Resource
    private DocumentAccessService documentAccessService;

    @Resource
    private LikeService likeService;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SystemConfigCache systemConfigCache;

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private StatisticsEventPublisher statisticsEventPublisher;

    @Resource
    private DocumentReviewService documentReviewService;

    @Resource
    private FileParserService fileParserService;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    @Resource
    private AutoSaveHistoryService autoSaveHistoryService;

    /** Avatar cache, avoiding repeated HTTP calls for the same user */
    private final Map<Long, String> avatarCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(DocumentDTO documentDTO) {
        log.info("Create document: title={}", documentDTO.getTitle());

        // Build the document entity
        Document document = new Document();
        // Exclude the content field when copying properties; content is stored in MongoDB
        BeanUtil.copyProperties(documentDTO, document, "content");

        // Generate the ID
        document.setId(SnowflakeIdGenerator.getInstance().nextId());

        // Set default values
        if (document.getDocumentType() == null) {
            document.setDocumentType(1);
        }
        if (document.getStatus() == null) {
            document.setStatus(0);
        }
        if (document.getIsTop() == null) {
            document.setIsTop(0);
        }
        if (document.getIsRecommend() == null) {
            document.setIsRecommend(0);
        }
        if (document.getSource() == null) {
            document.setSource(1);
        }
        if (document.getAllowComment() == null) {
            document.setAllowComment(1);
        }
        if (document.getSort() == null) {
            document.setSort(0);
        }
        if (document.getViewCount() == null) {
            document.setViewCount(0L);
        }
        if (document.getLikeCount() == null) {
            document.setLikeCount(0L);
        }
        if (document.getFavoriteCount() == null) {
            document.setFavoriteCount(0L);
        }
        if (document.getCommentCount() == null) {
            document.setCommentCount(0L);
        }

        // Imported documents (file type) get a default category and visibility
        if (Objects.equals(document.getDocumentType(), 2)) {
            if (document.getCategoryId() == null) {
                Category techCategory = categoryMapper.selectOne(
                        new LambdaQueryWrapper<Category>()
                                .eq(Category::getCategoryName, "Technical Documentation")
                );
                if (techCategory != null) {
                    document.setCategoryId(techCategory.getId());
                    log.info("Set default category for imported document: categoryId={}, categoryName=Technical Documentation", techCategory.getId());
                }
            }
            if (document.getIsPublic() == null) {
                document.setIsPublic(0); // team visible
            }
        }

        // Get the currently logged-in user from the context
        document.setAuthorId(UserContext.getCurrentUserId());
        document.setAuthorName(UserContext.getCurrentUserName());

        // If publishing, set the publish time
        if (Objects.equals(document.getStatus(), 1)) {
            document.setPublishTime(LocalDateTime.now());
        }

        // First save the MySQL document record (without the content field)
        int count = documentMapper.insert(document);
        if (count <= 0) {
            throw new BusinessException("Failed to create document");
        }

        // Increment the category's document count
        if (document.getCategoryId() != null) {
            categoryMapper.incrementDocumentCount(document.getCategoryId());
        }

        // Synchronously save the content to MongoDB
        boolean hasContent = StrUtil.isNotBlank(documentDTO.getContent());
        if (hasContent) {
            try {
                String contentId = documentContentService.saveContent(document.getId(), documentDTO.getContent());
                // Compute the content length (character count) and file size (UTF-8 byte count)
                String content = documentDTO.getContent();
                int contentLength = content.length();
                long fileSize = content.getBytes(StandardCharsets.UTF_8).length;
                // Update the MySQL record's contentId, contentLength, fileSize
                Document doc = new Document();
                doc.setId(document.getId());
                doc.setContentId(contentId);
                doc.setContentLength(contentLength);
                doc.setFileSize(fileSize);
                documentMapper.updateById(doc);
                log.info("Document content saved successfully: documentId={}, contentId={}, contentLength={}, fileSize={}", document.getId(), contentId, contentLength, fileSize);
            } catch (Exception e) {
                log.error("Failed to save document content to MongoDB: documentId={}, error={}", document.getId(), e.getMessage());
                throw new BusinessException("Failed to save document content: " + e.getMessage());
            }
        }

        // Asynchronously trigger RAG indexing (only for published documents that have content)
        if (Objects.equals(document.getStatus(), 1) && hasContent) {
            triggerRagReindex(document.getId());
            triggerKAGBuild(document.getId());
        }

        // Asynchronously sync to the ES search index (published documents)
        if (Objects.equals(document.getStatus(), 1)) {
            triggerSearchIndex(document, documentDTO.getContent());
        }

        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoSaveDocument(AutoSaveDTO autoSaveDTO) {
        // An empty title is auto-filled with "Untitled Document"
        String title = StrUtil.isBlank(autoSaveDTO.getTitle())
                ? "Untitled Document" : autoSaveDTO.getTitle().trim();

        // Capture the user information and content ahead of time, for the async snapshot save (ThreadLocal does not propagate to the async thread)
        final Long currentUserId = UserContext.getCurrentUserId();
        final String currentContent = autoSaveDTO.getContent();

        if (autoSaveDTO.getId() != null) {
            // -- Update an existing document --
            log.info("Auto-save - update: documentId={}", autoSaveDTO.getId());
            Document existDocument = documentMapper.selectById(autoSaveDTO.getId());
            if (existDocument == null) {
                throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
            }

            // Update the MongoDB content (if content was provided)
            if (StrUtil.isNotBlank(currentContent)) {
                try {
                    if (StrUtil.isNotBlank(existDocument.getContentId())) {
                        documentContentService.updateContent(autoSaveDTO.getId(), currentContent);
                    } else {
                        String contentId = documentContentService.saveContent(autoSaveDTO.getId(), currentContent);
                        Document upd = new Document();
                        upd.setId(autoSaveDTO.getId());
                        upd.setContentId(contentId);
                        documentMapper.updateById(upd);
                    }
                    // Sync the content length and size
                    Document sizeUpd = new Document();
                    sizeUpd.setId(autoSaveDTO.getId());
                    sizeUpd.setContentLength(currentContent.length());
                    sizeUpd.setFileSize((long) currentContent.getBytes(StandardCharsets.UTF_8).length);
                    documentMapper.updateById(sizeUpd);
                } catch (Exception e) {
                    log.warn("Auto-save - failed to update MongoDB content: documentId={}, error={}",
                            autoSaveDTO.getId(), e.getMessage());
                }
            }

            // Only update fields that have a value in MySQL, avoiding overwriting existing data with null
            Document updateDoc = new Document();
            updateDoc.setId(autoSaveDTO.getId());
            updateDoc.setTitle(title);
            updateDoc.setStatus(0); // force draft status
            if (autoSaveDTO.getSummary() != null) {
                updateDoc.setSummary(autoSaveDTO.getSummary());
            }
            if (autoSaveDTO.getCategoryId() != null) {
                updateDoc.setCategoryId(autoSaveDTO.getCategoryId());
            }
            if (autoSaveDTO.getTeamId() != null) {
                updateDoc.setTeamId(autoSaveDTO.getTeamId());
            }
            if (autoSaveDTO.getTags() != null) {
                updateDoc.setTags(autoSaveDTO.getTags());
            }

            documentMapper.updateById(updateDoc);

            // Asynchronously save an auto-save history snapshot
            final Long updatedDocId = autoSaveDTO.getId();
            CompletableFuture.runAsync(() -> {
                autoSaveHistoryService.saveSnapshot(updatedDocId, title, currentContent, currentUserId);
            }, asyncTaskExecutor);

            return autoSaveDTO.getId();
        } else {
            // -- Create a new draft --
            // Deduplication: check whether the current user already has a recently auto-saved draft (within 5 minutes)
            Document recentDraft = documentMapper.selectOne(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getAuthorId, currentUserId)
                            .eq(Document::getStatus, 0)
                            .ge(Document::getCreatedAt, LocalDateTime.now().minusMinutes(5))
                            .orderByDesc(Document::getCreatedAt)
                            .last("LIMIT 1")
            );

            if (recentDraft != null) {
                // Reuse the existing draft instead of creating a new one
                log.info("Auto-save - dedup reuse draft: documentId={}", recentDraft.getId());
                Document updateDoc = new Document();
                updateDoc.setId(recentDraft.getId());
                updateDoc.setTitle(title);
                updateDoc.setStatus(0);
                updateDoc.setAutoSaveDismissed(0); // mark as a draft that has had auto-save interaction
                if (autoSaveDTO.getSummary() != null) {
                    updateDoc.setSummary(autoSaveDTO.getSummary());
                }
                if (autoSaveDTO.getCategoryId() != null) {
                    updateDoc.setCategoryId(autoSaveDTO.getCategoryId());
                }
                if (autoSaveDTO.getTeamId() != null) {
                    updateDoc.setTeamId(autoSaveDTO.getTeamId());
                }
                if (autoSaveDTO.getTags() != null) {
                    updateDoc.setTags(autoSaveDTO.getTags());
                }
                documentMapper.updateById(updateDoc);

                // Update the MongoDB content
                if (StrUtil.isNotBlank(currentContent)) {
                    try {
                        if (StrUtil.isNotBlank(recentDraft.getContentId())) {
                            documentContentService.updateContent(recentDraft.getId(), currentContent);
                        } else {
                            String contentId = documentContentService.saveContent(recentDraft.getId(), currentContent);
                            Document upd = new Document();
                            upd.setId(recentDraft.getId());
                            upd.setContentId(contentId);
                            documentMapper.updateById(upd);
                        }
                        // Sync the content length and size (MySQL), which must be updated regardless of whether contentId already existed
                        Document sizeUpd = new Document();
                        sizeUpd.setId(recentDraft.getId());
                        sizeUpd.setContentLength(currentContent.length());
                        sizeUpd.setFileSize((long) currentContent.getBytes(StandardCharsets.UTF_8).length);
                        documentMapper.updateById(sizeUpd);
                    } catch (Exception e) {
                        log.warn("Auto-save - failed to update MongoDB after dedup: documentId={}, error={}",
                                recentDraft.getId(), e.getMessage());
                    }
                }

                // Asynchronously save an auto-save history snapshot
                final Long reusedId = recentDraft.getId();
                CompletableFuture.runAsync(() -> {
                    autoSaveHistoryService.saveSnapshot(reusedId, title, currentContent, currentUserId);
                }, asyncTaskExecutor);

                return recentDraft.getId();
            }

            log.info("Auto-save - create new draft: title={}", title);

            Document document = new Document();
            document.setId(SnowflakeIdGenerator.getInstance().nextId());
            document.setTitle(title);
            document.setContent(currentContent);
            document.setSummary(autoSaveDTO.getSummary());
            document.setCategoryId(autoSaveDTO.getCategoryId());
            document.setTeamId(autoSaveDTO.getTeamId());
            document.setTags(autoSaveDTO.getTags());
            document.setStatus(0); // draft
            document.setDocumentType(1);
            document.setIsTop(0);
            document.setIsRecommend(0);
            document.setSource(1);
            document.setAllowComment(1);
            document.setSort(0);
            document.setViewCount(0L);
            document.setLikeCount(0L);
            document.setFavoriteCount(0L);
            document.setCommentCount(0L);
            document.setAuthorId(currentUserId);
            document.setAuthorName(UserContext.getCurrentUserName());
            document.setAutoSaveDismissed(0); // mark as a draft created by auto-save

            int count = documentMapper.insert(document);
            if (count <= 0) {
                throw new BusinessException("Failed to create draft");
            }

            // Increment the category document count
            if (document.getCategoryId() != null) {
                categoryMapper.incrementDocumentCount(document.getCategoryId());
            }

            // Save the content to MongoDB (if any)
            if (StrUtil.isNotBlank(currentContent)) {
                try {
                    String contentId = documentContentService.saveContent(document.getId(), currentContent);
                    Document upd = new Document();
                    upd.setId(document.getId());
                    upd.setContentId(contentId);
                    upd.setContentLength(currentContent.length());
                    upd.setFileSize((long) currentContent.getBytes(StandardCharsets.UTF_8).length);
                    documentMapper.updateById(upd);
                } catch (Exception e) {
                    log.error("Auto-save - MongoDB save failed: documentId={}, error={}", document.getId(), e.getMessage());
                }
            }

            // Asynchronously save an auto-save history snapshot
            final Long newDocId = document.getId();
            CompletableFuture.runAsync(() -> {
                autoSaveHistoryService.saveSnapshot(newDocId, title, currentContent, currentUserId);
            }, asyncTaskExecutor);

            // Does not trigger RAG/KAG/ES indexing -- drafts do not need indexing
            return document.getId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDocument(DocumentDTO documentDTO) {
        log.info("Update document: documentId={}", documentDTO.getId());

        if (documentDTO.getId() == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document existDocument = documentMapper.selectById(documentDTO.getId());
        if (existDocument == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // If there is content to update, update the content in MongoDB (skip the content update if MongoDB is unavailable)
        String contentId = existDocument.getContentId();
        if (StrUtil.isNotBlank(documentDTO.getContent())) {
            try {
                if (StrUtil.isNotBlank(existDocument.getContentId())) {
                    // Update the existing content
                    documentContentService.updateContent(documentDTO.getId(), documentDTO.getContent());
                } else {
                    // Create new content
                    contentId = documentContentService.saveContent(documentDTO.getId(), documentDTO.getContent());
                }
            } catch (Exception e) {
                // If the MongoDB connection fails, log a warning but continue
                log.warn("Failed to update document content in MongoDB; document metadata was already updated in MySQL: documentId={}, error={}",
                        documentDTO.getId(), e.getMessage());
            }
        }

        // Build the update entity (without the content field)
        Document document = new Document();
        // Exclude the content field when copying properties; content is stored in MongoDB
        BeanUtil.copyProperties(documentDTO, document, "content");
        // Set contentId
        document.setContentId(contentId);

        // If the content changed, sync the content length and file size
        if (StrUtil.isNotBlank(documentDTO.getContent())) {
            String updatedContent = documentDTO.getContent();
            document.setContentLength(updatedContent.length());
            document.setFileSize((long) updatedContent.getBytes(StandardCharsets.UTF_8).length);
        }

        // If the status changed from draft to published, set the publish time
        if (Objects.equals(existDocument.getStatus(), 0)
            && Objects.equals(documentDTO.getStatus(), 1)) {
            document.setPublishTime(LocalDateTime.now());
        }

        // If the status changed to pending review (PENDING_REVIEW=3), trigger the review flow
        Integer newStatus = documentDTO.getStatus();
        boolean toPendingReview = !Objects.equals(existDocument.getStatus(), newStatus)
                && Objects.equals(newStatus, 3);

        int count = documentMapper.updateById(document);

        // If the category changed, update the corresponding categories' document counts
        Long oldCategoryId = existDocument.getCategoryId();
        Long newCategoryId = document.getCategoryId();
        if (count > 0 && !Objects.equals(oldCategoryId, newCategoryId)) {
            if (oldCategoryId != null) {
                categoryMapper.decrementDocumentCount(oldCategoryId);
            }
            if (newCategoryId != null) {
                categoryMapper.incrementDocumentCount(newCategoryId);
            }
        }

        // When the status changes to pending review, trigger the review flow (push a WebSocket notification)
        if (count > 0 && toPendingReview) {
            try {
                documentReviewService.submitForReview(documentDTO.getId());
                log.info("Automatically submitted for review after document edit: documentId={}", documentDTO.getId());
            } catch (Exception e) {
                log.error("Failed to submit for review: documentId={}, error={}", documentDTO.getId(), e.getMessage(), e);
            }
        }

        // Asynchronously trigger a RAG index update
        Long docId = documentDTO.getId();
        boolean contentChanged = StrUtil.isNotBlank(documentDTO.getContent());
        Integer oldStatus = existDocument.getStatus();
        boolean statusToPublished = Objects.equals(oldStatus, 0) && Objects.equals(newStatus, 1);
        boolean statusFromPublished = Objects.equals(oldStatus, 1) && !Objects.equals(newStatus, 1);

        if ((contentChanged || statusToPublished) && Objects.equals(newStatus, 1)) {
            // Content changed or published from draft -> rebuild the RAG index
            triggerRagReindex(docId);
            triggerKAGBuild(docId);
            // Sync the ES search index
            Document syncDoc = new Document();
            BeanUtil.copyProperties(documentDTO, syncDoc, "content");
            syncDoc.setContent(documentDTO.getContent());
            triggerSearchIndex(syncDoc, documentDTO.getContent());
        } else if (statusFromPublished) {
            // Changed from published to unpublished (unpublished/archived) -> remove from RAG
            triggerRagDelete(docId);
            triggerKAGDelete(docId);
            triggerGraphDelete(docId);
            // Remove from the ES search index
            triggerSearchDelete(docId);
        } else if (Objects.equals(newStatus, 1)) {
            // A published document has other updates -> sync the ES search index
            Document syncDoc = new Document();
            BeanUtil.copyProperties(documentDTO, syncDoc, "content");
            triggerSearchIndex(syncDoc, documentDTO.getContent());
        }

        return count > 0;
    }

    @Override
    public Boolean updateSummary(Long documentId, String summary) {
        log.info("Update document summary: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        Document existDocument = documentMapper.selectById(documentId);
        if (existDocument == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        Document document = new Document();
        document.setId(documentId);
        document.setSummary(summary);
        int count = documentMapper.updateById(document);

        log.info("Document summary updated successfully: documentId={}", documentId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDocument(Long documentId) {
        log.info("Delete document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Get the category ID before deletion, used to update the category's document count
        Document existDocument = documentMapper.selectById(documentId);
        Long categoryId = existDocument != null ? existDocument.getCategoryId() : null;

        int count = documentMapper.deleteById(documentId);

        // Asynchronously remove this document's vector index from RAG
        if (count > 0) {
            // Decrement the corresponding category's document count
            if (categoryId != null) {
                categoryMapper.decrementDocumentCount(categoryId);
            }
            triggerRagDelete(documentId);
            triggerKAGDelete(documentId);
            triggerSearchDelete(documentId);
            triggerGraphDelete(documentId);
        }

        return count > 0;
    }

    /**
     * Builds the author information VO
     */
    private AuthorVO buildAuthorVO(Long authorId, String authorName) {
        if (authorId == null) {
            return null;
        }
        AuthorVO authorVO = new AuthorVO();
        authorVO.setId(authorId);
        authorVO.setUsername(authorName);
        // Get the avatar from the user service, using the cache to avoid repeated calls
        String avatar = avatarCache.computeIfAbsent(authorId, id -> {
            String fetched = userServiceClient.getUserAvatar(id);
            return fetched != null ? fetched : "";
        });
        authorVO.setAvatar(avatar);
        authorVO.setEmail("");
        authorVO.setPosition("Employee");
        return authorVO;
    }

    @Override
    public DocumentVO getDocumentById(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        DocumentVO documentVO = BeanUtil.copyProperties(document, DocumentVO.class);

        // Build the author information
        documentVO.setAuthor(buildAuthorVO(document.getAuthorId(), document.getAuthorName()));

        // Get the document content from MongoDB
        if (StrUtil.isNotBlank(document.getContentId())) {
            try {
                DocumentContent documentContent = documentContentService.getContentById(document.getContentId());
                if (documentContent != null) {
                    documentVO.setContent(documentContent.getContent());
                }
            } catch (Exception e) {
                log.error("Failed to get document content: documentId={}, contentId={}", documentId, document.getContentId(), e);
                documentVO.setContent(null);
            }
        }

        // Query whether the current user has liked it
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId != null) {
                documentVO.setIsLiked(likeService.isLiked(documentId, userId, 1));
            }
        } catch (Exception e) {
            log.debug("Failed to get like status (possibly an anonymous access): documentId={}", documentId);
        }

        return documentVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO viewDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // Increment the view count
        documentMapper.incrementViewCount(documentId);

        DocumentVO documentVO = BeanUtil.copyProperties(document, DocumentVO.class);

        // Build the author information
        documentVO.setAuthor(buildAuthorVO(document.getAuthorId(), document.getAuthorName()));

        // Get the document content from MongoDB
        if (StrUtil.isNotBlank(document.getContentId())) {
            try {
                DocumentContent documentContent = documentContentService.getContentById(document.getContentId());
                if (documentContent != null) {
                    documentVO.setContent(documentContent.getContent());
                }
            } catch (Exception e) {
                log.error("Failed to get document content: documentId={}, contentId={}", documentId, document.getContentId(), e);
                documentVO.setContent(null);
            }
        }

        // Asynchronously record the access, without affecting the main flow's response time
        // Note: the user ID must be obtained on the main thread, since ThreadLocal cannot propagate across threads
        // Skip access recording when the user is not logged in during a public share access
        final Long currentUserId;
        try {
            currentUserId = UserContext.getCurrentUserId();
            // Query whether the current user has liked it
            documentVO.setIsLiked(likeService.isLiked(documentId, currentUserId, 1));
        } catch (IllegalStateException e) {
            log.debug("Anonymous user access, skipping recording: documentId={}", documentId);
            return documentVO;
        }
        final String documentTitle = document.getTitle();
        final String currentUserName = UserContext.getCurrentUserName();
        CompletableFuture.runAsync(() -> {
            try {
                documentAccessService.recordAccess(currentUserId, documentId, documentTitle);
                log.debug("Asynchronously recorded access successfully: userId={}, documentId={}, documentTitle={}", currentUserId, documentId, documentTitle);
            } catch (Exception e) {
                log.error("Failed to asynchronously record access: userId={}, documentId={}, documentTitle={}", currentUserId, documentId, documentTitle, e);
            }
        }, asyncTaskExecutor);

        // Publish a view statistics event to RabbitMQ
        statisticsEventPublisher.publishViewEvent(currentUserId, currentUserName, documentId, documentTitle);

        return documentVO;
    }

    @Override
    public IPage<DocumentVO> pageDocuments(Long current, Long size, Long categoryId, Long teamId, String keyword, Integer status, String sortBy, String sortOrder, Long authorId) {
        // Build the query conditions
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            List<Long> categoryIds = collectCategoryIds(categoryId);
            wrapper.in(Document::getCategoryId, categoryIds);
        }

        if (teamId != null) {
            wrapper.eq(Document::getTeamId, teamId);
        }

        if (status != null) {
            wrapper.eq(Document::getStatus, status);
        }

        if (authorId != null) {
            wrapper.eq(Document::getAuthorId, authorId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Document::getTitle, keyword)
                .or()
                .like(Document::getSummary, keyword)
                // Note: the content field is stored in MongoDB; only fields in MySQL are searched here
                // If full-text search is needed, it must be queried from MongoDB
                .or()
                .like(Document::getTags, keyword));
        }

        // Dynamic sorting logic
        if (StringUtils.hasText(sortBy) && StringUtils.hasText(sortOrder)) {
            // Sort according to the sorting parameters passed by the frontend
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);

            switch (sortBy) {
                case "updatedAt":
                    if (isAsc) {
                        wrapper.orderByAsc(Document::getUpdatedAt);
                    } else {
                        wrapper.orderByDesc(Document::getUpdatedAt);
                    }
                    // Secondary sort: creation time descending
                    wrapper.orderByDesc(Document::getCreatedAt);
                    break;
                case "createdAt":
                    if (isAsc) {
                        wrapper.orderByAsc(Document::getCreatedAt);
                    } else {
                        wrapper.orderByDesc(Document::getCreatedAt);
                    }
                    // Secondary sort: update time descending
                    wrapper.orderByDesc(Document::getUpdatedAt);
                    break;
                case "publishTime":
                    if (isAsc) {
                        wrapper.orderByAsc(Document::getPublishTime);
                    } else {
                        wrapper.orderByDesc(Document::getPublishTime);
                    }
                    break;
                case "title":
                    if (isAsc) {
                        wrapper.orderByAsc(Document::getTitle);
                    } else {
                        wrapper.orderByDesc(Document::getTitle);
                    }
                    break;
                case "viewCount":
                    if (isAsc) {
                        wrapper.orderByAsc(Document::getViewCount);
                    } else {
                        wrapper.orderByDesc(Document::getViewCount);
                    }
                    break;
                default:
                    // Default sort: pinned, sort order, then publish time descending
                    wrapper.orderByDesc(Document::getIsTop)
                        .orderByDesc(Document::getSort)
                        .orderByDesc(Document::getPublishTime);
                    break;
            }
        } else {
            // Default sort: pinned, sort order, then publish time descending
            wrapper.orderByDesc(Document::getIsTop)
                .orderByDesc(Document::getSort)
                .orderByDesc(Document::getPublishTime);
        }

        // Paginated query
        Page<Document> page = new Page<>(current, size);
        IPage<Document> documentPage = documentMapper.selectPage(page, wrapper);

        // Convert to VO
        return documentPage.convert(document -> {
            DocumentVO documentVO = BeanUtil.copyProperties(document, DocumentVO.class);
            // Build the author information
            documentVO.setAuthor(buildAuthorVO(document.getAuthorId(), document.getAuthorName()));
            // Query the category name
            if (document.getCategoryId() != null) {
                Category category = categoryMapper.selectById(document.getCategoryId());
                if (category != null) {
                    documentVO.setCategoryName(category.getCategoryName());
                }
            }
            // Query the team space name
            if (document.getTeamId() != null) {
                try {
                    String teamName = jdbcTemplate.queryForObject(
                        "SELECT team_name FROM kb_team WHERE id = ? AND deleted = 0",
                        String.class, document.getTeamId());
                    documentVO.setTeamName(teamName);
                } catch (Exception e) {
                    log.warn("Failed to query team name for teamId={}", document.getTeamId());
                }
            }
            return documentVO;
        });
    }

    @Override
    public DocumentNeighborVO getDocumentNeighbors(Long documentId) {
        Document currentDoc = getById(documentId);
        if (currentDoc == null) {
            return new DocumentNeighborVO();
        }

        Integer isTop = currentDoc.getIsTop() != null ? currentDoc.getIsTop() : 0;
        Integer sort = currentDoc.getSort() != null ? currentDoc.getSort() : 0;
        LocalDateTime publishTime = currentDoc.getPublishTime() != null ? currentDoc.getPublishTime() : LocalDateTime.now();

        DocumentNeighborVO prevDoc = documentMapper.selectPrev(isTop, sort, publishTime);
        DocumentNeighborVO nextDoc = documentMapper.selectNext(isTop, sort, publishTime);

        DocumentNeighborVO result = new DocumentNeighborVO();
        if (prevDoc != null) {
            result.setPrevId(prevDoc.getPrevId());
            result.setPrevTitle(prevDoc.getPrevTitle());
        }
        if (nextDoc != null) {
            result.setNextId(nextDoc.getNextId());
            result.setNextTitle(nextDoc.getNextTitle());
        }
        return result;
    }

    @Override
    public String uploadDocumentFile(MultipartFile file) {
        log.info("Upload document file to the file service: fileName={}", file.getOriginalFilename());

        // Check whether the file is empty
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        // Check the file size (read from system configuration)
        long maxSize = getMaxFileSizeFromConfig();
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // Get the original file name and extension
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);

        // Check the file type (read from system configuration)
        if (!StrUtil.isNotBlank(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
        List<String> allowedTypes = getAllowedFileTypesFromConfig();
        if (!allowedTypes.contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }

        try {
            // Call the file service to upload the file
            log.info("Calling the file service to upload the file: fileName={}", originalFilename);

            Result<FileUploadResponse> result = fileServiceFeignClient.uploadFile(
                    file, "document", 1, null);

            log.info("File service returned result: result={}", result);

            if (result == null) {
                log.error("File service returned a null result");
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }

            log.info("File service return code: code={}, message={}", result.getCode(), result.getMessage());

            if (result.getCode() != 200) {
                log.error("File service returned an error code: code={}, message={}", result.getCode(), result.getMessage());
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }

            FileUploadResponse response = result.getData();

            if (response == null) {
                log.error("File service returned null data");
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }

            log.info("File service returned data: response={}", response);

            // Get the file URL, preferring the fileUrl field (consistent with FileInfoVO)
            String fileUrl = response.getFileUrl();

            // If fileUrl is blank, try other possible fields
            if (StrUtil.isBlank(fileUrl)) {
                fileUrl = response.getPreviewUrl();
            }
            if (StrUtil.isBlank(fileUrl)) {
                fileUrl = response.getConvertedUrl();
            }

            // If all URL fields are blank but there is a file ID, construct the URL
            if (StrUtil.isBlank(fileUrl) && response.getId() != null) {
                fileUrl = constructFileUrl(response.getId());
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.error("The URL returned by the file service is blank: response={}", response);
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }

            log.info("File uploaded successfully: fileName={}, url={}", originalFilename, fileUrl);

            // Return the file access URL
            return fileUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("File upload failed: fileName={}", originalFilename, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Constructs the file access URL
     *
     * @param fileId file ID
     * @return file access URL
     */
    private String constructFileUrl(Long fileId) {
        // The URL can be constructed here as needed; currently returns the file service's download endpoint
        return String.format("http://localhost:8084/files/download/%d", fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadAndCreateDocument(MultipartFile file) {
        // 1. Parameter validation
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);

        if (!fileParserService.isSupported(extension)) {
            throw new BusinessException("Unsupported file format: " + extension + ", supported formats: pdf, docx, xlsx, pptx, txt, md");
        }

        log.info("Upload and parse file: name={}, size={}, ext={}", originalFilename, file.getSize(), extension);

        // 2. Parse the file content (must happen before upload: the upload consumes the InputStream via Feign)
        String parsedContent;
        try {
            parsedContent = fileParserService.parse(file);
        } catch (Exception e) {
            log.error("File parsing failed: name={}, error={}", originalFilename, e.getMessage(), e);
            throw new BusinessException("File parsing failed: " + e.getMessage());
        }

        // 3. Upload the file to the kb-file service
        String fileUrl = uploadDocumentFile(file);

        if (StrUtil.isBlank(parsedContent)) {
            throw new BusinessException("The parsed file result is empty; please confirm the file contains extractable text content");
        }

        // 4. Build the document title (strip the extension)
        String title = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;

        // 5. Build the DocumentDTO and create the document
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setTitle(title);
        documentDTO.setContent(parsedContent);
        documentDTO.setDocumentType(2);           // file type
        documentDTO.setFileSize(file.getSize());
        documentDTO.setFileExtension(extension);
        documentDTO.setFilePath(fileUrl);
        documentDTO.setStatus(0);                 // initially draft status

        Long documentId = createDocument(documentDTO);

        // 6. Build the response result
        int previewLen = Math.min(200, parsedContent.length());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId);
        result.put("title", title);
        result.put("fileUrl", fileUrl);
        result.put("fileSize", file.getSize());
        result.put("contentLength", parsedContent.length());
        result.put("contentPreview", parsedContent.substring(0, previewLen));

        log.info("File parsed and document created successfully: documentId={}, title={}, ext={}, chars={}",
                documentId, title, extension, parsedContent.length());

        return result;
    }

    // ==================== RAG automatic indexing ====================

    /**
     * Asynchronously triggers a RAG index rebuild (does not block the main flow)
     */
    private void triggerRagReindex(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                ragFeignClient.reindexDocument(docId);
                log.info("Triggered RAG index rebuild: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger RAG index rebuild (RAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously triggers RAG index deletion (does not block the main flow)
     */
    private void triggerRagDelete(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                ragFeignClient.removeFromIndex(docId);
                log.info("Triggered RAG index deletion: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger RAG index deletion (RAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously triggers KAG graph construction (does not block the main flow)
     */
    private void triggerKAGBuild(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                kagFeignClient.buildGraph(docId);
                log.info("Triggered KAG graph construction: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger KAG graph construction (KAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously triggers KAG graph deletion (does not block the main flow)
     */
    private void triggerKAGDelete(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                kagFeignClient.deleteGraph(docId);
                log.info("Triggered KAG graph deletion: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger KAG graph deletion (KAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Synchronously triggers Neo4j graph deletion (does not block the main flow)
     *
     * <p>Directly calls the kb-graph service's deletion endpoint to clean up document graph
     * data in Neo4j, serving as a synchronous backup path for KAG's asynchronous RabbitMQ
     * deletion, ensuring graph data is promptly cleaned up after a document is deleted.</p>
     */
    private void triggerGraphDelete(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                graphFeignClient.deleteDocumentGraph(docId);
                log.info("Triggered Neo4j graph deletion: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger Neo4j graph deletion (kb-graph service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    // ==================== ES search index synchronization ====================

    /**
     * Asynchronously syncs a document to the ES search index (does not block the main flow)
     *
     * @param document document entity
     * @param content  document content (optional, fetched from MongoDB)
     */
    private void triggerSearchIndex(Document document, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> docData = buildSearchIndexData(document, content);
                searchIndexFeignClient.indexDocument(docData);
                log.info("Synced to ES search index: documentId={}, title={}", document.getId(), document.getTitle());
            } catch (Exception e) {
                log.warn("Failed to sync ES search index (search service unavailable, does not affect document operations): documentId={}, error={}",
                        document.getId(), e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously deletes a document from the ES search index (does not block the main flow)
     */
    private void triggerSearchDelete(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                searchIndexFeignClient.deleteDocumentIndex(docId);
                log.info("Deleted from ES search index: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to delete from ES search index (search service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Builds the document data Map needed for the search index
     */
    private Map<String, Object> buildSearchIndexData(Document document, String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", document.getId());
        data.put("title", document.getTitle());
        data.put("summary", document.getSummary());
        // Partial content (first 1000 characters, used for full-text search)
        if (StrUtil.isNotBlank(content)) {
            data.put("content", content.length() > 1000 ? content.substring(0, 1000) : content);
        } else if (StrUtil.isNotBlank(document.getContent())) {
            String c = document.getContent();
            data.put("content", c.length() > 1000 ? c.substring(0, 1000) : c);
        }
        data.put("categoryId", document.getCategoryId());
        data.put("tags", document.getTags());
        data.put("status", document.getStatus());
        data.put("isPublic", document.getIsPublic());
        data.put("viewCount", document.getViewCount());
        data.put("likeCount", document.getLikeCount());
        data.put("commentCount", document.getCommentCount());
        data.put("authorId", document.getAuthorId());
        data.put("author", Map.of("id", document.getAuthorId() != null ? document.getAuthorId() : 0,
                "username", document.getAuthorName() != null ? document.getAuthorName() : ""));
        data.put("publishTime", document.getPublishTime() != null ? document.getPublishTime().toString() : null);
        data.put("createdAt", document.getCreatedAt() != null ? document.getCreatedAt().toString() : null);
        data.put("updatedAt", document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : null);

        // Query the category name
        if (document.getCategoryId() != null) {
            try {
                Category category = categoryMapper.selectById(document.getCategoryId());
                if (category != null) {
                    data.put("categoryName", category.getCategoryName());
                }
            } catch (Exception e) {
                // Ignore category lookup failures
            }
        }

        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean likeDocument(Long documentId) {
        log.info("Like document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        Long userId = UserContext.getCurrentUserId();

        // Like (uses tb_like's unique constraint to prevent concurrent duplicate likes)
        likeService.like(documentId, userId, 1);

        // Increment the like count
        int updated = documentMapper.incrementLikeCount(documentId);

        // Publish a like statistics event to RabbitMQ
        statisticsEventPublisher.publishLikeEvent(userId, UserContext.getCurrentUserName(), documentId, document.getTitle());

        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlikeDocument(Long documentId) {
        log.info("Unlike document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        Long userId = UserContext.getCurrentUserId();

        // Unlike
        likeService.unlike(documentId, userId, 1);

        // Decrement the like count
        documentMapper.decrementLikeCount(documentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean favoriteDocument(Long documentId) {
        log.info("Favorite document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // TODO: check whether the user has already favorited it

        // Increment the favorite count
        int count = documentMapper.incrementFavoriteCount(documentId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishDocument(Long documentId) {
        log.info("Publish document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check system configuration: whether review is required
        if (!checkRequireApproval()) {
            log.info("System configuration has document review disabled, publishing document directly: documentId={}", documentId);
            return directPublishDocument(documentId);
        }

        // Review is required, submit for the review flow
        log.info("Submit document for review: documentId={}", documentId);
        return documentReviewService.submitForReview(documentId);
    }

    /**
     * Checks system configuration: whether document review is required
     *
     * @return true-review required, false-publish directly
     */
    private boolean checkRequireApproval() {
        String value = systemConfigCache.getConfig("system.requireApproval");
        return !"false".equals(value);
    }

    /**
     * Reads the maximum file size from kb_system_config
     */
    private long getMaxFileSizeFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.max.size");
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 20971520L; // Default 20MB
    }

    /**
     * Reads the allowed file type list from kb_system_config
     */
    private List<String> getAllowedFileTypesFromConfig() {
        String value = systemConfigCache.getConfig("file.upload.allowed.types");
        if (value != null && !value.isBlank()) {
            return Arrays.asList(value.toLowerCase().split(","));
        }
        return Arrays.asList("pdf", "doc", "docx", "xlsx", "pptx", "txt", "md", "jpg", "png", "gif");
    }

    /**
     * Directly publishes a document (no review required)
     * <p>Called when the system.requireApproval configuration is set to false</p>
     */
    private Boolean directPublishDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Only draft, published, or pending-review documents can be published directly
        Integer status = document.getStatus();
        if (!status.equals(DocumentStatus.DRAFT.getCode())
                && !status.equals(DocumentStatus.PUBLISHED.getCode())
                && !status.equals(DocumentStatus.PENDING_REVIEW.getCode())) {
            throw new BusinessException("The current document status does not allow publishing");
        }

        // Set directly to published
        document.setStatus(DocumentStatus.PUBLISHED.getCode());
        document.setPublishTime(LocalDateTime.now());
        documentMapper.updateById(document);

        // Trigger RAG/KAG/ES indexing
        triggerRagReindex(documentId);
        triggerKAGBuild(documentId);
        triggerSearchIndex(document, null);

        log.info("Document published directly successfully: documentId={}", documentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean archiveDocument(Long documentId) {
        log.info("Archive document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        Document document = new Document();
        document.setId(documentId);
        document.setStatus(2);

        int count = documentMapper.updateById(document);

        // Asynchronously remove this document's vector index from RAG (no longer searchable after archiving)
        if (count > 0) {
            triggerRagDelete(documentId);
            triggerKAGDelete(documentId);
            triggerSearchDelete(documentId);
            triggerGraphDelete(documentId);
        }

        return count > 0;
    }

    @Override
    public int rebuildAllGraphs() {
        log.info("Starting batch knowledge graph rebuild...");
        // Query all published, non-soft-deleted documents
        List<Document> publishedDocs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getStatus, 1));
        log.info("Found {} published documents, starting graph rebuild one by one", publishedDocs.size());

        for (Document doc : publishedDocs) {
            try {
                triggerKAGBuild(doc.getId());
                log.info("Triggered graph rebuild: documentId={}, title={}", doc.getId(), doc.getTitle());
            } catch (Exception e) {
                log.warn("Failed to trigger graph rebuild: documentId={}, title={}, error={}",
                        doc.getId(), doc.getTitle(), e.getMessage());
            }
        }

        log.info("All batch knowledge graph rebuild requests have been sent, {} in total", publishedDocs.size());
        return publishedDocs.size();
    }

    @Override
    public int cleanupGraphGhostNodes() {
        log.info("Starting knowledge graph stale node cleanup...");
        try {
            // 1. Query all valid document IDs from MySQL
            List<Document> allDocs = documentMapper.selectList(
                    new LambdaQueryWrapper<Document>().select(Document::getId));
            List<Long> validDocIds = allDocs.stream().map(Document::getId).collect(Collectors.toList());
            log.info("Valid document count in MySQL: {}", validDocIds.size());

            // 2. Call the kb-graph service to clean up stale nodes
            Map<String, List<Long>> body = Map.of("validDocIds", validDocIds);
            Result<String> result = graphFeignClient.cleanupDocumentGraph(body);
            log.info("Graph stale node cleanup result: {}", result != null ? result.getData() : null);
            return validDocIds.size();
        } catch (Exception e) {
            log.error("Failed to clean up graph stale nodes: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Recursively collects a category ID and all of its subcategory IDs
     */
    private List<Long> collectCategoryIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        List<Category> children = categoryMapper.selectByParentId(parentId);
        for (Category child : children) {
            ids.addAll(collectCategoryIds(child.getId()));
        }
        return ids;
    }

    @Override
    public void dismissAutoSaveDrafts() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            log.warn("User is not logged in, cannot acknowledge auto-saved drafts");
            return;
        }

        int updated = documentMapper.update(null,
                new LambdaUpdateWrapper<Document>()
                        .eq(Document::getAuthorId, userId)
                        .eq(Document::getStatus, 0)
                        .set(Document::getAutoSaveDismissed, 1));
        log.info("User {} acknowledged dismissal of auto-saved drafts, {} records updated", userId, updated);
    }
}
