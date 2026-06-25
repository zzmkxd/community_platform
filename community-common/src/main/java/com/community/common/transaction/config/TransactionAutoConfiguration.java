package com.community.common.transaction.config;

import com.community.common.transaction.aspect.SecureInvokeAspect;
import com.community.common.transaction.dao.SecureInvokeRecordDao;
import com.community.common.transaction.mapper.SecureInvokeRecordMapper;
import com.community.common.transaction.service.MQProducer;
import com.community.common.transaction.service.SecureInvokeService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver") // ponytail: websocket 等无 DB 服务跳过
@MapperScan(basePackageClasses = SecureInvokeRecordMapper.class)
@Import({SecureInvokeAspect.class, SecureInvokeRecordDao.class})
public class TransactionAutoConfiguration {

    @Bean
    public SecureInvokeService secureInvokeService(
            SecureInvokeRecordDao dao,
            @Qualifier("communityExecutor") Executor executor) {
        return new SecureInvokeService(dao, executor);
    }

    @Bean
    public MQProducer mqProducer() {
        return new MQProducer();
    }
}
