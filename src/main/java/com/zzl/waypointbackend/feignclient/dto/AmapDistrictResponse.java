package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 高德行政区划查询 v3 响应（/v3/config/district）。
 *
 * <p>支持模糊匹配：keywords="北" 返回北京市/北海市等。
 * subdistrict=0 时 districts[] 只含当前级，不含下一级区县。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapDistrictResponse {

    private String status;
    private String info;
    private String infocode;

    private List<District> districts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class District {
        private String name;
        private String adcode;
        /** 层级：province / city / district */
        private String level;
        /** "经度,纬度"（城市中心点） */
        private String center;
    }
}