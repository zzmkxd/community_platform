package com.community.common.config;

import com.community.common.algorithm.sensitiveWord.DFAFilter;
import com.community.common.algorithm.sensitiveWord.SensitiveWordBs;
import com.community.common.sensitive.MyWordFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SensitiveWordConfig {

    private final MyWordFactory myWordFactory;

    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance()
                .filterStrategy(DFAFilter.getInstance())
                .sensitiveWord(myWordFactory)
                .init();
    }
}
