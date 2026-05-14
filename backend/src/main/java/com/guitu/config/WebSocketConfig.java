package com.guitu.config;

import com.guitu.security.WebSocketAuthHandshakeInterceptor;
import com.guitu.websocket.MessageSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final MessageSocketHandler messageSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;

    public WebSocketConfig(
            MessageSocketHandler messageSocketHandler,
            WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor
    ) {
        this.messageSocketHandler = messageSocketHandler;
        this.webSocketAuthHandshakeInterceptor = webSocketAuthHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageSocketHandler, "/ws/messages")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
