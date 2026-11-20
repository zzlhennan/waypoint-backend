package com.zzl.waypointbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * GET /along 入参。必填项走 Bean Validation，可选项不传时由 service 用配置默认值兜底。
 */
@Data
public class AlongQuery {

    @NotBlank(message = "city 不能为空")
    private String city;

    @NotBlank(message = "start 不能为空")
    private String start;

    @NotBlank(message = "end 不能为空")
    private String end;

    @NotBlank(message = "keyword 不能为空")
    private String keyword;

    /** 交通方式：driving / walking / bicycling；不传默认 driving */
    @Pattern(regexp = "driving|walking|bicycling", message = "mode 只能是 driving/walking/bicycling")
    private String mode;

    /** 返回数量上限（1-10）；不传用配置 along.top-n */
    @Positive(message = "topN 必须为正数")
    private Integer topN;

    /** 采样点搜索半径（米）；不传用配置 along.search-radius-meters */
    @Positive(message = "radius 必须为正数")
    private Integer radius;
}