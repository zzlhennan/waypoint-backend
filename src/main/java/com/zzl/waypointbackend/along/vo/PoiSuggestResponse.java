package com.zzl.waypointbackend.along.vo;

import lombok.Data;

import java.util.List;

/**
 * GET /suggest/poi 地名联想响应（起点/终点输入框用）。
 */
@Data
public class PoiSuggestResponse {

    private List<SuggestItem> suggestions;

    @Data
    public static class SuggestItem {
        /** POI 名（填入输入框用） */
        private String name;
        /** "经度,纬度" */
        private String location;
        /** 地址（辅助展示） */
        private String address;

        public SuggestItem(String name, String location, String address) {
            this.name = name;
            this.location = location;
            this.address = address;
        }
    }
}