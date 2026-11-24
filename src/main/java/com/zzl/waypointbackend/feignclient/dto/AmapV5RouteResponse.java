package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zzl.waypointbackend.common.dto.Point;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 高德路径规划 2.0 响应（driving/walking/bicycling 共用）。
 *
 * <p>必须请求时传 {@code show_fields=polyline}，否则 steps 无坐标串。
 * 自带 {@link #getRoutePoints()} 把 paths[0].steps[].polyline 解析成 {@link Point} 列表。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapV5RouteResponse {

    private String status;
    private String info;
    private String infocode;

    private Route route;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        /** 默认返回 3 条备选路线，我们只用 paths[0] */
        private List<Path> paths;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        private List<Step> steps;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        /** "经度,纬度;经度,纬度;..." 坐标串 */
        private String polyline;
    }

    /**
     * 取 paths[0] 的全部 step polyline，拼成连续坐标点列表。
     * 无有效路线时返回空列表（由 service 判空抛 502）。
     */
    public List<Point> getRoutePoints() {
        if (route == null || route.getPaths() == null || route.getPaths().isEmpty()) {
            return List.of();
        }
        Path first = route.getPaths().get(0);
        if (first.getSteps() == null) {
            return List.of();
        }
        List<Point> points = new ArrayList<>();
        for (Step step : first.getSteps()) {
            String polyline = step.getPolyline();
            if (polyline == null || polyline.isBlank()) {
                continue;
            }
            for (String token : polyline.split(";")) {
                String[] xy = token.split(",");
                if (xy.length == 2) {
                    points.add(new Point(
                            Double.parseDouble(xy[0].trim()),
                            Double.parseDouble(xy[1].trim())));
                }
            }
        }
        return points;
    }
}