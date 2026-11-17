package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 高德搜索 2.0 通用响应（place/text 与 place/around 共用同一结构）。
 *
 * <p>未知字段一律忽略（{@code ignoreUnknown = true}），避免高德加字段导致反序列化失败。
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
     * 单个 POI。定位用到 id/name/location；around 再用到 address/type/adname/business(tel)。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Poi {

        private String id;
        private String name;

        /** "经度,纬度"（如 "116.392159,40.018635"），原样保留，拆分解析在算法层做 */
        private String location;

        private String address;
        private String type;
        private String adname;

        /** 需 show_fields=business 才返回；tel 在嵌套 business 对象里 */
        private Business business;

        /** POI 业务信息子对象（show_fields=business 时才有） */
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Business {
            private String tel;
        }
    }
}