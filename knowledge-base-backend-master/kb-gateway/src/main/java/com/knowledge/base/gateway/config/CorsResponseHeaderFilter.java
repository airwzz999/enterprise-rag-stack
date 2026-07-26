package com.knowledge.base.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CORS response header deduplication filter
 *
 * <p>Based on the CORS solution from the susan-mall-cloud project</p>
 * <p>Strips duplicate CORS headers set by backend services, ensuring only the CORS
 * headers set at the gateway layer are returned to the client</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {

    private static final String ANY = "*";

    @Override
    public int getOrder() {
        // Runs after NettyWriteResponseFilter to ensure deduplication happens after response headers are finalized
        return org.springframework.cloud.gateway.filter.NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                if (exchange.getResponse().isCommitted()) {
                    return;
                }
                HttpHeaders headers = exchange.getResponse().getHeaders();
                if (headers == null || headers.isEmpty()) {
                    return;
                }

                // Use a safer approach to iterate the headers, avoiding a NullPointerException in the Reactor environment
                // Operate directly on the specific CORS headers instead of iterating the whole entrySet
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
                deduplicateCorsHeader(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
                deduplicateVaryHeader(headers);

            } catch (Exception e) {
                log.error("Error while deduplicating CORS response headers", e);
            }
        }));
    }

    /**
     * Deduplicate a CORS header
     *
     * @param headers HTTP response headers
     * @param headerName the header name to deduplicate
     */
    private void deduplicateCorsHeader(HttpHeaders headers, String headerName) {
        try {
            List<String> values = headers.get(headerName);
            if (values == null || values.size() <= 1) {
                return;
            }

            List<String> deduplicatedValues = new ArrayList<>();
            // Access-Control-Allow-Origin cannot be * when Access-Control-Allow-Credentials is true
            // If there are multiple values, prefer keeping a specific non-* origin
            if (headerName.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                String nonAny = values.stream().filter(v -> !ANY.equals(v)).findFirst().orElse(ANY);
                deduplicatedValues.add(nonAny);
            } else if (values.contains(ANY)) {
                deduplicatedValues.add(ANY);
            } else {
                deduplicatedValues.add(values.get(0));
            }

            headers.put(headerName, deduplicatedValues);
            log.debug("Deduplicated CORS header: {} -> {}", headerName, deduplicatedValues);
        } catch (Exception e) {
            log.warn("Error while processing CORS header: headerName={}, error={}", headerName, e.getMessage());
        }
    }

    /**
     * Deduplicate the Vary header
     *
     * @param headers HTTP response headers
     */
    private void deduplicateVaryHeader(HttpHeaders headers) {
        try {
            List<String> varyValues = headers.get(HttpHeaders.VARY);
            if (varyValues == null || varyValues.size() <= 1) {
                return;
            }

            List<String> deduplicatedValues = varyValues.stream()
                    .distinct()
                    .collect(Collectors.toList());

            headers.put(HttpHeaders.VARY, deduplicatedValues);
            log.debug("Deduplicated Vary header: {} -> {}", HttpHeaders.VARY, deduplicatedValues);
        } catch (Exception e) {
            log.warn("Error while processing the Vary header: error={}", e.getMessage());
        }
    }
}
