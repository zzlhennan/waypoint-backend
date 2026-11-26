package com.zzl.waypointbackend.along.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * 返回数量上限；不传用配置 along.top-n（默认 3）。
     * 特殊值：0 表示「全部」（不截取，返回硬过滤后的全部候选）。
     */
    @Min(value = 0, message = "topN 不能为负数；0 表示全部")
    private Integer topN;

    /** 采样点搜索半径（米）；不传用配置 along.search-radius-meters */
    @Positive(message = "radius 必须为正数")
    private Integer radius;

    /**
     * 分类名（如「冷饮」「咖啡厅」），后端经 DictUtils 翻译成 types 编码再透传。
     * 不校验枚举值——查不到就 null 兜底走纯 keyword 模糊搜（不报错）。
     */
    private String category;

    /** 驾车算路策略，仅 driving 生效：0速度/1费用/2距离/3不走高速；不传用高德默认(0) */
    @Min(value = 0, message = "strategy 只能是 0-3")
    @Max(value = 3, message = "strategy 只能是 0-3")
    private Integer strategy;
}
