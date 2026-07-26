package com.knowledge.base.common.enums;

import lombok.Getter;

/**
 * User type enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public enum UserType {

    /**
     * Super administrator
     */
    SUPER_ADMIN(0, "SUPER_ADMIN", "Super Administrator"),

    /**
     * Knowledge administrator
     */
    KNOWLEDGE_ADMIN(1, "KNOWLEDGE_ADMIN", "Knowledge Administrator"),

    /**
     * Content administrator
     */
    CONTENT_ADMIN(2, "CONTENT_ADMIN", "Content Administrator"),

    /**
     * Team leader
     */
    TEAM_LEADER(3, "TEAM_LEADER", "Team Leader"),

    /**
     * Contributor
     */
    CONTRIBUTOR(4, "CONTRIBUTOR", "Content Contributor"),

    /**
     * Regular user
     */
    VIEWER(5, "VIEWER", "Regular User");

    private final Integer code;
    private final String roleCode;
    private final String name;

    UserType(Integer code, String roleCode, String name) {
        this.code = code;
        this.roleCode = roleCode;
        this.name = name;
    }

    public static UserType getByCode(Integer code) {
        for (UserType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static UserType getByRoleCode(String roleCode) {
        for (UserType type : values()) {
            if (type.getRoleCode().equals(roleCode)) {
                return type;
            }
        }
        return null;
    }
}
