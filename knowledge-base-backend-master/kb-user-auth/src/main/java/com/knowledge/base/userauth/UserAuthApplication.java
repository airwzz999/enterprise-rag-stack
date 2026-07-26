package com.knowledge.base.userauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.knowledge.base.userauth", "com.knowledge.base.common"})
public class UserAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserAuthApplication.class, args);
        System.out.println("========================================");
        System.out.println("User Auth Service started successfully!");
        System.out.println("Swagger docs: http://localhost:8081/api/auth/doc.html");
        System.out.println("========================================");
    }
}