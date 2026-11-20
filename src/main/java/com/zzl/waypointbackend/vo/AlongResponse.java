package com.zzl.waypointbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * GET /along 成功响应。
 */
@Data
public class AlongResponse {

    /** 回显本次查询条件（让调用方确认搜的是什么） */
    private Query query;

    /** 按 distanceToRouteMeters 升序的 Top N POI */
    private List<PoiItem> result;

    @Data
    public static class Query {
        private String city;
        private String start;
        private String end;
        private String keyword;
        private String mode;
    }
}