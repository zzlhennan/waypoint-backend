package com.zzl.waypointbackend.along.vo;

import lombok.Data;

import java.util.List;

/**
 * GET /along/search 成功响应。
 */
@Data
public class AlongResponse {

    /** 回显本次查询条件（让调用方确认搜的是什么） */
    private Query query;

    /** 按 distanceToRouteMeters 升序的 Top N POI（受 topN 限制） */
    private List<PoiItem> result;

    /**
     * 当前结果的二级分类统计（按 count 降序）。
     *
     * <p>返回规则：本次请求<b>不带 category</b>（keyword 主搜 / 点「全部」tab）时返回数组；
     * 本次请求<b>带 category</b>（点了具体分类 tab）时返回 null，前端据此不刷新 tab 缓存，
     * 避免 tab 被"过滤后的小集合"冲掉。
     */
    private List<CategoryCount> categories;

    @Data
    public static class Query {
        private String city;
        private String start;
        private String end;
        private String keyword;
        private String mode;
    }

    /**
     * 二级分类统计项：label=分类名（如「咖啡厅」），count=硬过滤后该分类的 POI 数量。
     */
    @Data
    public static class CategoryCount {
        private String label;
        private int count;

        public CategoryCount(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }
}