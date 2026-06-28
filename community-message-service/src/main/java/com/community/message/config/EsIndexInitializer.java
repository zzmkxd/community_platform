package com.community.message.config;

import com.community.message.domain.document.MessageDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

// 启动时按 @Document 注解建索引与映射，确保 ik_max_word 分词器、字段类型真正生效。
// 否则裸 esOps.save 会让 ES 用动态映射建索引，@Field 注解被全部忽略。
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer {

    private final ElasticsearchOperations esOps;

    @PostConstruct
    public void ensureIndex() {
        IndexOperations indexOps = esOps.indexOps(MessageDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("ES index [messages] created with mapping from annotations");
        }
    }
}
