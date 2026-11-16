package com.zzl.waypointbackend.feignclient;

import com.zzl.waypointbackend.feignclient.dto.AmapV5PoiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 高德开放平台声明式客户端（全 v5 2.0）。
 * base-url 来自配置；key 由 {@link AmapKeyInterceptor} 统一注入（经 {@link AmapFeignConfig}
 * 仅绑定到本 client，不作为全局拦截器），方法签名不携带 key。
 */
@FeignClient(name = "amap", url = "${amap.base-url}", configuration = AmapFeignConfig.class)
public interface AmapClient {

    /**
     * 关键字搜索 POI 2.0——定位起点/终点，取 pois[0].location。
     *
     * @param keywords 地名（如「奥森公园」）
     * @param region   城市名（v5 参数，≠v3 的 city）
     * @param cityLimit 是否严格限定城市（v5 参数，≠v3 的 citylimit）
     */
    @GetMapping("/v5/place/text")
    AmapV5PoiResponse searchText(@RequestParam("keywords") String keywords,
                                @RequestParam("region") String region,
                                @RequestParam("city_limit") Boolean cityLimit);
}