package com.zzl.waypointbackend.utils.dict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用字典工具：按 dictType 维护 label ↔ value 双向缓存。
 *
 * <p>本期字典全部写死在静态块；未来迁数据库时，只需把 {@link #register} 的调用源换掉，
 * 对外 {@link #getValue}/{@link #getDictList} 签名不变。
 */
public final class DictUtils {

    /** 字典类型：沿途 POI 分类（label=分类名如「咖啡厅」，value=高德 types 编码） */
    public static final String TYPE_POI_CATEGORY = "poi_category";

    /** label → value 缓存；key: dictType，value: Map<label, value>（LinkedHashMap 保留注册顺序） */
    private static final Map<String, Map<String, String>> LABEL_TO_VALUE = new ConcurrentHashMap<>();

    /** value → label 缓存；key: dictType，value: Map<value, label> */
    private static final Map<String, Map<String, String>> VALUE_TO_LABEL = new ConcurrentHashMap<>();

    static {
        // 编码已从官方分类表（docs/Attachment02）核实
        register(TYPE_POI_CATEGORY, Map.of(
                "咖啡厅", "050500",
                "冷饮",   "050700",
                "糕饼",   "050800",
                "甜品",   "050900",
                "茶艺",   "050600",
                "加油站", "010100",
                "便利店", "060200"
        ));
    }

    private DictUtils() {
    }

    /**
     * 注册一个字典类型：双向写入两个缓存。
     *
     * @param dictType     字典类型标识
     * @param labelToValue label → value（Map.of 本身无序，用 LinkedHashMap 包一层以保证 getDictList 输出稳定）
     */
    private static void register(String dictType, Map<String, String> labelToValue) {
        Map<String, String> labelMap = new LinkedHashMap<>(labelToValue);
        Map<String, String> valueMap = new LinkedHashMap<>();
        labelMap.forEach((label, value) -> valueMap.put(value, label));
        LABEL_TO_VALUE.put(dictType, Collections.unmodifiableMap(labelMap));
        VALUE_TO_LABEL.put(dictType, Collections.unmodifiableMap(valueMap));
    }

    /**
     * label → value。
     *
     * @param dictType 字典类型
     * @param label    分类名（如「冷饮」）
     * @return 对应编码；label 为空或查不到时返回 null（走纯 keyword 模糊搜，不报错）
     */
    public static String getValue(String dictType, String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        Map<String, String> map = LABEL_TO_VALUE.get(dictType);
        if (map == null) {
            return null;
        }
        return map.get(label.trim());
    }

    /**
     * value → label（反向翻译，预留；本期暂未使用）。
     */
    public static String getLabel(String dictType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Map<String, String> map = VALUE_TO_LABEL.get(dictType);
        if (map == null) {
            return null;
        }
        return map.get(value.trim());
    }

    /**
     * 取整个字典列表（按注册顺序），供 /along/categories 等接口直接序列化。
     *
     * @param dictType 字典类型
     * @return 字典项列表；dictType 未注册时返回空列表
     */
    public static List<DictItem> getDictList(String dictType) {
        Map<String, String> map = LABEL_TO_VALUE.get(dictType);
        if (map == null) {
            return Collections.emptyList();
        }
        List<DictItem> list = new ArrayList<>(map.size());
        map.forEach((label, value) -> list.add(new DictItem(label, value)));
        return list;
    }
}
