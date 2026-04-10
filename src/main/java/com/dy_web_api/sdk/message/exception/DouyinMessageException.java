package com.dy_web_api.sdk.message.exception;

import lombok.Getter;

/**
 * 抖音消息SDK异常类
 */
@Getter
public class DouyinMessageException extends RuntimeException {

    private final ErrorCode errorCode;

    public DouyinMessageException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DouyinMessageException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public DouyinMessageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public DouyinMessageException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    // 保持向后兼容性的构造函数
    public DouyinMessageException(String message) {
        super(message);
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }

    public DouyinMessageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }

    public DouyinMessageException(Throwable cause) {
        super(cause);
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }

    public String getErrorCode() {
        return errorCode.getCode();
    }
}