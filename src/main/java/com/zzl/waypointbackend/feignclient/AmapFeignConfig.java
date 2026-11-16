package com.zzl.waypointbackend.feignclient;

import com.zzl.waypointbackend.config.AmapProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * amap client 的专属 Feign 配置（per-client configuration）。
 *
 * <p><b>故意不加 {@code @Configuration}</b>：否则本类位于组件扫描路径内，
 * 会被主应用上下文注册，下面这个 {@code RequestInterceptor} Bean 就会变成全局的，
 * 从而作用到所有 FeignClient。不标 {@code @Configuration} 时，它只通过
 * {@code @FeignClient(configuration = AmapFeignConfig.class)} 注册进 amap 的子上下文，
 * 做到「key 只跟随高德请求」。
 *
 * <p>{@link AmapProperties} 由主上下文提供，Feign 子上下文以主上下文为 parent，可直接注入。
 */
public class AmapFeignConfig {

    @Bean
    public RequestInterceptor amapKeyInterceptor(AmapProperties amapProperties) {
        return new AmapKeyInterceptor(amapProperties);
    }
}