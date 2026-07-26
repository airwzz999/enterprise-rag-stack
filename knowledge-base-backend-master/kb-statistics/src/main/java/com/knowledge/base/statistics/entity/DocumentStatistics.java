package com.knowledge.base.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document statistics entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_document")
public class DocumentStatistics {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Title
     */
    private String title;

    /**
     * Author ID
     */
    private Long authorId;

    /**
     * Category ID
     */
    private Long categoryId;

    /**
     * Status
     */
    private Integer status;

    /**
     * View count
     */
    private Long viewCount;

    /**
     * Like count
     */
    private Long likeCount;

    /**
     * Favorite count
     */
    private Long favoriteCount;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;

    /**
     * Whether deleted (0 = not deleted, 1 = deleted)
     */
    private Integer deleted;
}
