package com.zzl.waypointbackend.utils;

import java.util.Map;

/**
 * POI 分类字典：label（用户/前端传的分类名）→ types 编码（透传给高德 around）。
 *
 * <p>本期写死在代码常量；未来迁数据库时，换掉内部 Map、对外 {@link #getValue} 签名不变。
 * 只做入参翻译（label→value）一个方向——出参高德已返回中文名，无需反向翻译。
 */
public final class DictUtils {

    /** label → types 编码；编码已从官方分类表（docs/Attachment02）核实 */
    private static final Map<String, String> LABEL_TO_VALUE = Map.of(
            "咖啡厅", "050500",
            "冷饮",   "050700",
            "糕饼",   "050800",
            "甜品",   "050900",
            "茶艺",   "050600",
            "加油站", "010100",
            "便利店", "060200"
    );

    private DictUtils() {
    }

    /**
     * label → types 编码。
     *
     * @param label 分类名（如「冷饮」）
     * @return 对应编码；label 为空或查不到时返回 null（走纯 keyword 模糊搜，不报错）
     */
    public static String getValue(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return LABEL_TO_VALUE.get(label.trim());
    }
}