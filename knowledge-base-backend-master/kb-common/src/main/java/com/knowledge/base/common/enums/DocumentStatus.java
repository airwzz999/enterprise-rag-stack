package com.knowledge.base.common.enums;

import lombok.Getter;

/**
 * Document status enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public enum DocumentStatus {

    /**
     * Draft
     */
    DRAFT(0, "Draft"),

    /**
     * Published
     */
    PUBLISHED(1, "Published"),

    /**
     * Archived
     */
    ARCHIVED(2, "Archived"),

    /**
     * Pending review
     */
    PENDING_REVIEW(3, "Pending review");

    private final Integer code;
    private final String name;

    DocumentStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DocumentStatus getByCode(Integer code) {
        for (DocumentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
