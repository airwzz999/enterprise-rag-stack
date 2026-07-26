package com.knowledge.base.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus configuration class
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Optimistic lock plugin: automatically handles concurrency control for the version field</li>
 *   <li>Pagination plugin: automatically handles paginated queries</li>
 *   <li>Plugin to prevent full-table updates and deletes</li>
 * </ul>
 *
 * <p>Usage notes:</p>
 * <ul>
 *   <li>Optimistic locking: add the @Version annotation to the version field of the entity class</li>
 *   <li>Pagination: use a Page<T> object for paginated queries</li>
 *   <li>Logical delete: add the @TableLogic annotation to the deleted field of the entity class</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * Configure the MyBatis Plus interceptors
     *
     * <p>Interceptor execution order (in the order added):</p>
     * <ol>
     *   <li>Optimistic lock interceptor: automatically handles the version field on update</li>
     *   <li>Pagination interceptor: automatically handles pagination on query</li>
     *   <li>Interceptor preventing full-table updates and deletes</li>
     * </ol>
     *
     * @return MyBatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Add the optimistic lock plugin
        // Note: must be added before the pagination plugin
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // Add the pagination plugin
        // Auto-detects the database type based on the project's configured DbType
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // Add the plugin that prevents full-table updates and deletes
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * Custom ID generator (Snowflake algorithm)
     *
     * <p>Highest priority; overrides MyBatis-Plus's default IdWorker</p>
     * <p>Automatically invoked when using @TableId(type = IdType.ASSIGN_ID)</p>
     */
    @Bean
    public IdentifierGenerator customIdGenerator() {
        return new IdentifierGenerator() {
            @Override
            public Number nextId(Object entity) {
                return SnowflakeIdGenerator.getInstance().nextId();
            }
        };
    }
}
