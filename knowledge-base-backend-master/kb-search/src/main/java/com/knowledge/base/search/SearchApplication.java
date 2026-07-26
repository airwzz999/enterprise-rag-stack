package com.knowledge.base.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Search service startup class
 *
 * @author airwzz999
 * @since 2026-04-24
 */
@EnableFeignClients(basePackages = "com.knowledge.base.search.feign")
@SpringBootApplication(scanBasePackages = "com.knowledge.base")
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
