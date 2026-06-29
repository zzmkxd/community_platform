package com.community.common.config;

import com.community.common.domain.dto.RequestInfo;
import com.community.common.utils.RequestHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignUserContextInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestInfo info = RequestHolder.get();
        if (info != null && info.getUid() != null) {
            template.header("X-Uid", String.valueOf(info.getUid()));
        }
    }
}
