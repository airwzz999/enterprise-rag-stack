package com.knowledge.base.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Lettuce Redis client configuration
 *
 * Addresses intermittent timeout issues with remote Redis:
 * 1. TCP KeepAlive - prevents intermediate network devices (firewall/NAT/load balancer) from reclaiming idle connections
 * 2. pingBeforeActivateConnection - PING to verify availability when a connection is taken from the pool
 * 3. autoReconnect - automatically reconnect after a disconnect
 * 4. Timeout configuration - separate TCP connect timeout and command timeout
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(LettuceConnectionFactory.class)
public class LettuceConfig {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return builder -> {

            // TCP KeepAlive: send a probe after 60s idle, 30s interval, disconnect after 3 failures
            SocketOptions.KeepAliveOptions keepAliveOptions = SocketOptions.KeepAliveOptions.builder()
                    .enable(true)
                    .idle(Duration.ofSeconds(60))
                    .interval(Duration.ofSeconds(30))
                    .count(3)
                    .build();

            SocketOptions socketOptions = SocketOptions.builder()
                    .connectTimeout(Duration.ofSeconds(3))   // TCP handshake timeout 3s
                    .keepAlive(keepAliveOptions)
                    .tcpNoDelay(true)                         // Disable Nagle's algorithm
                    .build();

            ClientOptions clientOptions = ClientOptions.builder()
                    .autoReconnect(true)                      // Auto reconnect
                    .pingBeforeActivateConnection(true)       // Verify with PING before activating connection
                    .socketOptions(socketOptions)
                    .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(10))) // Command timeout 10s
                    .build();

            builder.clientOptions(clientOptions);
        };
    }
}
