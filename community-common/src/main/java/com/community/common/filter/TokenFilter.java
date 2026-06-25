package com.community.common.filter;

import com.community.common.domain.dto.RequestInfo;
import com.community.common.utils.JwtUtils;
import com.community.common.utils.RequestHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

// ponytail: JWT-only token extraction, no Redis check (Gateway will do full validation).
// 所有微服务扫到 com.community.common 即自动启用，无需每个服务各自配置。
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class TokenFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_SCHEMA = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = extractToken(httpRequest);
        if (token != null) {
            Long uid = jwtUtils.getUidOrNull(token);
            if (uid != null) {
                RequestInfo info = new RequestInfo();
                info.setUid(uid);
                info.setIp(httpRequest.getRemoteAddr());
                RequestHolder.set(info);
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            RequestHolder.remove();
        }
    }

    private String extractToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER))
                .filter(h -> h.startsWith(AUTHORIZATION_SCHEMA))
                .map(h -> h.substring(AUTHORIZATION_SCHEMA.length()))
                .orElse(null);
    }
}
