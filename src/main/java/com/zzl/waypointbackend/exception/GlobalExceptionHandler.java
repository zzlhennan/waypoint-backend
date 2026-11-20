package com.zzl.waypointbackend.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 统一异常处理：把业务异常 / 参数校验异常转成 {code, message}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：用异常自带的 code 作为 HTTP 状态码 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        return ResponseEntity.status(ex.getCode()).body(Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage()
        ));
    }

    /** 入参校验失败（@NotBlank/@Pattern 等）→ 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", 400,
                "message", msg
        ));
    }

    /** 兜底未知异常 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "服务器内部错误"
        ));
    }
}