package com.knowledge.base.common.constants;

/**
 * System constants
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class Constants {

    /**
     * UTF-8 encoding
     */
    public static final String UTF8 = "UTF-8";

    /**
     * Default page size
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Maximum page size
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Token request header name
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token prefix
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token cache key prefix
     */
    public static final String TOKEN_CACHE_PREFIX = "token:";

    /**
     * User cache key prefix
     */
    public static final String USER_CACHE_PREFIX = "user:";

    /**
     * Document cache key prefix
     */
    public static final String DOC_CACHE_PREFIX = "doc:";

    /**
     * Category cache key prefix
     */
    public static final String CATEGORY_CACHE_PREFIX = "category:";

    /**
     * Tag cache key prefix
     */
    public static final String TAG_CACHE_PREFIX = "tag:";

    /**
     * Permission cache key prefix
     */
    public static final String PERMISSION_CACHE_PREFIX = "permission:";

    /**
     * Distributed lock key prefix
     */
    public static final String LOCK_PREFIX = "lock:";

    /**
     * Default password
     */
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * Super administrator ID
     */
    public static final Long SUPER_ADMIN_ID = 1000000000000000001L;

    /**
     * System user ID
     */
    public static final Long SYSTEM_USER_ID = 0L;

    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
