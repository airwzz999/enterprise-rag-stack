package com.knowledge.base.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async task thread pool configuration
 *
 * <p>All {@code CompletableFuture.runAsync/supplyAsync} calls must use this thread pool;
 * the default ForkJoinPool.commonPool() must not be used.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class AsyncTaskConfig {

    @Bean(name = "asyncTaskExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        // Core pool size = number of CPU cores, max pool size = core pool size x 2
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores * 2);
        // Queue capacity
        executor.setQueueCapacity(500);
        // Thread name prefix
        executor.setThreadNamePrefix("async-task-");
        // Wait for tasks to complete before shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // Await termination timeout
        executor.setAwaitTerminationSeconds(60);
        // Rejection policy: run on the calling thread
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Async task thread pool initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                cores, cores * 2, 500);
        return executor;
    }
}
