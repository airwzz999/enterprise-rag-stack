package com.knowledge.base.document.constants;

/**
 * Document service permission point constants.
 *
 * <p>Centralizes maintenance of document-domain permission codes, avoiding scattered
 * hardcoding at the controller layer, and making permission governance, code review, and
 * future unified refactoring easier.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public final class DocumentPermissionConstants {

    private DocumentPermissionConstants() {
    }

    /**
     * View documents and query lists.
     */
    public static final String DOCUMENT_LIST = "document:list";

    /**
     * Create a document.
     */
    public static final String DOCUMENT_CREATE = "document:create";

    /**
     * Edit a document.
     */
    public static final String DOCUMENT_EDIT = "document:edit";

    /**
     * Delete a document.
     */
    public static final String DOCUMENT_DELETE = "document:delete";

    /**
     * Review a document.
     */
    public static final String DOCUMENT_REVIEW = "document:review";

    /**
     * Category management.
     */
    public static final String DOCUMENT_CATEGORY = "document:category";

    /**
     * Category query.
     */
    public static final String DOCUMENT_CATEGORY_QUERY = "document:category:query";

    /**
     * Tag management.
     */
    public static final String DOCUMENT_TAG = "document:tag";

    /**
     * Version management.
     */
    public static final String DOCUMENT_VERSION = "document:version";
}
