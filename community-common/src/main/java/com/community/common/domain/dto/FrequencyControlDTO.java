package com.community.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyControlDTO {

    /** 频控的 Key */
    private String key;

    /** 频控时间范围 */
    private Integer time;

    /** 频控时间单位 */
    private TimeUnit unit;

    /** 单位时间内最大访问次数 */
    private Integer count;
}
