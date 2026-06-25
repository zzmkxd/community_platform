package com.community.message.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

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

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

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
        doc.setCreateTime(msg.getCreateTime());
        return doc;
    }
}
