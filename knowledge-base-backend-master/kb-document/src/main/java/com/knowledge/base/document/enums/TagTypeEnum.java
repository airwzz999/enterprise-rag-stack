package com.knowledge.base.document.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Tag type enum
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, defines tag types</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum TagTypeEnum {

    /**
     * System tag
     */
    SYSTEM(0, "System tag"),

    /**
     * User tag
     */
    USER(1, "User tag");

    /**
     * Type code
     */
    private final Integer code;

    /**
     * Type description
     */
    private final String desc;

    /**
     * Gets the enum by code
     *
     * @param code code
     * @return enum value, or null if not found
     */
    public static TagTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TagTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets the enum by code, returning the default value USER if not found
     *
     * @param code code
     * @return enum value
     */
    public static TagTypeEnum ofOrDefault(Integer code) {
        TagTypeEnum type = of(code);
        return type != null ? type : USER;
    }
}
