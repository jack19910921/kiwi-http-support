package com.vip.study.http.support.enums.error;

/**
 * Created by jack on 16/8/8.
 */
public enum HttpErrorEnum implements ErrorEnum {
    UNSUPPORTED_REQUEST_METHOD("50001", "不支持的方法"),
    RESPONSE_IS_EMPTY("50002", "响应内容为空"),
    RESPONSE_STATUS_CODE_INVALID("50003", "响应码不等于200,请求失败"),
    CLOSE_CHANNEL_ERROR("50004", "关闭链路出现异常"),
    SYSTEM_INTERNAL_ERROR("50005", "Http模块系统内部错误"),
    SC_METHOD_NOT_ALLOWED("50006", "方法不被允许");

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
