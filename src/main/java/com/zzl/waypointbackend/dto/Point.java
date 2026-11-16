package com.zzl.waypointbackend.dto;

/**
 * 经纬度坐标点（算法内部对象）。经度在前、纬度在后，与高德 polyline/location 顺序一致。
 *
 * @param longitude 经度
 * @param latitude  纬度
 */
public record Point(double longitude, double latitude) {
}