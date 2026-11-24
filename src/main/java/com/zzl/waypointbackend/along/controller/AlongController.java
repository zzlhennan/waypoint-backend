package com.zzl.waypointbackend.along.controller;

import com.zzl.waypointbackend.along.dto.AlongQuery;
import com.zzl.waypointbackend.along.service.AlongService;
import com.zzl.waypointbackend.along.vo.AlongResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 沿途 POI 推荐。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/along")
public class AlongController {

    private final AlongService alongService;

    /** 沿途搜索 POI：GET /along/search?city=...&start=...&end=...&keyword=... */
    @GetMapping("/search")
    public AlongResponse along(@Validated AlongQuery query) {
        return alongService.along(query);
    }
}
