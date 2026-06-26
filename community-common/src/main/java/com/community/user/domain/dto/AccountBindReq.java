package com.community.user.domain.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class AccountBindReq {

    /** WeChat OAuth2 code (Phase 2+ — exchange for openId via WeChat API) */
    private String code;

    /** Direct openId (for testing, bypasses code exchange) */
    private String openId;

    @AssertTrue(message = "code 或 openId 至少提供一个")
    public boolean isValid() {
        return (code != null && !code.isBlank()) || (openId != null && !openId.isBlank());
    }
}
