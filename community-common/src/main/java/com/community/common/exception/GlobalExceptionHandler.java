package com.community.common.exception;

import com.community.common.domain.vo.response.ApiResult;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ApiResponse(responseCode = "400", description = "请求参数校验失败",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ApiResult methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        StringBuilder errorMsg = new StringBuilder();
        e.getBindingResult().getFieldErrors().forEach(x ->
                errorMsg.append(x.getField()).append(x.getDefaultMessage()).append(","));
        String message = errorMsg.toString();
        log.info("validation parameters error! reason: {}", message);
        return ApiResult.fail(CommonErrorEnum.PARAM_VALID.getErrorCode(),
                message.substring(0, message.length() - 1));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BindException.class)
    public ApiResult bindException(BindException e) {
        StringBuilder errorMsg = new StringBuilder();
        e.getBindingResult().getFieldErrors().forEach(x ->
                errorMsg.append(x.getField()).append(x.getDefaultMessage()).append(","));
        String message = errorMsg.toString();
        log.info("validation parameters error! reason: {}", message);
        return ApiResult.fail(CommonErrorEnum.PARAM_VALID.getErrorCode(),
                message.substring(0, message.length() - 1));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = NullPointerException.class)
    public ApiResult nullPointerExceptionHandler(NullPointerException e) {
        log.error("null point exception! reason: ", e);
        return ApiResult.fail(CommonErrorEnum.SYSTEM_ERROR);
    }

    @ApiResponse(responseCode = "500", description = "服务器内部错误",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public ApiResult systemExceptionHandler(Exception e) {
        log.error("system exception! reason: {}", e.getMessage(), e);
        return ApiResult.fail(CommonErrorEnum.SYSTEM_ERROR);
    }

    @ApiResponse(responseCode = "400", description = "业务逻辑错误（如权限不足、数据不存在）",
            content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BusinessException.class)
    public ApiResult businessExceptionHandler(BusinessException e) {
        log.info("business exception! reason: {}", e.getMessage(), e);
        return ApiResult.fail(e.getErrorCode(), e.getMessage());
    }

    @ApiResponse(responseCode = "405", description = "不支持的 HTTP 方法")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleHttpMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.error(e.getMessage(), e);
        return ApiResult.fail(-1, "不支持'" + e.getMethod() + "'请求");
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(value = FrequencyControlException.class)
    public ApiResult frequencyControlExceptionHandler(FrequencyControlException e) {
        log.info("frequencyControl exception! reason: {}", e.getMessage(), e);
        return ApiResult.fail(e.getErrorCode(), e.getMessage());
    }
}
