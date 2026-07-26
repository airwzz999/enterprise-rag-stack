package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.document.entity.UserFavorite;
import com.knowledge.base.document.vo.UserFavoriteVO;

import java.util.List;

/**
 * User favorite Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface UserFavoriteService extends IService<UserFavorite> {

    /**
     * Adds a favorite
     *
     * @param userId user ID
     * @param documentId document ID
     * @return whether successful
     */
    Boolean addFavorite(Long userId, Long documentId);

    /**
     * Removes a favorite
     *
     * @param userId user ID
     * @param documentId document ID
     * @return whether successful
     */
    Boolean removeFavorite(Long userId, Long documentId);

    /**
     * Checks whether a document is favorited
     *
     * @param userId user ID
     * @param documentId document ID
     * @return whether favorited
     */
    Boolean isFavorited(Long userId, Long documentId);

    /**
     * Gets the user's favorite list
     *
     * @param userId user ID
     * @return favorite list
     */
    List<UserFavoriteVO> getUserFavorites(Long userId);

    /**
     * Gets the favorite count for a document
     *
     * @param documentId document ID
     * @return favorite count
     */
    Long getFavoriteCount(Long documentId);

    /**
     * Toggles favorite status
     *
     * @param userId user ID
     * @param documentId document ID
     * @return favorite status (true-favorited, false-not favorited)
     */
    Boolean toggleFavorite(Long userId, Long documentId);
}
