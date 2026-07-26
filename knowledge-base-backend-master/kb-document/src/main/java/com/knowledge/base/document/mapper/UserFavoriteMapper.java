package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.UserFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User favorite Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * Queries a user's favorite list (including document details)
     *
     * @param userId user ID
     * @return favorite list
     */
    @Select("SELECT uf.id, uf.user_id, uf.document_id, uf.document_title, uf.document_category_id, " +
            "uf.favorite_time, d.summary as document_summary, d.category_id as document_category_id, " +
            "d.author_id as document_author_id, d.author_name as document_author_name, " +
            "d.status as document_status, d.view_count as document_view_count, " +
            "d.created_at as document_created_at, d.updated_at as document_updated_at " +
            "FROM kb_user_favorite uf " +
            "LEFT JOIN kb_document d ON uf.document_id = d.id " +
            "WHERE uf.user_id = #{userId} AND uf.deleted = 0 " +
            "ORDER BY uf.favorite_time DESC")
    List<UserFavorite> getUserFavorites(@Param("userId") Long userId);

    /**
     * Checks whether a user has favorited a given document
     *
     * @param userId user ID
     * @param documentId document ID
     * @return favorite record
     */
    @Select("SELECT * FROM kb_user_favorite " +
            "WHERE user_id = #{userId} AND document_id = #{documentId} AND deleted = 0 " +
            "LIMIT 1")
    UserFavorite findByUserAndDocument(@Param("userId") Long userId, @Param("documentId") Long documentId);

    /**
     * Counts the favorites for a document
     *
     * @param documentId document ID
     * @return favorite count
     */
    @Select("SELECT COUNT(*) FROM kb_user_favorite " +
            "WHERE document_id = #{documentId} AND deleted = 0")
    Integer countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Physically deletes a favorite record (does not use logical delete)
     *
     * @param userId user ID
     * @param documentId document ID
     * @return delete result
     */
    @Delete("DELETE FROM kb_user_favorite " +
            "WHERE user_id = #{userId} AND document_id = #{documentId}")
    int physicalDelete(@Param("userId") Long userId, @Param("documentId") Long documentId);
}
