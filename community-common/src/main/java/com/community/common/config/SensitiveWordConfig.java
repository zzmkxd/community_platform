package com.community.common.config;

import com.community.common.algorithm.sensitiveWord.DFAFilter;
import com.community.common.algorithm.sensitiveWord.SensitiveWordBs;
import com.community.common.sensitive.MyWordFactory;
import com.community.common.sensitive.dao.SensitiveWordDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver") // ponytail: websocket 等无 DB 服务跳过
public class SensitiveWordConfig {

    @Bean
    public MyWordFactory myWordFactory(SensitiveWordDao sensitiveWordDao) {
        return new MyWordFactory(sensitiveWordDao);
    }

    @Bean
    public SensitiveWordBs sensitiveWordBs(MyWordFactory myWordFactory) {
        return SensitiveWordBs.newInstance()
                .filterStrategy(DFAFilter.getInstance())
                .sensitiveWord(myWordFactory)
                .init();
    }
}
