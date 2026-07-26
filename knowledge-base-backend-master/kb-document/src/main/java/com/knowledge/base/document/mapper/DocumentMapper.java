package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.dto.CategoryDocCountDTO;
import com.knowledge.base.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.knowledge.base.document.vo.DocumentNeighborVO;
import java.util.List;

/**
 * Document Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    int incrementViewCount(@Param("documentId") Long documentId);

    int incrementLikeCount(@Param("documentId") Long documentId);

    int decrementLikeCount(@Param("documentId") Long documentId);

    int incrementFavoriteCount(@Param("documentId") Long documentId);

    int decrementFavoriteCount(@Param("documentId") Long documentId);

    int incrementCommentCount(@Param("documentId") Long documentId);

    /**
     * Counts documents by category
     */
    List<CategoryDocCountDTO> countByCategory();

    /**
     * Queries the previous document (default order: is_top DESC, sort DESC, publish_time DESC)
     */
    DocumentNeighborVO selectPrev(@Param("isTop") Integer isTop,
                                  @Param("sort") Integer sort,
                                  @Param("publishTime") java.time.LocalDateTime publishTime);

    /**
     * Queries the next document (default order: is_top DESC, sort DESC, publish_time DESC)
     */
    DocumentNeighborVO selectNext(@Param("isTop") Integer isTop,
                                  @Param("sort") Integer sort,
                                  @Param("publishTime") java.time.LocalDateTime publishTime);
}
