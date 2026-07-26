package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.DocumentReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

/**
 * Document review Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentReviewMapper extends BaseMapper<DocumentReview> {

    /**
     * Queries the latest review record for a document.
     *
     * @param documentId document ID
     * @return latest review record
     */
    DocumentReview selectLatestByDocumentId(@Param("documentId") Long documentId);
}
