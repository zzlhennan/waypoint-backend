package com.zzl.waypointbackend.along.vo;

import lombok.Data;

import java.util.List;

/**
 * GET /suggest/district 城市联想响应（城市输入框用）。
 */
@Data
public class DistrictSuggestResponse {

    private List<DistrictItem> suggestions;

    @Data
    public static class DistrictItem {
        /** 城市名（填入输入框用，如"北京市"） */
        private String name;
        /** 层级：province / city / district（辅助展示，告诉用户这是省还是市） */
        private String level;

        public DistrictItem(String name, String level) {
            this.name = name;
            this.level = level;
        }
    }
}