package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 高德搜索 2.0 通用响应（place/text 与 place/around 共用同一结构）。
 *
 * <p>本步（定位）仅解析用到的字段；around 阶段再增量补充 address/type/business 等。
 * 未知字段一律忽略（{@code ignoreUnknown = true}），避免高德加字段导致反序列化失败。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapV5PoiResponse {

    /** 高德返回："1" 为成功，其他为错误（配合 infocode 判断） */
    private String status;
    private String info;
    private String infocode;

    private List<Poi> pois;

    /**
     * 单个 POI。本步定位只取 id/name/location；
     * around 阶段再扩 address/type/adname/business(tel) 等字段。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Poi {

        private String id;
        private String name;

        /** "经度,纬度"（如 "116.392159,40.018635"），原样保留，拆分解析在算法层做 */
        private String location;
    }
}