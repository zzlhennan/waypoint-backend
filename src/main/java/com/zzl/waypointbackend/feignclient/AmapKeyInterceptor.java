package com.zzl.waypointbackend.feignclient;

import com.zzl.waypointbackend.config.AmapProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 给高德 Feign 请求统一拼接 {@code key} 参数，多 key 轮询使用，分散免费版 QPS 限流压力。
 *
 * <p><b>故意不标注 {@code @Component}</b>：一旦成为全局组件，就会作用于所有 FeignClient，
 * 将来新增别的 client 时会被误带高德 key。它只通过 {@link AmapFeignConfig}
 * 绑定到 {@code @FeignClient(configuration=...)} 指定的 amap client。
 *
 * <p>轮询：每个高德请求按 {@link AtomicInteger} 计数器取下一个 key（原子递增，线程安全）；
 * 只配 1 个 key 时 {@code % 1 = 0}，永远用它，自动兼容单 key 场景。
 */
public class AmapKeyInterceptor implements RequestInterceptor {

    private final List<String> keys;
    private final AtomicInteger counter = new AtomicInteger(0);

    public AmapKeyInterceptor(AmapProperties amapProperties) {
        this.keys = amapProperties.keys();
    }

    @Override
    public void apply(RequestTemplate template) {
        int idx = Math.floorMod(counter.getAndIncrement(), keys.size());
        template.query("key", keys.get(idx));
    }
}