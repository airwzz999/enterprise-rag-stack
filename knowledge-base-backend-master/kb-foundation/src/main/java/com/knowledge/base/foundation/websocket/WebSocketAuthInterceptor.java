package com.knowledge.base.foundation.websocket;

import com.knowledge.base.common.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket STOMP auth interceptor
 *
 * <p>Extracts the JWT token from the Authorization header during the STOMP CONNECT
 * phase. Once validated, it sets the userId as the session Principal so that
 * {@code convertAndSendToUser} can route messages correctly.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtUtil.validateToken(token)) {
                        Long userId = jwtUtil.getUserId(token);
                        if (userId != null) {
                            accessor.setUser(new Principal() {
                                @Override
                                public String getName() {
                                    return String.valueOf(userId);
                                }
                            });
                            log.debug("WebSocket authentication succeeded: userId={}", userId);
                        }
                    } else {
                        log.warn("WebSocket authentication failed: token is invalid or expired");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket authentication error: {}", e.getMessage());
                }
            } else {
                log.debug("WebSocket CONNECT did not include an Authorization header; connecting anonymously (can only receive broadcast messages)");
            }
        }

        return message;
    }
}
