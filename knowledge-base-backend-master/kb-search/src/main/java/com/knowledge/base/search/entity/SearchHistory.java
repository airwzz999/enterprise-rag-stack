package com.knowledge.base.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Search history entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_search_history")
public class SearchHistory {

    /**
     * Primary key ID (generated with the Snowflake algorithm)
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Search keyword
     */
    private String keyword;

    /**
     * Search count
     */
    private Integer searchCount;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Auto-fill the Snowflake ID before insert
     */
    public void preInsert() {
        if (this.id == null) {
            this.id = SnowflakeIdGenerator.getInstance().nextId();
        }
    }
}
