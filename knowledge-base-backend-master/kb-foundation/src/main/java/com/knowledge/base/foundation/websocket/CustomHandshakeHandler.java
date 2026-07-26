package com.knowledge.base.foundation.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Custom WebSocket handshake handler
 *
 * <p>Overrides {@link #determineUser} to read the Principal set by
 * {@link WebSocketHandshakeInterceptor} from the attributes, ensuring the WebSocket
 * session carries the correct user identity.</p>
 *
 * <p>By default, Spring's {@link DefaultHandshakeHandler#determineUser} only obtains the
 * Principal from the ServerHttpRequest; but a WebSocket connection established via SockJS
 * has no Servlet Filter chain to set the Principal. This class therefore reads it from the
 * handshake attributes and returns it.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        // Read the Principal set by the HandshakeInterceptor from the attributes
        Principal principal = (Principal) attributes.get("principal");
        if (principal != null) {
            log.debug("CustomHandshakeHandler.determineUser: obtained Principal from attributes, name={}", principal.getName());
            return principal;
        }

        // Fall back to the default behavior
        Principal requestPrincipal = request.getPrincipal();
        if (requestPrincipal != null) {
            log.debug("CustomHandshakeHandler.determineUser: obtained Principal from request, name={}", requestPrincipal.getName());
            return requestPrincipal;
        }

        log.debug("CustomHandshakeHandler.determineUser: no Principal, connecting anonymously");
        return super.determineUser(request, wsHandler, attributes);
    }
}
