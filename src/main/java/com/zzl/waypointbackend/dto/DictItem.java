package com.zzl.waypointbackend.dto;

/**
 * 字典项（内部传输对象）：label ↔ value 成对。
 *
 * <p>这是后端内部视图（如 DictUtils.getDictList 返回）；
 * 对外 VO 见 {@link com.zzl.waypointbackend.vo.CategoriesResponse.CategoryItem}——后者只暴露 label，
 * value（types 编码）是后端翻译用的内部细节，不下发给前端。
 */
public record DictItem(String label, String value) {
}