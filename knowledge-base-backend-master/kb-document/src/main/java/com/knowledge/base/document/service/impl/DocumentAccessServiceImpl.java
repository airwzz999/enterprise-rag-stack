package com.knowledge.base.document.service.impl;

import com.knowledge.base.document.entity.DocumentAccess;
import com.knowledge.base.document.mapper.DocumentAccessMapper;
import com.knowledge.base.document.service.DocumentAccessService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.DocumentAccessVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document access record service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements document access record related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAccessServiceImpl implements DocumentAccessService {

    private final DocumentAccessMapper documentAccessMapper;

    /**
     * Default query result limit
     */
    private static final int DEFAULT_LIMIT = 20;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAccess(Long userId, Long documentId, String documentTitle) {
        if (userId == null) {
            log.warn("User not logged in, skipping access record");
            return;
        }

        // Delete the old record first to ensure uniqueness
        documentAccessMapper.deleteByUserIdAndDocumentId(userId, documentId);

        // Insert the new record
        DocumentAccess access = new DocumentAccess();
        access.setUserId(userId);
        access.setDocumentId(documentId);
        access.setDocumentTitle(documentTitle);
        access.setAccessTime(LocalDateTime.now());
        documentAccessMapper.insert(access);

        log.debug("Recorded access for user {} to document {} ({})", userId, documentId, documentTitle);
    }

    @Override
    public List<DocumentAccessVO> getRecentAccess(Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            log.warn("User not logged in, returning empty access list");
            return List.of();
        }

        int queryLimit = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;
        List<DocumentAccess> accessList = documentAccessMapper.selectRecentAccessByUserId(userId, queryLimit);

        return accessList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccess(Long documentId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            log.warn("User not logged in, skipping delete access");
            return;
        }

        documentAccessMapper.deleteByUserIdAndDocumentId(userId, documentId);
        log.debug("Deleted access record for user {} to document {}", userId, documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllAccess() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            log.warn("User not logged in, skipping clear access");
            return;
        }

        documentAccessMapper.deleteAllByUserId(userId);
        log.debug("Cleared all access records for user {}", userId);
    }

    /**
     * Converts the entity to a VO
     */
    private DocumentAccessVO convertToVO(DocumentAccess access) {
        DocumentAccessVO vo = new DocumentAccessVO();
        vo.setId(access.getId());
        vo.setUserId(access.getUserId());
        vo.setDocumentId(access.getDocumentId());
        vo.setDocumentTitle(access.getDocumentTitle());
        vo.setAccessTime(access.getAccessTime());
        vo.setSummary(access.getSummary());
        vo.setCategoryName(access.getCategoryName());
        vo.setAuthorName(access.getAuthorName());
        vo.setStatus(access.getStatus());
        return vo;
    }
}
