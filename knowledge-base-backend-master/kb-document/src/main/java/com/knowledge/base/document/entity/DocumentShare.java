package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document share entity class
 *
 * <p>Stores document share information, including share link, expiration, and access permissions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document_share")
public class DocumentShare extends BaseEntity {

    /**
     * Share ID (unique identifier, used in the share link)
     */
    private String shareId;

    /**
     * Document ID
     */
    private Long documentId;

    /**
     * Share title
     */
    private String title;

    /**
     * Share type (1-public link, 2-direct message share)
     */
    private Integer shareType;

    /**
     * Share code (optional, used to add extra security)
     */
    private String shareCode;

    /**
     * Expiration type (1-permanent, 2-time-limited)
     */
    private Integer expireType;

    /**
     * Expiration time
     */
    private LocalDateTime expireTime;

    /**
     * Access count limit (0-unlimited)
     */
    private Integer accessLimit;

    /**
     * Number of times accessed
     */
    private Integer accessCount;

    /**
     * Whether a password is required (0-no, 1-yes)
     */
    private Integer requirePassword;

    /**
     * Access password (stored encrypted)
     */
    private String password;

    /**
     * Sharer ID
     */
    private Long sharerId;

    /**
     * Sharer name
     */
    private String sharerName;

    /**
     * Share description
     */
    private String description;

    /**
     * Status (0-active, 1-expired, 2-deleted)
     */
    private Integer status;

    /**
     * Share time
     */
    private LocalDateTime shareTime;
}
