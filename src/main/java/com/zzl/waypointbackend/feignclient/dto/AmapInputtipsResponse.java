package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 高德输入提示 v3 响应（/v3/assistant/inputtips）。
 *
 * <p>与 v5 响应结构不同：成功标识是 {@code status="1"}，结果在 {@code tips[]} 而非 {@code pois[]}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapInputtipsResponse {

    private String status;
    private String info;
    private String infocode;

    private List<Tip> tips;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tip {
        private String name;
        /** "经度,纬度"（行政区联想时可能为空） */
        private String location;
        private String address;
        private String adcode;
        private String district;
        private String type;
    }
}