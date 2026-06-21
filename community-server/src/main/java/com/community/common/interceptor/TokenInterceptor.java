package com.community.common.interceptor;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.JwtUtils;
import com.community.common.utils.RequestHolder;
import com.community.common.domain.dto.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Order(-2)
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_SCHEMA = "Bearer ";
    public static final String ATTRIBUTE_UID = "uid";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = getToken(request);
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid != null) {
            RequestHolder.set(buildRequestInfo(request, uid));
            return true;
        }
        if (!isPublicURI(request.getRequestURI())) {
            throw new BusinessException(BusinessErrorEnum.TOKEN_INVALID);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestHolder.remove();
    }

    private boolean isPublicURI(String requestURI) {
        String[] split = requestURI.split("/");
        return split.length > 2 && "public".equals(split[3]);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        return Optional.ofNullable(header)
                .filter(h -> h.startsWith(AUTHORIZATION_SCHEMA))
                .map(h -> h.substring(AUTHORIZATION_SCHEMA.length()))
                .orElse(null);
    }

    private RequestInfo buildRequestInfo(HttpServletRequest request, Long uid) {
        RequestInfo info = new RequestInfo();
        info.setUid(uid);
        info.setIp(request.getRemoteAddr());
        return info;
    }
}
