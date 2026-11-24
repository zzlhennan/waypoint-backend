package com.zzl.waypointbackend.controller;

import com.zzl.waypointbackend.utils.DictUtils;
import com.zzl.waypointbackend.vo.CategoriesResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典/分类接口（独立于 /along，未来其他字典接口也挂这里）。
 */
@RestController
public class CategoryController {

    /** 「全部」项的 label：前端概念，表示不做分类过滤（category 传空） */
    private static final String ALL_LABEL = "全部";

    /**
     * 返回分类 tab 列表：第一项「全部」，后面是 DictUtils 里注册的 POI 分类。
     * 前端直接拿来渲染横排按钮，点击后把 label 作为 category 参数回传，翻译仍收口在后端。
     */
    @GetMapping("/along/categories")
    public CategoriesResponse categories() {
        List<CategoriesResponse.CategoryItem> items = new ArrayList<>();
        items.add(new CategoriesResponse.CategoryItem(ALL_LABEL));
        DictUtils.getDictList(DictUtils.TYPE_POI_CATEGORY)
                .forEach(item -> items.add(new CategoriesResponse.CategoryItem(item.label())));

        CategoriesResponse resp = new CategoriesResponse();
        resp.setCategories(items);
        return resp;
    }
}