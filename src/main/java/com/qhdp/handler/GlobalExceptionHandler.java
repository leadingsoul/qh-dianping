package com.qhdp.handler;

import com.qhdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //全局异常处理
    @ExceptionHandler(Exception.class)
    public Result<Void> exception(Exception e) {
        e.printStackTrace();
        //记录异常日志
        log.error("服务器发生运行时异常！异常信息为：{}",e.getMessage());
        //返回对应的提示
        return Result.error(e.getMessage());
    }
}
