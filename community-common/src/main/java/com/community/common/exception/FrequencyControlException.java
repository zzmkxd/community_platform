package com.community.common.exception;

import lombok.Data;

@Data
public class FrequencyControlException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected Integer errorCode;
    protected String errorMsg;

    public FrequencyControlException(Integer errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
