package com.xuecheng.base.exception;

import java.io.Serializable;

/**
 * 错误响应参数包装
 */
public class RestErrorResponse implements Serializable {

    private int errCode;
    private String errMessage;
    public RestErrorResponse(String errMessage){
        this.errCode = -1;
        this.errMessage= errMessage;
    }
    public RestErrorResponse(int errCode, String errMessage){
        this.errCode = errCode;
        this.errMessage= errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}