package com.knowledge.base.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Transaction manager configuration
 *
 * <p>Fixes the issue where, with both MyBatis-Plus and spring-data-neo4j on the
 * classpath in the kb-ai module, the {@code @Transactional} annotation defaults
 * to routing to Neo4jTransactionManager. Marks the JDBC DataSourceTransactionManager
 * as @Primary, ensuring all database operations that don't explicitly specify a
 * transactionManager use JDBC transactions.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class TransactionManagerConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
