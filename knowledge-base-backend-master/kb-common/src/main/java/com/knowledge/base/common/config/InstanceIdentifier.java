package com.knowledge.base.common.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Instance identifier — provides RabbitMQ resource isolation for local environments shared by multiple developers
 *
 * <p>All microservice modules running on the same machine share the same instanceId,
 * while instanceIds on different machines are independent of each other, ensuring that MQ messages
 * produced by developer A can only be consumed by developer A's consumers.</p>
 *
 * <p>Priority order for determining the instanceId:</p>
 * <ol>
 *   <li>The {@code app.instance.id} configuration property (explicitly specified)</li>
 *   <li>The local hostname (auto-detected)</li>
 *   <li>The first 8 characters of a random UUID (fallback)</li>
 * </ol>
 *
 * <p>Usage: append the instanceId to queue names and routing keys, for example:</p>
 * <pre>
 * Queue name:     kb.notification.review.queue.&#64;{instanceId}
 * Routing key:    notification.review.&#64;{instanceId}.submitted
 * Binding pattern: notification.review.&#64;{instanceId}.*
 * </pre>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class InstanceIdentifier implements InitializingBean {

    /**
     * Explicitly configured instance ID (highest priority)
     */
    @Value("${app.instance.id:}")
    private String configuredId;

    /**
     * The finally resolved instance ID
     */
    @Getter
    private String id;

    @Override
    public void afterPropertiesSet() {
        this.id = resolve();
        log.info("Current instance identifier (instanceId): {}", this.id);
    }

    private String resolve() {
        // 1. Explicit configuration (highest priority)
        if (configuredId != null && !configuredId.trim().isEmpty()) {
            return sanitize(configuredId.trim());
        }

        // 2. Local hostname
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.isBlank()) {
                // Strip the domain suffix, keep only the host name
                int dotIndex = hostname.indexOf('.');
                if (dotIndex > 0) {
                    hostname = hostname.substring(0, dotIndex);
                }
                return sanitize(hostname);
            }
        } catch (Exception e) {
            log.warn("Failed to get local hostname: {}", e.getMessage());
        }

        // 3. Random UUID fallback
        return "local-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Strip invalid characters, keeping only letters, digits, hyphens, underscores, and dots
     */
    private String sanitize(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
