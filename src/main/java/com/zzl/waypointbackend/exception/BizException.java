package com.zzl.waypointbackend.exception;

import lombok.Getter;

/**
 * 业务异常：承载 HTTP 状态码 + 给调用方看的 message。
 * 由 GlobalExceptionHandler 统一转成 {code, message} 响应体。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    /**
     * @param code    HTTP 状态码（400 定位失败 / 404 沿途无 POI / 502 高德调用失败）
     * @param message 给调用方看的提示
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException upstream(String message) {
        return new BizException(502, message);
    }
}