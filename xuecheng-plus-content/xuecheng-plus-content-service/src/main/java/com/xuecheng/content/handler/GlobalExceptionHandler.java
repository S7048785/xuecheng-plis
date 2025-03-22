package com.xuecheng.content.handler;

import com.xuecheng.base.exception.CommonException;
import com.xuecheng.base.exception.RestErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler{

    @ExceptionHandler
    public RestErrorResponse handleException(Exception e){
        log.error("全局异常信息{}", e.getMessage());
        return new RestErrorResponse(e.getMessage());
    }

    @ExceptionHandler
    public RestErrorResponse handleCommonException(CommonException e) {
        log.error("通用异常信息{}", e.getErrMessage());
        return new RestErrorResponse(e.getErrCode(), e.getErrMessage());
    }

    @ExceptionHandler
    public RestErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("参数校验失败：{}", errMessage);
        return new RestErrorResponse(errMessage);
    }
}
