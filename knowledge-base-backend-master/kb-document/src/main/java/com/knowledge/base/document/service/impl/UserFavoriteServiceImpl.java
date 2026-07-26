package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.UserFavorite;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.UserFavoriteMapper;
import com.knowledge.base.document.service.UserFavoriteService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.AuthorVO;
import com.knowledge.base.document.vo.UserFavoriteVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User favorite Service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {

    @Resource
    private UserFavoriteMapper userFavoriteMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addFavorite(Long userId, Long documentId) {
        log.info("🆕 [UserFavoriteService] addFavorite started: userId={}, documentId={}", userId, documentId);

        // Verify the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("❌ [UserFavoriteService] Document does not exist: documentId={}", documentId);
            throw new BusinessException("Document does not exist");
        }
        log.info("✅ [UserFavoriteService] Document exists: title={}", document.getTitle());

        // Check whether it is already favorited
        UserFavorite existingFavorite = userFavoriteMapper.findByUserAndDocument(userId, documentId);
        if (existingFavorite != null) {
            log.warn("⚠️ [UserFavoriteService] User already favorited this document: userId={}, documentId={}, existingFavorite.deleted={}", userId, documentId, existingFavorite.getDeleted());
            return true;
        }
        log.info("✅ [UserFavoriteService] No existing favorite record found, preparing to create a new record");

        // Create the favorite record
        UserFavorite favorite = new UserFavorite();
        favorite.setId(SnowflakeIdGenerator.getInstance().nextId());
        favorite.setUserId(userId);
        favorite.setDocumentId(documentId);
        favorite.setDocumentTitle(document.getTitle());
        favorite.setDocumentCategoryId(document.getCategoryId());
        log.info("📝 [UserFavoriteService] Creating favorite record: id={}, userId={}, documentId={}", favorite.getId(), userId, documentId);

        int count = userFavoriteMapper.insert(favorite);
        log.info("🔢 [UserFavoriteService] Insert result: count={}", count);

        if (count > 0) {
            // Update the document's favorite count
            documentMapper.incrementFavoriteCount(documentId);
            log.info("✅ [UserFavoriteService] Favorite added successfully, returning true");
            return true;
        } else {
            log.error("❌ [UserFavoriteService] Insert failed, returning false");
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeFavorite(Long userId, Long documentId) {
        log.info("❌ [UserFavoriteService] removeFavorite started: userId={}, documentId={}", userId, documentId);

        // Use the physical delete method to avoid unique constraint conflicts with logical delete
        log.info("🗑️ [UserFavoriteService] Performing physical delete");

        int count = userFavoriteMapper.physicalDelete(userId, documentId);
        log.info("🔢 [UserFavoriteService] Physical delete result: count={}", count);

        if (count > 0) {
            // Decrement the document's favorite count
            documentMapper.decrementFavoriteCount(documentId);
            log.info("✅ [UserFavoriteService] Favorite removed successfully, returning true");
            return true;
        } else {
            log.warn("⚠️ [UserFavoriteService] No record found to delete, returning false");
            return false;
        }
    }

    @Override
    public Boolean isFavorited(Long userId, Long documentId) {
        UserFavorite favorite = userFavoriteMapper.findByUserAndDocument(userId, documentId);
        return favorite != null;
    }

    @Override
    public List<UserFavoriteVO> getUserFavorites(Long userId) {
        log.info("Get user favorite list: userId={}", userId);

        List<UserFavorite> favorites = userFavoriteMapper.getUserFavorites(userId);
        List<UserFavoriteVO> result = new ArrayList<>();

        for (UserFavorite favorite : favorites) {
            UserFavoriteVO vo = new UserFavoriteVO();
            vo.setId(favorite.getId());
            vo.setUserId(favorite.getUserId());
            vo.setDocumentId(favorite.getDocumentId());
            vo.setDocumentTitle(favorite.getDocumentTitle());
            vo.setDocumentSummary(favorite.getDocumentSummary());
            vo.setDocumentCategoryId(favorite.getDocumentCategoryId());
            vo.setDocumentAuthorName(favorite.getDocumentAuthorName());
            vo.setFavoriteTime(favorite.getFavoriteTime());
            result.add(vo);
        }

        return result;
    }

    @Override
    public Long getFavoriteCount(Long documentId) {
        Integer count = userFavoriteMapper.countByDocumentId(documentId);
        return count != null ? count.longValue() : 0L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleFavorite(Long userId, Long documentId) {
        log.info("🔄 [UserFavoriteService] toggleFavorite started: userId={}, documentId={}", userId, documentId);

        boolean isFavorited = isFavorited(userId, documentId);
        log.info("📊 [UserFavoriteService] Current favorite status: isFavorited={}", isFavorited);

        boolean newStatus;
        if (isFavorited) {
            log.info("❌ [UserFavoriteService] Performing remove-favorite operation");
            boolean success = removeFavorite(userId, documentId);
            newStatus = false; // After removing the favorite, the status is not favorited
            log.info("✅ [UserFavoriteService] Remove favorite complete: success={}, new status=not favorited({})", success, newStatus);
        } else {
            log.info("⭐ [UserFavoriteService] Performing add-favorite operation");
            boolean success = addFavorite(userId, documentId);
            newStatus = true; // After adding the favorite, the status is favorited
            log.info("✅ [UserFavoriteService] Add favorite complete: success={}, new status=favorited({})", success, newStatus);
        }

        log.info("🎯 [UserFavoriteService] toggleFavorite returning final new status: newStatus={}", newStatus);
        return newStatus;
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
        authorVO.setEmail("");
        authorVO.setAvatar("");
        authorVO.setPosition("Employee");
        return authorVO;
    }
}
