package com.shadowledger.api_gateway.filter;

import com.shadowledger.api_gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication
        if (path.equals("/auth/token") || path.startsWith("/actuator")) {
            return addTraceId(exchange, chain);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Missing Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorizedResponse(exchange, "Missing or Invalid Authorization Header");
        }

        String token = authHeader.substring(7);
        try {
            // Validate JWT token
            Claims claims = jwtService.validateToken(token);
            String username = claims.getSubject();
            String role = claims.get("Role", String.class);

            if (jwtService.isTokenExpired(token)) {
                return sendUnauthorizedResponse(exchange, "Token Expired");
            }
            if (!hasPermission(path, role)) {
                return sendForbiddenEvent(exchange, "Insufficient Permissions for Role: " + role);
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Trace-Id", UUID.randomUUID().toString())
                    .header("X-User", username)
                    .header("X-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return sendUnauthorizedResponse(exchange, "Invalid JWT Token: " + e.getMessage());
        }
    }

    private boolean hasPermission(String path, String role) {
        if (role == null) return false;
        if (path.startsWith("/events")) {
            return "USER".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/drift-check")) {
            return "AUDITOR".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/correct")) {
            return "ADMIN".equals(role);
        }
        return false;
    }

    private Mono<Void> addTraceId(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Trace-Id", UUID.randomUUID().toString())
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"status\":401}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> sendForbiddenEvent(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"Forbidden\",\"message\":\"" + message + "\",\"status\":403}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
