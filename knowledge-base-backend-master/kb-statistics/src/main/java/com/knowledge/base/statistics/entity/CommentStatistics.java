package com.knowledge.base.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Comment statistics entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_comment")
public class CommentStatistics {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Document ID
     */
    private Long documentId;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;
}
