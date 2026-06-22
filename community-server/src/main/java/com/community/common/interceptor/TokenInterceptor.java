package com.community.common.interceptor;

import cn.hutool.json.JSONUtil;
import com.community.common.domain.vo.response.ApiResult;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.utils.RequestHolder;
import com.community.common.domain.dto.RequestInfo;
import com.community.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

@Order(-2)
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_SCHEMA = "Bearer ";
    public static final String ATTRIBUTE_UID = "uid";

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getToken(request);
        Long uid = authService.getValidUid(token);
        if (uid != null) {
            RequestHolder.set(buildRequestInfo(request, uid));
            authService.renewalTokenIfNecessary(token);
            return true;
        }
        if (!isPublicURI(request.getRequestURI())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            try {
                PrintWriter writer = response.getWriter();
                writer.write(JSONUtil.toJsonStr(ApiResult.fail(BusinessErrorEnum.TOKEN_INVALID)));
                writer.flush();
            } catch (IOException e) {
                log.error("Failed to write 401 response", e);
            }
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestHolder.remove();
    }

    private boolean isPublicURI(String requestURI) {
        return requestURI.contains("/public/")
                || requestURI.contains("/auth/")
                || requestURI.contains("/wx/portal/")
                || requestURI.matches(".*/users/\\d+$");
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
