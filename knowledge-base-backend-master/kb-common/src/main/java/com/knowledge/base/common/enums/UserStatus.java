package com.knowledge.base.common.enums;

import lombok.Getter;

/**
 * User status enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public enum UserStatus {

    /**
     * Disabled
     */
    DISABLED(0, "Disabled"),

    /**
     * Normal
     */
    NORMAL(1, "Normal"),

    /**
     * Locked
     */
    LOCKED(2, "Locked");

    private final Integer code;
    private final String message;

    UserStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static UserStatus getByCode(Integer code) {
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
