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

// ponytail: 双重提取策略：
//   1. Gateway 已鉴权 → X-Uid header 直接信任，零开销
//   2. 直连服务（调试）→ Authorization Bearer token JWT 解析
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class TokenFilter implements Filter {

    private static final String HEADER_X_UID = "X-Uid";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_SCHEMA = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Long uid = extractUid(httpRequest);
        if (uid != null) {
            RequestInfo info = new RequestInfo();
            info.setUid(uid);
            info.setIp(httpRequest.getRemoteAddr());
            RequestHolder.set(info);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            RequestHolder.remove();
        }
    }

    private Long extractUid(HttpServletRequest request) {
        // ponytail: Gateway 已鉴权 → 直接用 X-Uid
        String xUid = request.getHeader(HEADER_X_UID);
        if (xUid != null) {
            try { return Long.parseLong(xUid); } catch (NumberFormatException e) { /* fall through */ }
        }
        // ponytail: 直连模式 → JWT 解析
        return Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER))
                .filter(h -> h.startsWith(AUTHORIZATION_SCHEMA))
                .map(h -> jwtUtils.getUidOrNull(h.substring(AUTHORIZATION_SCHEMA.length())))
                .orElse(null);
    }
}
