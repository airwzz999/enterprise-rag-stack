package com.knowledge.base.foundation.websocket;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Resource
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Resource
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

    /**
     * Configure the message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable the simple message broker, used to push messages to clients
        registry.enableSimpleBroker("/topic", "/queue");
        // Destination prefix for messages sent by the client
        registry.setApplicationDestinationPrefixes("/app");
        // User message prefix (used by convertAndSendToUser)
        registry.setUserDestinationPrefix("/user/");
    }

    /**
     * Configure the STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint, allowing cross-origin requests
        // CustomHandshakeHandler ensures the Principal set by the HandshakeInterceptor
        // is correctly applied to the WebSocket session, so convertAndSendToUser can route messages
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .addInterceptors(webSocketHandshakeInterceptor)
                .withSockJS(); // Enable SockJS support
    }

    /**
     * Register the auth interceptor, extracting the user identity from the JWT on STOMP CONNECT
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
