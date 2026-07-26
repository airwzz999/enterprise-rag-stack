package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.DocumentAccess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Document access record Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentAccessMapper extends BaseMapper<DocumentAccess> {

    /**
     * Queries a user's recent access records, ordered by access time descending
     *
     * @param userId user ID
     * @param limit  query result limit
     * @return access record list
     */
    @Select("SELECT da.*, d.summary, d.category_id, c.category_name, d.author_name, d.status " +
            "FROM kb_document_access da " +
            "LEFT JOIN kb_document d ON da.document_id = d.id " +
            "LEFT JOIN kb_category c ON d.category_id = c.id " +
            "WHERE da.user_id = #{userId} AND d.deleted = 0 " +
            "ORDER BY da.access_time DESC " +
            "LIMIT #{limit}")
    List<DocumentAccess> selectRecentAccessByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * Deletes a user's access record
     *
     * @param userId user ID
     * @param documentId document ID
     * @return number deleted
     */
    int deleteByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);

    /**
     * Clears all of a user's access records
     *
     * @param userId user ID
     * @return number deleted
     */
    int deleteAllByUserId(@Param("userId") Long userId);
}
