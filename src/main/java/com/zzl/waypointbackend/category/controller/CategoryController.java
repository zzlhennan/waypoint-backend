package com.zzl.waypointbackend.category.controller;

import com.zzl.waypointbackend.category.vo.CategoriesResponse;
import com.zzl.waypointbackend.utils.dict.DictItem;
import com.zzl.waypointbackend.utils.dict.DictUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用字典查询接口：按 dictType 返回字典列表。
 *
 * <p>未来 DictUtils 注册新的 dictType，前端不用改路径，传不同 dictType 即可。
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    /** 「全部」项的 label：前端概念，表示不做分类过滤（category 传空），仅 POI 分类列表前拼 */
    private static final String ALL_LABEL = "全部";

    /**
     * 取字典列表。
     *
     * @param dictType 字典类型（必填，如 {@link DictUtils#TYPE_POI_CATEGORY}）
     * @return 字典项列表；POI 分类时列表前会拼「全部」项
     */
    @GetMapping("/list")
    public CategoriesResponse list(@RequestParam String dictType) {
        List<CategoriesResponse.CategoryItem> items = new ArrayList<>();

        // POI 分类列表前拼「全部」（前端概念，不做分类过滤）
        if (DictUtils.TYPE_POI_CATEGORY.equals(dictType)) {
            items.add(new CategoriesResponse.CategoryItem(ALL_LABEL));
        }

        List<DictItem> dict = DictUtils.getDictList(dictType);
        dict.forEach(item -> items.add(new CategoriesResponse.CategoryItem(item.label())));

        CategoriesResponse resp = new CategoriesResponse();
        resp.setCategories(items);
        return resp;
    }
}