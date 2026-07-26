package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Like entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("tb_like")
public class Like {

    /**
     * Like ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Target ID (document or comment)
     */
    private Long targetId;

    /**
     * Target type: 1-document, 2-comment
     */
    private Integer targetType;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;
}
