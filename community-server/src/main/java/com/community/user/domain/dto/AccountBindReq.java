package com.community.user.domain.dto;

import lombok.Data;

@Data
public class AccountBindReq {

    /** WeChat OAuth2 code (Phase 2+ — exchange for openId via WeChat API) */
    private String code;

    /** Direct openId (for testing, bypasses code exchange) */
    private String openId;
}
