package com.zzl.waypointbackend.along.vo;

import lombok.Data;

/**
 * 返回给调用方的单个 POI（与 03 接口文档 7.3 响应示例字段一致）。
 */
@Data
public class PoiItem {

    private String name;
    private String address;
    private double distanceToRouteMeters;
    private String location;
    private String tel;
    private String type;
    private String adname;
}
