package com.zzl.waypointbackend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 高德开放平台配置（prefix: amap）。
 *
 * <p>真实 key 只来自 application-local.yml（git 忽略）或环境变量 AMAP_KEY；
 * key / baseUrl 为空白时，由 {@link NotBlank} 在启动期 fail-fast，避免带着空 key 调到高德才报错。
 */
@ConfigurationProperties("amap")
@Validated
public record AmapProperties(
        @NotBlank String key,
        @NotBlank String baseUrl
) {
}