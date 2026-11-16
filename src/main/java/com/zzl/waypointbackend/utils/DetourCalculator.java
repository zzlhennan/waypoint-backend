package com.zzl.waypointbackend.utils;

import com.zzl.waypointbackend.dto.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * 绕路距离计算（纯几何，无状态、不依赖高德）。
 *
 * <p>口径（PRD 第 7 节）：欧氏近似——经纬度先转成"米"再算平面距离。
 * 纬度方向 1° ≈ 111000 m；经度方向随纬度缩放，1° ≈ 111000 × cos(纬度) m。
 * 每段用自己两端的平均纬度做缩放，路线跨宽时更准。
 */
public final class DetourCalculator {

    /** 纬度方向每度对应的米数（近似常量） */
    private static final double METERS_PER_DEGREE_LAT = 111_000.0;

    /** 采样点去重的最小间距（米）：小于此视为同一点 */
    private static final double DEDUP_EPSILON_METERS = 0.01;

    private DetourCalculator() {
    }

    /**
     * 沿折线采样：起点 + 每 intervalMeters 一个插值点 + 终点（强制包含，Q10 决策）。
     * 相邻采样点间距小于 {@link #DEDUP_EPSILON_METERS} 时去重
     * （短路线不产生中间点；插值恰好落在端点时不重复）。
     *
     * @param route         折线坐标串（来自高德 polyline 解析）
     * @param intervalMeters 采样间隔（米），必须为正数（配置层 @Positive 已保证）
     * @return 采样点列表；route 为空时返回空列表
     */
    public static List<Point> sampleRoute(List<Point> route, int intervalMeters) {
        if (route == null || route.isEmpty()) {
            return List.of();
        }
        if (intervalMeters <= 0) {
            throw new IllegalArgumentException("intervalMeters 必须为正数: " + intervalMeters);
        }

        List<Point> samples = new ArrayList<>();
        addSample(samples, route.get(0));

        double nextSampleAt = intervalMeters;
        double traveled = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            Point a = route.get(i);
            Point b = route.get(i + 1);
            double segLen = distanceMeters(a, b);
            if (segLen == 0) {
                continue;
            }
            // 在当前段内，把所有"已到达的采样刻度"插值出来
            while (traveled + segLen >= nextSampleAt) {
                double t = (nextSampleAt - traveled) / segLen;
                addSample(samples, interpolate(a, b, t));
                nextSampleAt += intervalMeters;
            }
            traveled += segLen;
        }

        addSample(samples, route.get(route.size() - 1));
        return samples;
    }

    /**
     * POI 到整条折线的最近直线距离（米），即响应里的 distanceToRouteMeters。
     * 注意：这是 POI 到"整条路线"的距离，不是高德 around 返回的 POI 到采样点的 distance。
     *
     * @param poi   候选 POI 坐标
     * @param route 主路折线
     * @return 最近距离（米）；route 为空时防御性返回 0（正常流程不会走到这）
     */
    public static double minDistanceMeters(Point poi, List<Point> route) {
        if (route == null || route.isEmpty()) {
            return 0.0;
        }
        if (route.size() == 1) {
            return distanceMeters(poi, route.get(0));
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < route.size() - 1; i++) {
            min = Math.min(min, pointToSegmentMeters(poi, route.get(i), route.get(i + 1)));
        }
        return min;
    }

    /**
     * 点到线段的最短距离（米）：垂足落在线段内用投影，落在延长线上则退化为到端点。
     */
    private static double pointToSegmentMeters(Point p, Point a, Point b) {
        // 以 a 为原点、按段平均纬度缩放的平面米坐标
        double mPerLon = metersPerLonDegree(a, b);
        double px = (p.longitude() - a.longitude()) * mPerLon;
        double py = (p.latitude() - a.latitude()) * METERS_PER_DEGREE_LAT;
        double bx = (b.longitude() - a.longitude()) * mPerLon;
        double by = (b.latitude() - a.latitude()) * METERS_PER_DEGREE_LAT;

        double dx = bx;
        double dy = by;
        double segLenSq = dx * dx + dy * dy;
        if (segLenSq == 0) {
            return Math.sqrt(px * px + py * py);
        }
        double t = (px * dx + py * dy) / segLenSq;
        t = Math.max(0.0, Math.min(1.0, t));
        double cx = t * dx;
        double cy = t * dy;
        return Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
    }

    /** 两点平面距离（米），采样时沿折线累计弧长用 */
    private static double distanceMeters(Point a, Point b) {
        double mPerLon = metersPerLonDegree(a, b);
        double dx = (b.longitude() - a.longitude()) * mPerLon;
        double dy = (b.latitude() - a.latitude()) * METERS_PER_DEGREE_LAT;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** 段平均纬度对应的"每经度米数" */
    private static double metersPerLonDegree(Point a, Point b) {
        double latMidRad = Math.toRadians((a.latitude() + b.latitude()) / 2);
        return METERS_PER_DEGREE_LAT * Math.cos(latMidRad);
    }

    private static Point interpolate(Point a, Point b, double t) {
        return new Point(
                a.longitude() + t * (b.longitude() - a.longitude()),
                a.latitude() + t * (b.latitude() - a.latitude())
        );
    }

    private static void addSample(List<Point> samples, Point p) {
        if (samples.isEmpty() || distanceMeters(samples.get(samples.size() - 1), p) > DEDUP_EPSILON_METERS) {
            samples.add(p);
        }
    }
}