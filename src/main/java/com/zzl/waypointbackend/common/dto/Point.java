package com.zzl.waypointbackend.common.dto;

/**
 * 经纬度坐标点（横切通用数据载体）。经度在前、纬度在后，与高德 polyline/location 顺序一致。
 *
 * <p>不专属于某个业务域：feignclient（解析高德 polyline）、DetourCalculator（几何算法）、
 * along（业务编排）都用它，是各层之间传坐标的通用类型。
 *
 * @param longitude 经度
 * @param latitude  纬度
 */
public record Point(double longitude, double latitude) {
}
