package com.vote.exception;

import com.vote.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 静态资源（如 /uploads/**）找不到时，Spring 6 会抛 NoResourceFoundException。
     * 以前会被下面的 catch-all 吞成 HTTP 200 + JSON，导致 <img> 拿到的不是图片却不报错、
     * 排查时根本看不出是 404。这里显式还原成真正的 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统内部错误：" + e.getMessage());
    }
}