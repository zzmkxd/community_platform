package com.community.gateway.filter;

import com.community.gateway.JwtGatewayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

// ponytail: 一个 GlobalFilter 搞定鉴权+uid header 注入。不需要单独 Filter 类。
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_SCHEMA = "Bearer ";
    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    private final JwtGatewayUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String normalized = normalizePath(path);

        if (WHITELIST.contains(normalized)) {
            return chain.filter(exchange);
        }

        if (normalized.startsWith("/v3/api-docs")
                || normalized.startsWith("/swagger-ui")
                || normalized.startsWith("/webjars")) {
            return chain.filter(exchange);
        }

        if (normalized.startsWith("/internal/")) {
            if (!isInternalRequest(exchange)) {
                return forbidden(exchange, "Internal endpoints not accessible externally");
            }
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null) {
            return unauthorized(exchange, "Missing Authorization header");
        }

        Long uid = jwtUtil.getUidOrNull(token);
        if (uid == null) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-Uid", String.valueOf(uid))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String extractToken(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(AUTHORIZATION_SCHEMA)) {
            return auth.substring(AUTHORIZATION_SCHEMA.length());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        log.warn("Auth rejected [{} {}]: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(), msg);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] body = ("{\"code\":401,\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String msg) {
        log.warn("Forbidden [{} {}]: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(), msg);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        byte[] body = ("{\"code\":403,\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String normalizePath(String path) {
        return path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
    }

    private boolean isInternalRequest(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null) {
            return false;
        }
        String host = exchange.getRequest().getRemoteAddress().getHostString();
        if (host.startsWith("10.") || host.startsWith("127.") || host.startsWith("192.168.")
                || "0:0:0:0:0:0:0:1".equals(host) || "::1".equals(host)) {
            return true;
        }
        if (host.startsWith("172.")) {
            int dot = host.indexOf('.', 4);
            if (dot > 0) {
                try {
                    int second = Integer.parseInt(host.substring(4, dot));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
