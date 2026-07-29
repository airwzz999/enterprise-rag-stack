package com.knowledge.base.graph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.knowledge.base.graph", "com.knowledge.base.common"})
@EnableNeo4jRepositories(basePackages = "com.knowledge.base.graph.repository")
@EnableFeignClients(basePackages = "com.knowledge.base.graph.feign")
public class GraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphApplication.class, args);
    }
}