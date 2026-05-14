package com.guitu.security;

import com.guitu.domain.enums.UserStatus;
import com.guitu.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE = "ws_user_id";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public WebSocketAuthHandshakeInterceptor(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        return tokenService.parseUserId(token)
                .flatMap(userRepository::findById)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .map(user -> {
                    attributes.put(USER_ID_ATTRIBUTE, user.getId());
                    return true;
                })
                .orElseGet(() -> {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                });
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
