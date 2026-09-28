package com.zmh.atlantic.coffee.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.CategoryNode;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.MenuResponse;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.ProductCard;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.ProductDetail;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.SpecGroupDto;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.SpecOptionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 菜单/单品查询（含规格分组与中文组名映射）。 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private static final Map<String, String> GROUP_LABELS = Map.of(
            "TEMPERATURE", "温度", "SWEETNESS", "糖度", "ICE", "冰量");

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SpecOptionMapper specOptionMapper;

    public MenuResponse menu() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getStatus, 1).orderByAsc(Category::getSort));
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1).orderByAsc(Product::getId));
        Map<Long, List<Product>> byCategory = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(Product::getCategoryId));
        List<CategoryNode> nodes = categories.stream()
                .map(c -> new CategoryNode(c.getId(), c.getName(),
                        byCategory.getOrDefault(c.getId(), List.of()).stream()
                                .map(p -> new ProductCard(p.getId(), p.getName(), p.getDescription(),
                                        p.getBasePrice(), splitTags(p.getTags())))
                                .toList()))
                .filter(node -> !node.products().isEmpty())
                .toList();
        return new MenuResponse(nodes);
    }

    public ProductDetail detail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }
        var category = categoryMapper.selectById(product.getCategoryId());
        List<SpecOption> options = specOptionMapper.selectList(new LambdaQueryWrapper<SpecOption>()
                .eq(SpecOption::getStatus, 1).orderByAsc(SpecOption::getSort));
        Map<String, List<SpecOptionDto>> grouped = new LinkedHashMap<>();
        for (SpecOption option : options) {
            grouped.computeIfAbsent(option.getSpecGroup(), k -> new ArrayList<>())
                    .add(new SpecOptionDto(option.getOptionName(), option.getPriceDelta()));
        }
        List<SpecGroupDto> groups = grouped.entrySet().stream()
                .map(e -> new SpecGroupDto(e.getKey(), GROUP_LABELS.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .toList();
        return new ProductDetail(product.getId(), product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getName(), product.getDescription(), product.getBasePrice(),
                splitTags(product.getTags()), groups);
    }

    public static List<String> splitTags(String tags) {
        return tags == null || tags.isBlank() ? List.of() : Arrays.stream(tags.split(",")).map(String::trim).toList();
    }
}
