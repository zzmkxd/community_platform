package com.community.common.websocket.dto;

import lombok.Data;

@Data
public class WSBaseResp<T> {

    /** WSRespTypeEnum 中的 type */
    private Integer type;

    /** 推送体 JSON */
    private T data;

    public static <T> WSBaseResp<T> of(Integer type, T data) {
        WSBaseResp<T> resp = new WSBaseResp<>();
        resp.setType(type);
        resp.setData(data);
        return resp;
    }
}
