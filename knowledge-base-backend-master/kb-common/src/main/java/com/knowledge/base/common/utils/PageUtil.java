package com.knowledge.base.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.result.PageResult;

/**
 * Pagination utility class
 *
 * <p>Provides convenient methods for creating and converting MyBatis-Plus pagination objects</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class PageUtil {

    private PageUtil() {
    }

    /**
     * Create a MyBatis-Plus pagination object
     *
     * @param current the current page
     * @param size    the page size
     * @param <T>     the entity type
     * @return the pagination object
     */
    public static <T> Page<T> of(long current, long size) {
        return new Page<>(current, size);
    }

    /**
     * Convert a MyBatis-Plus IPage into the project's common PageResult
     *
     * @param iPage the MyBatis-Plus pagination result
     * @param <T>   the data type
     * @return the common pagination result
     */
    public static <T> PageResult<T> toPageResult(IPage<T> iPage) {
        return PageResult.<T>builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .total(iPage.getTotal())
                .pages(iPage.getPages())
                .records(iPage.getRecords())
                .build();
    }
}
