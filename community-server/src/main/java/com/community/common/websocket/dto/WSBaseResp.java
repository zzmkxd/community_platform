package com.community.common.websocket.dto;

import lombok.Data;

@Data
public class WSBaseResp {

    /** WSRespTypeEnum 中的 type */
    private Integer type;

    /** 推送体 JSON */
    private Object data;

    public static WSBaseResp of(Integer type, Object data) {
        WSBaseResp resp = new WSBaseResp();
        resp.setType(type);
        resp.setData(data);
        return resp;
    }
}
