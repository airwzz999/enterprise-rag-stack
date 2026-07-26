package com.knowledge.base.foundation.websocket;

import com.knowledge.base.common.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket handshake interceptor
 *
 * <p>Extracts the JWT token from the URL query parameters during the SockJS HTTP
 * handshake phase. Once validated, it sets the session Principal to ensure that
 * subsequent calls to {@code convertAndSendToUser} route correctly to that user.</p>
 *
 * <p>The Authorization header in the STOMP CONNECT frame arrives late (after the
 * handshake completes), so it cannot be used to retroactively update the Principal of an
 * already-established WebSocket session. Authentication must therefore be completed in
 * this interceptor rather than relying solely on a ChannelInterceptor.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
        // Read the token from the URL query parameters
        String token = null;
        if (request.getURI().getQuery() != null) {
            String[] queryParams = request.getURI().getQuery().split("&");
            for (String param : queryParams) {
                String[] kv = param.split("=", 2);
                if ("token".equals(kv[0]) && kv.length > 1) {
                    token = kv[1];
                    break;
                }
            }
        }

        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserId(token);
                    if (userId != null) {
                        String userIdStr = String.valueOf(userId);
                        // Key point: setting the Principal in attributes lets Spring use it as the session Principal
                        attributes.put("principal", new Principal() {
                            @Override
                            public String getName() {
                                return userIdStr;
                            }
                        });
                        log.debug("WebSocket handshake authentication succeeded: userId={}", userId);
                        return true;
                    }
                } else {
                    log.warn("WebSocket handshake authentication failed: token is invalid or expired");
                }
            } catch (Exception e) {
                log.warn("WebSocket handshake authentication error: {}", e.getMessage());
            }
        } else {
            log.debug("WebSocket handshake did not include a token parameter; connecting anonymously");
        }

        // Allow the connection even without a token (anonymous users can still receive broadcast messages)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler,
                                Exception exception) {
        // no-op
    }
}
