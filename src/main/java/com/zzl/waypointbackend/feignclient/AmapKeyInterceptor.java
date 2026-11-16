package com.zzl.waypointbackend.feignclient;

import com.zzl.waypointbackend.config.AmapProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * 给高德 Feign 请求统一拼接 {@code key} 参数。
 *
 * <p><b>故意不标注 {@code @Component}</b>：一旦成为全局组件，就会作用于所有 FeignClient，
 * 将来新增别的 client 时会被误带高德 key。它只通过 {@link AmapFeignConfig}
 * 绑定到 {@code @FeignClient(configuration=...)} 指定的 amap client。
 */
public class AmapKeyInterceptor implements RequestInterceptor {

    private final AmapProperties amapProperties;

    public AmapKeyInterceptor(AmapProperties amapProperties) {
        this.amapProperties = amapProperties;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.query("key", amapProperties.key());
    }
}