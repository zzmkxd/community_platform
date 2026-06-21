package com.community.common.websocket.dto;

import lombok.Data;

@Data
public class WSBaseReq {

    /** WSReqTypeEnum 中的 type */
    private Integer type;

    /** 请求体 JSON，由具体 handler 反序列化 */
    private String data;
}
