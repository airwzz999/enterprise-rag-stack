package com.knowledge.base.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.common.result.Result;
import io.netty.channel.ConnectTimeoutException;

import java.net.ConnectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Gateway global exception handler
 *
 * <p>Handles exceptions from both the gateway layer and backend services uniformly,
 * returning error responses in a consistent format</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Order(-1)
@Component("gatewayGlobalExceptionHandler")
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Set the response headers
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Build the error response based on the exception type
        Result<?> result;
        HttpStatus status;

        if (ex instanceof ResponseStatusException rse) {
            status = (HttpStatus) rse.getStatusCode();
            result = Result.error(status.value(), rse.getReason());
        } else if (ex instanceof ConnectTimeoutException || ex instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            result = Result.error(status.value(), "The backend service timed out, please try again later");
        } else if (ex instanceof ConnectException) {
            status = HttpStatus.BAD_GATEWAY;
            result = Result.error(status.value(), "The backend service is unavailable, please check its status");
        } else {
            log.error("Gateway exception: ", ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            result = Result.error(status.value(), "System error: " + ex.getMessage());
        }

        response.setStatusCode(status);

        try {
            String responseBody = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            return Mono.error(ex);
        }
    }
}
