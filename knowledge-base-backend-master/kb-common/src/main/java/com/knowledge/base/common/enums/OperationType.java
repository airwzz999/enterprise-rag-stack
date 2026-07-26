package com.knowledge.base.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Operation type enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OperationType {

    /**
     * Query operation
     */
    QUERY("Query"),

    /**
     * Create operation
     */
    CREATE("Create"),

    /**
     * Update operation
     */
    UPDATE("Update"),

    /**
     * Delete operation
     */
    DELETE("Delete"),

    /**
     * Export operation
     */
    EXPORT("Export"),

    /**
     * Import operation
     */
    IMPORT("Import"),

    /**
     * Login operation
     */
    LOGIN("Login"),

    /**
     * Logout operation
     */
    LOGOUT("Logout"),

    /**
     * Review operation
     */
    REVIEW("Review"),

    /**
     * Other operation
     */
    OTHER("Other");

    /**
     * Operation type description
     */
    private final String description;

    /**
     * Get an operation type by its description
     *
     * @param description the operation description
     * @return the operation type
     */
    public static OperationType fromDescription(String description) {
        for (OperationType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return OTHER;
    }
}
