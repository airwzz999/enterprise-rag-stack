package com.knowledge.base.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global logging filter
 *
 * <p>Designed following the Alibaba Java Development Guidelines; logs all request and response information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Log the request information
        log.info("Request => Method: {}, URI: {}, RemoteAddress: {}",
            request.getMethod(),
            request.getURI(),
            request.getRemoteAddress());

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long endTime = System.currentTimeMillis();
            log.info("Response => StatusCode: {}, Time: {}ms",
                exchange.getResponse().getStatusCode(),
                endTime - startTime);
        }));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
