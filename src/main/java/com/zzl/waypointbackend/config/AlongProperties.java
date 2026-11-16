package com.zzl.waypointbackend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 沿途业务参数（prefix: along）。
 *
 * <p>默认值统一在 application.yml 维护，不硬编码到代码；各项必须为正数，
 * 误配成 0 或负数时由 {@link Positive} 在启动期 fail-fast。
 */
@ConfigurationProperties("along")
@Validated
public record AlongProperties(
        @Positive int sampleIntervalMeters,
        @Positive int searchRadiusMeters,
        @Positive int searchRadiusRetryMeters,
        @Positive int maxDetourMeters,
        @Positive int topN
) {
}