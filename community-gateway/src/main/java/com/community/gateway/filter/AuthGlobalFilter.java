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

        // ponytail: 白名单直接放行（登录/注册）
        if (WHITELIST.contains(path)) {
            return chain.filter(exchange);
        }

        // ponytail: /internal/* 是 Feign 内部调用，不需要鉴权
        if (path.startsWith("/internal/")) {
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

        // ponytail: 注入 X-Uid header，下游服务 RequestHolder/TraceFilter 可直接使用
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
        log.debug("Auth rejected [{} {}]: {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(), msg);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] body = ("{\"code\":401,\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
