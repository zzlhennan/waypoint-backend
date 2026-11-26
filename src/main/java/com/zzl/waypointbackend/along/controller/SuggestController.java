package com.zzl.waypointbackend.along.controller;

import com.zzl.waypointbackend.along.vo.DistrictSuggestResponse;
import com.zzl.waypointbackend.along.vo.PoiSuggestResponse;
import com.zzl.waypointbackend.common.exception.BizException;
import com.zzl.waypointbackend.feignclient.AmapClient;
import com.zzl.waypointbackend.feignclient.dto.AmapDistrictResponse;
import com.zzl.waypointbackend.feignclient.dto.AmapInputtipsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 地名/城市联想：后端代理高德接口，key 不下发前端。
 *
 * <p>两个端点各自返回自己的 VO，不共用：
 * <ul>
 *   <li>{@code GET /suggest/poi} —— 地名联想（inputtips v3，datatype=poi）</li>
 *   <li>{@code GET /suggest/district} —— 城市联想（district v3，支持模糊匹配）</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/suggest")
public class SuggestController {

    private final AmapClient amapClient;

    /**
     * 地名联想：起点/终点输入框用。
     *
     * @param keyword 输入框内容（如"奥森"）
     * @param city    城市名（限定搜索范围）
     */
    @GetMapping("/poi")
    public PoiSuggestResponse suggestPoi(@RequestParam String keyword,
                                          @RequestParam(required = false) String city) {
        AmapInputtipsResponse resp = amapClient.inputtips(keyword, "poi", city);
        if (resp == null || !"1".equals(resp.getStatus())) {
            throw BizException.upstream("地图服务暂时不可用");
        }

        List<PoiSuggestResponse.SuggestItem> items = new ArrayList<>();
        if (resp.getTips() != null) {
            for (AmapInputtipsResponse.Tip tip : resp.getTips()) {
                if (tip.getName() == null || tip.getName().isBlank()) continue;
                items.add(new PoiSuggestResponse.SuggestItem(
                        tip.getName(), tip.getLocation(), tip.getAddress()));
            }
        }

        PoiSuggestResponse result = new PoiSuggestResponse();
        result.setSuggestions(items);
        return result;
    }

    /**
     * 城市联想：城市输入框用。支持模糊匹配（输"北"返回北京市/北海市）。
     *
     * @param keyword 输入框内容（如"北"）
     */
    @GetMapping("/district")
    public DistrictSuggestResponse suggestDistrict(@RequestParam String keyword) {
        AmapDistrictResponse resp = amapClient.district(keyword, "0");
        if (resp == null || !"1".equals(resp.getStatus())) {
            throw BizException.upstream("地图服务暂时不可用");
        }

        List<DistrictSuggestResponse.DistrictItem> items = new ArrayList<>();
        if (resp.getDistricts() != null) {
            for (AmapDistrictResponse.District d : resp.getDistricts()) {
                if (d.getName() == null || d.getName().isBlank()) continue;
                items.add(new DistrictSuggestResponse.DistrictItem(d.getName(), d.getLevel()));
            }
        }

        DistrictSuggestResponse result = new DistrictSuggestResponse();
        result.setSuggestions(items);
        return result;
    }
}