package com.zzl.waypointbackend.common.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一异常处理：把业务异常 / 参数校验异常转成 {code, message, detail?}。
 *
 * <p>响应体约定：
 * <ul>
 *   <li>{@code code} —— HTTP 状态码</li>
 *   <li>{@code message} —— 给用户看的友好文案（前端直接展示）</li>
 *   <li>{@code detail} —— 仅系统异常时填（Feign/兜底），业务异常不填；
 *       前端默认不展示，供开发者排查用</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：message 已是给用户看的友好文案，不加 detail */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getCode()).body(body);
    }

    /** 入参校验失败（@NotBlank/@Pattern 等）→ 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 400);
        body.put("message", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Feign 调用异常（断网/超时/高德返回非 2xx）→ 502 */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 502);
        body.put("message", "地图服务暂时不可用");
        body.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /** 兜底未知异常 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 500);
        body.put("message", "服务器内部错误");
        // detail 带异常类名 + message，方便定位是哪个 NPE/反序列化失败
        body.put("detail", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}