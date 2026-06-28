package com.community.common.transaction.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.community.common.transaction.domain.dto.SecureInvokeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "secure_invoke_record", autoResultMap = true)
public class SecureInvokeRecord {
    public static final byte STATUS_WAIT = 1;
    public static final byte STATUS_FAIL = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "secure_invoke_json", typeHandler = JacksonTypeHandler.class)
    private SecureInvokeDTO secureInvokeDTO;

    @TableField("status")
    @Builder.Default
    private byte status = STATUS_WAIT;

    @TableField("next_retry_time")
    @Builder.Default
    private LocalDateTime nextRetryTime = LocalDateTime.now();

    @TableField("retry_times")
    @Builder.Default
    private Integer retryTimes = 0;

    @TableField("max_retry_times")
    private Integer maxRetryTimes;

    @TableField("fail_reason")
    private String failReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
