package com.shaadi.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Extract userId from query parameters
        String query = request.getURI().getQuery();
        if (query != null && query.contains("userId=")) {
            String userId = extractUserId(query);
            if (userId != null) {
                return new StompPrincipal(userId);
            }
        }
        
        // Fallback: generate a random principal if no userId provided
        return new StompPrincipal("anonymous-" + System.currentTimeMillis());
    }

    private String extractUserId(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("userId=")) {
                return param.substring(7); // "userId=".length() == 7
            }
        }
        return null;
    }

    // Simple Principal implementation
    private static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
