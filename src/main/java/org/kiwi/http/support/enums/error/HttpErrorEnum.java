package org.kiwi.http.support.enums.error;

/**
 * Created by jack on 16/8/8.
 */
public enum HttpErrorEnum implements ErrorEnum {
    UNSUPPORTED_REQUEST_METHOD("50001", "不支持的方法"),
    RESPONSE_IS_EMPTY("50002", "响应内容为空"),
    RESPONSE_FAILURE("50003", "响应失败"),
    SYSTEM_INTERNAL_ERROR("50004", "Http模块内部错误"),
    CONNECTION_REFUSED("50005", "连接超时"),
    UNKNOWN_HOST("50006", "DNS域名解析异常");

    private String errorCode;
    private String errorMessage;

    HttpErrorEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
