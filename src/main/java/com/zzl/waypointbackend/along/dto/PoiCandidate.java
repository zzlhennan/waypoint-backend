package com.zzl.waypointbackend.along.dto;

import lombok.Data;

/**
 * 内部候选 POI：从高德 around 收集、去重后，附上算好的 distanceToRouteMeters。
 * 仅在 service 内部流转，最终映射成 {@link com.zzl.waypointbackend.along.vo.PoiItem} 返回。
 */
@Data
public class PoiCandidate {

    /** 高德 POI id，用于去重 */
    private String id;
    private String name;
    private String address;
    private String location;
    private String tel;
    private String type;
    /** POI 类型编码（如 "050501"）；用于二级分类聚合（取前 4 位） */
    private String typecode;
    /** 二级分类中文（从 type.split(";")[1] 截取，如 "咖啡厅"）；前端本地 tab 过滤用 */
    private String secondaryType;
    private String adname;

    /** 由 DetourCalculator 算出：到主路折线的最近直线距离（米） */
    private double distanceToRouteMeters;
}
