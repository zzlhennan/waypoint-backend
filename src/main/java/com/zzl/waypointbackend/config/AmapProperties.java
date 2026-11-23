package com.zzl.waypointbackend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 高德开放平台配置（prefix: amap）。
 *
 * <p>真实 key 只来自 application-local.yml（git 忽略）或环境变量；
 * keys 列表为空 / 元素全空白时，由校验注解在启动期 fail-fast。
 * 多个 key 用于轮询，分散免费版 QPS 限流压力。
 */
@ConfigurationProperties("amap")
@Validated
public record AmapProperties(
        @NotEmpty(message = "amap.keys 至少配置一个 key")
        List<@NotBlank(message = "amap.key 元素不能为空白") String> keys,
        @NotBlank String baseUrl
) {
}