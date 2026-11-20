package com.zzl.waypointbackend.service;

import com.zzl.waypointbackend.dto.AlongQuery;
import com.zzl.waypointbackend.vo.AlongResponse;

/**
 * 沿途 POI 推荐：编排 定位 → 路线 → 采样 → 周边搜 → 去重 → 算距离 → 硬过滤 → 排序 TopN。
 */
public interface AlongService {

    AlongResponse along(AlongQuery query);
}