package com.zzl.waypointbackend.feignclient;

import com.zzl.waypointbackend.feignclient.dto.AmapDistrictResponse;
import com.zzl.waypointbackend.feignclient.dto.AmapInputtipsResponse;
import com.zzl.waypointbackend.feignclient.dto.AmapV5PoiResponse;
import com.zzl.waypointbackend.feignclient.dto.AmapV5RouteResponse;
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
     * @param keywords  地名（如「奥森公园」）
     * @param region    城市名（v5 参数，≠v3 的 city）
     * @param cityLimit 是否严格限定城市（v5 参数，≠v3 的 citylimit）
     */
    @GetMapping("/v5/place/text")
    AmapV5PoiResponse searchText(@RequestParam("keywords") String keywords,
                                 @RequestParam("region") String region,
                                 @RequestParam("city_limit") Boolean cityLimit);

    /**
     * 输入提示 v3（地名联想用）。datatype=poi 返回 POI 列表。
     * 与 place/text 区别：天生为联想设计，返回更短更准。
     */
    @GetMapping("/v3/assistant/inputtips")
    AmapInputtipsResponse inputtips(@RequestParam("keywords") String keywords,
                                     @RequestParam("datatype") String datatype,
                                     @RequestParam(value = "region", required = false) String region);

    /**
     * 行政区划查询 v3（城市联想用）。支持模糊匹配（输"北"返回北京市/北海市）。
     * subdistrict=0 只返回当前级，不返回下一级区县。
     */
    @GetMapping("/v3/config/district")
    AmapDistrictResponse district(@RequestParam("keywords") String keywords,
                                  @RequestParam(value = "subdistrict", defaultValue = "0") String subdistrict);

    /**
     * 驾车路线规划 2.0。必须传 showFields="polyline" 才返回坐标串。
     *
     * @param strategy 驾车策略（可选，null 时高德用默认）；MVP 暂不使用，留第二步
     */
    @GetMapping("/v5/direction/driving")
    AmapV5RouteResponse driving(@RequestParam("origin") String origin,
                                @RequestParam("destination") String destination,
                                @RequestParam(value = "strategy", required = false) Integer strategy,
                                @RequestParam("show_fields") String showFields);

    /** 步行路线规划 2.0。showFields="polyline"。 */
    @GetMapping("/v5/direction/walking")
    AmapV5RouteResponse walking(@RequestParam("origin") String origin,
                               @RequestParam("destination") String destination,
                               @RequestParam("show_fields") String showFields);

    /** 骑行路线规划 2.0。showFields="polyline"。 */
    @GetMapping("/v5/direction/bicycling")
    AmapV5RouteResponse bicycling(@RequestParam("origin") String origin,
                                 @RequestParam("destination") String destination,
                                 @RequestParam("show_fields") String showFields);

    /**
     * POI 周边搜索 2.0（采样点搜 POI）。
     *
     * @param location  "经度,纬度"
     * @param keywords  关键词（如「咖啡」）
     * @param radius    搜索半径（米）
     * @param types     POI 类型编码（可选，MVP 不用）
     * @param showFields 传 "business" 以获取 poi.business.tel
     */
    @GetMapping("/v5/place/around")
    AmapV5PoiResponse around(@RequestParam("location") String location,
                             @RequestParam("keywords") String keywords,
                             @RequestParam("radius") Integer radius,
                             @RequestParam(value = "types", required = false) String types,
                             @RequestParam("show_fields") String showFields);
}