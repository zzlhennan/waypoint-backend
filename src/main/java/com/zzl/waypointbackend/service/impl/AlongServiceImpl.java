package com.zzl.waypointbackend.service.impl;

import com.zzl.waypointbackend.config.AlongProperties;
import com.zzl.waypointbackend.dto.AlongQuery;
import com.zzl.waypointbackend.dto.PoiCandidate;
import com.zzl.waypointbackend.dto.Point;
import com.zzl.waypointbackend.exception.BizException;
import com.zzl.waypointbackend.feignclient.AmapClient;
import com.zzl.waypointbackend.feignclient.dto.AmapV5PoiResponse;
import com.zzl.waypointbackend.feignclient.dto.AmapV5RouteResponse;
import com.zzl.waypointbackend.service.AlongService;
import com.zzl.waypointbackend.utils.DetourCalculator;
import com.zzl.waypointbackend.vo.AlongResponse;
import com.zzl.waypointbackend.vo.PoiItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AlongService 实现：串行调高德，按 PRD 7 步编排。MVP 不并发。
 */
@Service
@RequiredArgsConstructor
public class AlongServiceImpl implements AlongService {

    private static final String SHOW_FIELDS_POLYLINE = "polyline";
    private static final String SHOW_FIELDS_BUSINESS = "business";

    private final AmapClient amapClient;
    private final AlongProperties alongProperties;

    @Override
    public AlongResponse along(AlongQuery query) {
        // 入参覆盖配置：mode/topN/radius 不传时用默认值
        String mode = query.getMode() != null ? query.getMode() : "driving";
        int topN = query.getTopN() != null ? query.getTopN() : alongProperties.topN();
        int radius = query.getRadius() != null ? query.getRadius() : alongProperties.searchRadiusMeters();

        // ① 定位起点、终点
        Point start = locate(query.getStart(), query.getCity(), "起点");
        Point end = locate(query.getEnd(), query.getCity(), "终点");

        // ② 路线规划（按 mode 选接口）→ 折线
        List<Point> route = routeByMode(mode, start, end);
        if (route.size() < 2) {
            throw BizException.upstream("路线规划返回为空，请稍后重试");
        }

        // ③ 沿折线采样（起点 + 每 500m 中间点 + 终点）
        List<Point> samples = DetourCalculator.sampleRoute(route, alongProperties.sampleIntervalMeters());

        // ④ 每个采样点独立搜 POI：本采样点无结果才扩到 retry 半径重试一次（不影响其他采样点）；
        // ④.5 按 id 去重
        Map<String, PoiCandidate> candidates = new LinkedHashMap<>();
        for (Point sample : samples) {
            int before = candidates.size();
            collectAround(sample, query.getKeyword(), query.getCity(), radius, candidates);
            if (candidates.size() == before) {
                // 本采样点初始半径无新增候选 → 用 retry 半径再试一次
                collectAround(sample, query.getKeyword(), query.getCity(),
                        alongProperties.searchRadiusRetryMeters(), candidates);
            }
        }

        // ⑤ 算每个候选到折线的最近距离
        for (PoiCandidate c : candidates.values()) {
            c.setDistanceToRouteMeters(DetourCalculator.minDistanceMeters(parsePoint(c.getLocation()), route));
        }

        // ⑥ 硬过滤：离主路超 maxDetourMeters 丢弃；⑦ 升序取 TopN
        List<PoiCandidate> filtered = candidates.values().stream()
                .filter(c -> c.getDistanceToRouteMeters() <= alongProperties.maxDetourMeters())
                .sorted((a, b) -> Double.compare(a.getDistanceToRouteMeters(), b.getDistanceToRouteMeters()))
                .limit(topN)
                .toList();

        if (filtered.isEmpty()) {
            throw BizException.notFound("沿途 " + alongProperties.maxDetourMeters() + " 米内没找到 " + query.getKeyword());
        }

        return buildResponse(query, mode, filtered);
    }

    /** ① 定位：地名 → 坐标，取 pois[0].location；空 → 400 */
    private Point locate(String name, String city, String label) {
        AmapV5PoiResponse resp = amapClient.searchText(name, city, Boolean.TRUE);
        if (isFailure(resp) || resp.getPois() == null || resp.getPois().isEmpty()) {
            throw BizException.badRequest("找不到" + label + "：" + name);
        }
        return parsePoint(resp.getPois().get(0).getLocation());
    }

    /** ② 按 mode 调对应路径接口 */
    private List<Point> routeByMode(String mode, Point start, Point end) {
        String origin = toAmapLocation(start);
        String destination = toAmapLocation(end);
        AmapV5RouteResponse resp = switch (mode) {
            case "walking" -> amapClient.walking(origin, destination, SHOW_FIELDS_POLYLINE);
            case "bicycling" -> amapClient.bicycling(origin, destination, SHOW_FIELDS_POLYLINE);
            default -> amapClient.driving(origin, destination, null, SHOW_FIELDS_POLYLINE);
        };
        if (isFailure(resp)) {
            throw BizException.upstream("路线规划失败，请稍后重试");
        }
        return resp.getRoutePoints();
    }

    /** ④ 单个采样点周边搜，结果按 id 并入候选 map（去重） */
    private void collectAround(Point sample, String keyword, String city, int radius,
                               Map<String, PoiCandidate> candidates) {
        AmapV5PoiResponse resp = amapClient.around(
                toAmapLocation(sample), keyword, radius, null, SHOW_FIELDS_BUSINESS);
        if (isFailure(resp) || resp.getPois() == null) {
            return;
        }
        for (AmapV5PoiResponse.Poi p : resp.getPois()) {
            if (p.getId() == null || p.getLocation() == null) {
                continue;
            }
            PoiCandidate c = new PoiCandidate();
            c.setId(p.getId());
            c.setName(p.getName());
            c.setAddress(p.getAddress());
            c.setLocation(p.getLocation());
            c.setType(p.getType());
            c.setAdname(p.getAdname());
            c.setTel(p.getBusiness() != null ? p.getBusiness().getTel() : null);
            candidates.putIfAbsent(p.getId(), c);
        }
    }

    private AlongResponse buildResponse(AlongQuery query, String mode, List<PoiCandidate> candidates) {
        AlongResponse resp = new AlongResponse();
        AlongResponse.Query q = new AlongResponse.Query();
        q.setCity(query.getCity());
        q.setStart(query.getStart());
        q.setEnd(query.getEnd());
        q.setKeyword(query.getKeyword());
        q.setMode(mode);
        resp.setQuery(q);

        List<PoiItem> items = new ArrayList<>();
        for (PoiCandidate c : candidates) {
            PoiItem item = new PoiItem();
            item.setName(c.getName());
            item.setAddress(c.getAddress());
            // 算法内部用 double 保证排序精度，对外取整成"米"，避免 12.273972520623792 这种长尾
            item.setDistanceToRouteMeters(Math.round(c.getDistanceToRouteMeters()));
            item.setLocation(c.getLocation());
            item.setTel(c.getTel());
            item.setType(c.getType());
            item.setAdname(c.getAdname());
            items.add(item);
        }
        resp.setResult(items);
        return resp;
    }

    /** "经度,纬度" 字符串 → Point */
    private static Point parsePoint(String location) {
        String[] xy = location.split(",");
        return new Point(Double.parseDouble(xy[0].trim()), Double.parseDouble(xy[1].trim()));
    }

    /** Point → "经度,纬度"（高德入参格式） */
    private static String toAmapLocation(Point p) {
        return p.longitude() + "," + p.latitude();
    }

    /** 高德调用是否失败：响应为空或 status != "1" */
    private static boolean isFailure(AmapV5PoiResponse resp) {
        return resp == null || !"1".equals(resp.getStatus());
    }

    private static boolean isFailure(AmapV5RouteResponse resp) {
        return resp == null || !"1".equals(resp.getStatus());
    }
}