package com.zzl.waypointbackend.utils;

import com.zzl.waypointbackend.dto.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * DetourCalculator 单测：纯几何，不联网、不依赖 Spring。
 * 坐标取整是为了手算预期值（lat≈39.9° 处 m/经度≈85148，m/纬度=111000）。
 */
class DetourCalculatorTest {

    /** lat=39.9° 处每经度对应的米数：111000 × cos(39.9°) ≈ 85148 */
    private static final double M_PER_LON_399 = 85_148;

    @Test
    void pointToSegment_footInside_returnsPerpendicularDistance() {
        // 线段沿 39.9° 纬线从 0.0°E 到 0.1°E；POI 在线段中点正北 0.01°（=1110m）
        Point a = new Point(0.0, 39.9);
        Point b = new Point(0.1, 39.9);
        Point poi = new Point(0.05, 39.91);

        double d = DetourCalculator.minDistanceMeters(poi, List.of(a, b));

        assertThat(d).isCloseTo(1110.0, within(2.0));
    }

    @Test
    void pointToSegment_footBeyondEndpoint_degradesToEndpointDistance() {
        // POI 在 b 东侧 0.2°，垂足超出线段 → 退化为到 b 的距离：0.2 × 85148 ≈ 17030m
        Point a = new Point(0.0, 39.9);
        Point b = new Point(0.1, 39.9);
        Point poi = new Point(0.3, 39.9);

        double d = DetourCalculator.minDistanceMeters(poi, List.of(a, b));

        assertThat(d).isCloseTo(0.2 * M_PER_LON_399, within(5.0));
    }

    @Test
    void minDistance_overLShapedRoute_picksNearerSegment() {
        // L 形路线：A→B 向东，B→C 向北；POI 紧贴 BC 边（距 BC ≈ 45m，距 AB 端点 B ≈ 1111m）
        Point a = new Point(0.0, 39.9);
        Point b = new Point(0.1, 39.9);
        Point c = new Point(0.1, 40.0);
        Point poi = new Point(0.101, 39.91);

        double d = DetourCalculator.minDistanceMeters(poi, List.of(a, b, c));

        assertThat(d).isCloseTo(0.001 * M_PER_LON_399, within(2.0));
    }

    @Test
    void sampling_shortRouteUnderInterval_onlyStartAndEnd() {
        // 全程 ≈ 0.004° × 85148 ≈ 341m < 500m 间隔 → 只有起点 + 终点
        Point a = new Point(0.0, 39.9);
        Point b = new Point(0.004, 39.9);

        List<Point> samples = DetourCalculator.sampleRoute(List.of(a, b), 500);

        assertThat(samples).hasSize(2);
        assertThat(samples.get(0)).isEqualTo(a);
        assertThat(samples.get(1)).isEqualTo(b);
    }

    @Test
    void sampling_routeLongerThanInterval_includesStartInterpolatedMidAndEnd() {
        // 全程 ≈ 0.014° × 85148 ≈ 1192m，间隔 500m → 起点 + 500m 点 + 1000m 点 + 终点 = 4 个
        Point a = new Point(0.0, 39.9);
        Point b = new Point(0.014, 39.9);

        List<Point> samples = DetourCalculator.sampleRoute(List.of(a, b), 500);

        assertThat(samples).hasSize(4);
        assertThat(samples.get(0)).isEqualTo(a);
        assertThat(samples.get(3)).isEqualTo(b);
    }

    @Test
    void sampling_zeroLengthRoute_dedupesToSinglePoint() {
        List<Point> samples = DetourCalculator.sampleRoute(
                List.of(new Point(0.0, 39.9), new Point(0.0, 39.9)), 500);

        assertThat(samples).hasSize(1);
    }

    @Test
    void conversion_oneDegreeLatitude_equals111000Meters() {
        // 纵向 1° 纬度 = 111000m（验证纬度换算不含 cos）
        double d = DetourCalculator.minDistanceMeters(
                new Point(0.0, 39.9), List.of(new Point(0.0, 40.9)));

        assertThat(d).isCloseTo(111_000.0, within(100.0));
    }

    @Test
    void emptyRoute_returnsSafeDefaults() {
        assertThat(DetourCalculator.sampleRoute(List.of(), 500)).isEmpty();
        assertThat(DetourCalculator.minDistanceMeters(new Point(0, 0), List.of())).isEqualTo(0.0);
    }
}