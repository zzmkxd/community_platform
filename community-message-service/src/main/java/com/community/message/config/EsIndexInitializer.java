package com.community.message.config;

import com.community.message.domain.document.MessageDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

// 启动时按 @Document 注解建索引与映射，确保 ik_max_word 分词器、字段类型真正生效。
// 否则裸 esOps.save 会让 ES 用动态映射建索引，@Field 注解被全部忽略。
//
// 特别注意：旧版本可能已通过动态映射创建了索引，该索引缺少：
//   - content 字段的 ik_max_word 分词器（中文分词不生效）
//   - createTime 字段的 format 约束（空格分隔被拒绝 → document_parsing_exception）
// 如果检测到这种情况，删除旧索引并重建。
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer {

    private final ElasticsearchOperations esOps;

    @PostConstruct
    public void ensureIndex() {
        try {
            IndexOperations indexOps = esOps.indexOps(MessageDocument.class);

            if (!indexOps.exists()) {
                indexOps.createWithMapping();
                log.info("ES index [messages] created with mapping from annotations (ik_max_word analyzer)");
                return;
            }

            // 索引已存在 — 检查 mapping 是否由 @Field 注解驱动
            boolean needRebuild = detectBadMapping(indexOps);
            long count = esOps.count(new CriteriaQuery(new Criteria()), MessageDocument.class);

            if (needRebuild) {
                log.warn("ES index [messages] has dynamic mapping (created by old code) — " +
                        "DROPPING and RECREATING to apply proper mapping. " +
                        "Current document count: {} (will be lost, use POST /api/v1/servers/{{serverId}}/search/reindex to rebuild)", count);
                indexOps.delete();
                indexOps.createWithMapping();
                log.info("ES index [messages] recreated with correct mapping (ik_max_word analyzer + ISO date format). " +
                        "Old document count was: {} — call POST /api/v1/servers/{{serverId}}/search/reindex to restore.", count);
            } else {
                log.info("ES connected — index [messages] OK (correct mapping), document count: {}", count);
            }
        } catch (Exception e) {
            log.error("ES INIT FAILED — search will NOT work! " +
                    "Check that Elasticsearch is running and spring.elasticsearch.uris is correct. Error: {}",
                    e.toString());
        }
    }

    /**
     * 检测索引是否为动态映射（缺少 ik_max_word 分词器 = 由 esOps.save 动态创建，非 createWithMapping 创建）。
     * 动态映射会导致：
     * 1. content 字段没有 ik_max_word 分词器 → 中文分词不生效
     * 2. createTime 没有 format 约束 → 可能按 strict_date_optional_time 解析，拒绝空格分隔
     */
    @SuppressWarnings("unchecked")
    private boolean detectBadMapping(IndexOperations indexOps) {
        try {
            Map<String, Object> mapping = indexOps.getMapping();
            // 导航到 content 字段的属性
            Object propsObj = Optional.ofNullable(mapping.get("properties"))
                    .orElse(Map.of());
            if (!(propsObj instanceof Map<?, ?> props)) {
                return true; // 无法解析 mapping → 安全起见重建
            }
            Object contentObj = props.get("content");
            if (!(contentObj instanceof Map<?, ?> contentField)) {
                return true; // 缺少 content 字段 → 重建
            }
            // 检查是否配置了 analyzer
            Object analyzer = contentField.get("analyzer");
            if (!"ik_max_word".equals(analyzer)) {
                log.warn("Detected bad mapping: content.analyzer={} (expected ik_max_word)", analyzer);
                return true;
            }
            return false; // mapping OK
        } catch (Exception e) {
            log.warn("Failed to inspect ES mapping: {} — will NOT rebuild index", e.getMessage());
            return false; // 保守处理：不要因为检测失败就删除索引
        }
    }
}
