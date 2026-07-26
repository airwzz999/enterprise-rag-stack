package com.knowledge.base.document.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration class
 *
 * <p>Configures the MongoDB connection</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.knowledge.base.document.repository.mongodb")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.host:localhost}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port:27017}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database:knowledge_base}")
    private String databaseName;

    @Value("${spring.data.mongodb.username:}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        // Build the MongoDB connection URI
        StringBuilder mongoUri = new StringBuilder("mongodb://");

        // Add authentication info (if username and password are configured)
        if (username != null && !username.isEmpty()) {
            mongoUri.append(username).append(":").append(password).append("@");
        }

        // Add host and port
        mongoUri.append(mongoHost).append(":").append(mongoPort);

        // Add database name
        mongoUri.append("/").append(databaseName);

        // Add auth source and connection options
        mongoUri.append("?authSource=admin");
        mongoUri.append("&connectTimeoutMS=30000");
        mongoUri.append("&socketTimeoutMS=60000");
        mongoUri.append("&serverSelectionTimeoutMS=30000");
        // MongoDB uses UTF-8 encoding by default, no extra configuration needed

        System.out.println("Connecting to MongoDB: " + mongoUri.toString());

        return MongoClients.create(mongoUri.toString());
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
