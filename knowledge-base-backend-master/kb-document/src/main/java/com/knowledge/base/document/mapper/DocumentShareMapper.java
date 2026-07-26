package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.DocumentShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Document share Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentShareMapper extends BaseMapper<DocumentShare> {

    /**
     * Queries by share ID
     *
     * @param shareId share ID
     * @return share information
     */
    @Select("SELECT * FROM kb_document_share WHERE share_id = #{shareId} AND deleted = 0")
    DocumentShare selectByShareId(@Param("shareId") String shareId);

    /**
     * Queries all valid shares for a document
     *
     * @param documentId document ID
     * @return share list
     */
    @Select("SELECT * FROM kb_document_share WHERE document_id = #{documentId} AND status = 0 AND deleted = 0 ORDER BY share_time DESC")
    List<DocumentShare> selectValidSharesByDocumentId(@Param("documentId") Long documentId);

    /**
     * Queries the share list by sharer ID
     *
     * @param sharerId sharer ID
     * @return share list
     */
    @Select("SELECT * FROM kb_document_share WHERE sharer_id = #{sharerId} AND deleted = 0 ORDER BY share_time DESC")
    List<DocumentShare> selectBySharerId(@Param("sharerId") Long sharerId);
}
