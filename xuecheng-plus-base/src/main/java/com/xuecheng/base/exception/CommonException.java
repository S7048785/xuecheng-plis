package com.xuecheng.base.exception;

public class CommonException extends RuntimeException{

    private int errCode;
    private String errMessage;
    public CommonException() {
    }

    public CommonException(String message) {
        super(message);
        this.errCode = -1;
        this.errMessage = message;
    }

    public CommonException(int errCode, String message) {
        super(message);
        this.errCode = errCode;
        this.errMessage = message;
    }

    public int getErrCode() {
        return errCode;
    }
    public String getErrMessage() {
        return errMessage;
    }

    public static void cast(CommonException ex) {
        throw new CommonException(ex.getErrMessage());
    }

    public static void cast(String errMessage) {
        throw new CommonException(errMessage);
    }
}
