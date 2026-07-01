package com.community.message.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Document(indexName = "messages")
public class MessageDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long channelId;

    // ponytail: looked up from ChannelService during indexing, not on Message entity
    @Field(type = FieldType.Long)
    private Long serverId;

    @Field(type = FieldType.Long)
    private Long threadId;

    @Field(type = FieldType.Long)
    private Long fromUid;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Integer)
    private Integer msgType;

    @Field(type = FieldType.Integer)
    private Integer status;

    // ponytail: String over LocalDateTime — ElasticsearchDateConverter writes date-only when
    // Jackson lacks JavaTimeModule; manual format bypasses the converter chain entirely.
    // MUST use ISO 8601 format (T separator) — ES strict_date_optional_time rejects space-separated dates
    // which causes document_parsing_exception and silently drops messages from the index.
    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private String createTime;

    private static final DateTimeFormatter ES_DTF = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    public static MessageDocument from(com.community.message.domain.entity.Message msg, Long serverId) {
        MessageDocument doc = new MessageDocument();
        doc.setId(msg.getId());
        doc.setChannelId(msg.getChannelId());
        doc.setServerId(serverId);
        doc.setThreadId(msg.getThreadId());
        doc.setFromUid(msg.getFromUid());
        doc.setContent(msg.getContent());
        doc.setMsgType(msg.getMsgType());
        doc.setStatus(msg.getStatus());
        if (msg.getCreateTime() != null) {
            doc.setCreateTime(msg.getCreateTime().format(ES_DTF));
        }
        return doc;
    }
}
