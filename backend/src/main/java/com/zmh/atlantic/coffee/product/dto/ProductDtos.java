package com.zmh.atlantic.coffee.product.dto;

import java.math.BigDecimal;
import java.util.List;

/** 菜单/单品视图结构（总体设计 §5.2 菜单域契约）。 */
public final class ProductDtos {

    private ProductDtos() {
    }

    public record MenuResponse(List<CategoryNode> categories) {
    }

    public record CategoryNode(Long id, String name, List<ProductCard> products) {
    }

    public record ProductCard(Long id, String name, String description, BigDecimal basePrice, List<String> tags) {
    }

    public record ProductDetail(Long id, Long categoryId, String categoryName, String name, String description,
                                BigDecimal basePrice, List<String> tags, List<SpecGroupDto> specs) {
    }

    /** 规格组：group 为枚举名（TEMPERATURE/SWEETNESS/ICE），label 为中文展示名。 */
    public record SpecGroupDto(String group, String label, List<SpecOptionDto> options) {
    }

    public record SpecOptionDto(String optionName, BigDecimal priceDelta) {
    }
}
