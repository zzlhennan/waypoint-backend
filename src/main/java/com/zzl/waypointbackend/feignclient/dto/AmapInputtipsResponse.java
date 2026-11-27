package com.zzl.waypointbackend.feignclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * 高德输入提示 v3 响应（/v3/assistant/inputtips）。
 *
 * <p>与 v5 响应结构不同：成功标识是 {@code status="1"}，结果在 {@code tips[]} 而非 {@code pois[]}。
 *
 * <p>⚠️ v3 坑：字段缺失时有时返回空数组 {@code []}（如 {@code "address": []}），
 * 直接用 String 接收会反序列化失败。下面 {@link EmptyArrayAsNullDeserializer}
 * 把"空数组"统一转成 null。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapInputtipsResponse {

    private String status;
    private String info;
    private String infocode;

    private List<Tip> tips;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tip {
        private String name;
        /** "经度,纬度"（行政区联想时可能为空） */
        @JsonDeserialize(using = EmptyArrayAsNullDeserializer.class)
        private String location;
        /** ⚠️ v3 缺失时返回 []，用自定义反序列化兼容 */
        @JsonDeserialize(using = EmptyArrayAsNullDeserializer.class)
        private String address;
        @JsonDeserialize(using = EmptyArrayAsNullDeserializer.class)
        private String district;
    }

    /**
     * 把"空数组 []"反序列化成 null；正常字符串照旧。
     * 解决高德 v3 字段缺失时返回 [] 而非 "" 的坑。
     */
    public static class EmptyArrayAsNullDeserializer extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = p.readValueAsTree();
            // 空数组 → null
            if (node.isArray() && node.isEmpty()) {
                return null;
            }
            // 文本节点 → 取文本（空串也归为 null）
            String text = node.asString();
            return text == null || text.isBlank() ? null : text;
        }
    }
}