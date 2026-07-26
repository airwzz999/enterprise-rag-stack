package com.knowledge.base.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Unified response filter
 *
 * <p>Based on the AuthFilter implementation from the susan-mall-cloud project; wraps response bodies uniformly</p>
 * <p>Main responsibilities:</p>
 * <ul>
 *   <li>Intercepts backend service responses to ensure a consistent response format</li>
 *   <li>Handles chunked/streamed data</li>
 *   <li>Automatically wraps non-standard-format responses</li>
 *   <li>Logs the full response</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class UnifiedResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    String contentType = getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    // Only process JSON responses
                    if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);

                        // Handle chunked/streamed data
                        return super.writeWith(fluxBody.buffer().flatMap(dataBuffers -> {
                            String responseData;
                            try {
                                // Merge the bytes of all DataBuffers first (avoids UTF-8 multi-byte characters
                                // being split across chunks and garbled during decoding)
                                int totalSize = 0;
                                for (DataBuffer dataBuffer : dataBuffers) {
                                    totalSize += dataBuffer.readableByteCount();
                                }
                                byte[] allBytes = new byte[totalSize];
                                int offset = 0;
                                for (DataBuffer dataBuffer : dataBuffers) {
                                    int size = dataBuffer.readableByteCount();
                                    dataBuffer.read(allBytes, offset, size);
                                    offset += size;
                                }
                                // Decode all bytes at once to preserve the integrity of UTF-8 multi-byte
                                // characters (such as CJK characters)
                                responseData = new String(allBytes, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                log.error("Error reading the response byte stream: {}", e.getMessage(), e);
                                responseData = "";
                            }

                            // Release the original data buffers
                            dataBuffers.forEach(DataBufferUtils::release);
                            log.info("Gateway forwarded response: URI={}, Status={}, Response={}",
                                    exchange.getRequest().getURI(),
                                    getStatusCode(),
                                    responseData);

                            // Wrap the response data
                            String wrappedResponse = wrapResponse(responseData);
                            byte[] uppedContent = wrappedResponse.getBytes(StandardCharsets.UTF_8);

                            // Set Content-Type to ensure UTF-8 encoding
                            getDelegate().getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
                            // Update Content-Length
                            getDelegate().getHeaders().setContentLength(uppedContent.length);

                            // Return the new data buffer
                            return Mono.just(bufferFactory.wrap(uppedContent));
                        }));
                    }
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * Wrap the response data
     * <p>If the response data is already in the standard format, return it as-is; otherwise wrap it into the standard format</p>
     *
     * @param responseData the original response data
     * @return the wrapped response data
     */
    private String wrapResponse(String responseData) {
        try {
            // Try to parse as JSON
            Object json = JSON.parse(responseData);
            if (json instanceof JSONObject) {
                JSONObject obj = (JSONObject) json;
                // Check whether it already contains code and message fields (the standard Result format)
                if (obj.containsKey("code") && obj.containsKey("message")) {
                    return responseData;
                }
            }
        } catch (Exception ignored) {
            // JSON parsing failed, meaning it isn't the standard format and needs wrapping
        }

        // Wrap into the standard Result format
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", JSON.parse(responseData));
        result.put("timestamp", System.currentTimeMillis());

        return result.toJSONString();
    }

    @Override
    public int getOrder() {
        return -2; // Set a higher priority
    }
}
