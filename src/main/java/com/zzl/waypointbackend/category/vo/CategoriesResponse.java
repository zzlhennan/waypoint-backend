package com.zzl.waypointbackend.category.vo;

import lombok.Data;

import java.util.List;

/**
 * GET /along/categories 成功响应。
 *
 * <p>只暴露 label：types 编码是后端 DictUtils 翻译用的内部细节，不下发给前端，
 * 避免前端误传裸编码、绕开字典翻译。
 */
@Data
public class CategoriesResponse {

    /** 分类列表（第一项为「全部」，表示不做分类过滤） */
    private List<CategoryItem> categories;

    @Data
    public static class CategoryItem {
        /** 分类显示名（如「全部」「咖啡厅」），前端直接渲染 tab，点击后原样作为 category 参数回传 */
        private String label;

        public CategoryItem(String label) {
            this.label = label;
        }
    }
}
