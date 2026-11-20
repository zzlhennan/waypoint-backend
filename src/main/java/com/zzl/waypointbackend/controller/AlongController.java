package com.zzl.waypointbackend.controller;

import com.zzl.waypointbackend.dto.AlongQuery;
import com.zzl.waypointbackend.service.AlongService;
import com.zzl.waypointbackend.vo.AlongResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 沿途 POI 推荐（MVP 唯一业务接口）。
 */
@RestController
@RequiredArgsConstructor
public class AlongController {

    private final AlongService alongService;

    @GetMapping("/along")
    public AlongResponse along(@Validated AlongQuery query) {
        return alongService.along(query);
    }
}