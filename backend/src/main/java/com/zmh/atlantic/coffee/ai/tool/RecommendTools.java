package com.zmh.atlantic.coffee.ai.tool;

import com.zmh.atlantic.coffee.ai.log.ToolCallLogger;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.DrinkCandidate;
import com.zmh.atlantic.coffee.product.Product;
import com.zmh.atlantic.coffee.product.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 推荐规则引擎工具 ×1（详细设计 §3.3 / 技术栈 §12.2）：规则在工具内实现，
 * 模型只把候选组织成话术，不得编造菜单外饮品（Prompts.RECOMMEND_AGENT_PROMPT 核心约束）。
 */
@Component
@RequiredArgsConstructor
public class RecommendTools {

    /** 口味偏好词 → 商品 tags 关键词映射（V2 种子标签口径：微苦/清爽/果香/奶香/偏甜…）。 */
    private static final List<String> TASTE_KEYWORDS =
            List.of("偏甜", "微苦", "果香", "奶香", "清爽", "浓烈", "微酸", "巧克力", "气泡");

    /** 热饮排除项：名称命中即视为明显冷饮（规格为全局可选，MVP 以名称关键词近似）。 */
    private static final List<String> COLD_HINTS = List.of("冰", "冷萃", "气泡", "生椰");

    private final ProductQueryService productQueryService;
    private final ToolCallLogger toolCallLogger;

    @Value("${atlantic.recommend.max-candidates:3}")
    private int maxCandidates;

    @Tool(description = "按口味/温度/预算规则筛选推荐饮品，返回候选及命中活动标记")
    public List<DrinkCandidate> recommendDrinks(
            ToolContext toolContext,
            @ToolParam(required = false, description = "口味偏好，如：偏甜/微苦/果香/奶香") String taste,
            @ToolParam(required = false, description = "可选：热饮/冷饮") String temperature,
            @ToolParam(required = false, description = "可选：预算上限（元）") BigDecimal maxPrice) {
        return toolCallLogger.record(toolContext, "recommendDrinks",
                ToolCallLogger.params("taste", taste, "temperature", temperature, "maxPrice", maxPrice), () -> {
                    currentUserId(toolContext);
                    List<String> wanted = extractTasteKeywords(taste);
                    boolean hotOnly = temperature != null && temperature.contains("热");

                    List<Scored> scored = new ArrayList<>();
                    for (Product p : productQueryService.listOnShelf()) {
                        if (maxPrice != null && p.getBasePrice().compareTo(maxPrice) > 0) {
                            continue;
                        }
                        if (hotOnly && COLD_HINTS.stream().anyMatch(p.getName()::contains)) {
                            continue;
                        }
                        List<String> tags = ProductQueryService.splitTags(p.getTags());
                        List<String> matched = tags.stream().filter(wanted::contains).toList();
                        // 无口味偏好时全部候选可行；有偏好时按命中数排序，未命中者仅作兜底
                        scored.add(new Scored(new DrinkCandidate(p.getName(), p.getBasePrice(),
                                matched.isEmpty() ? tags.stream().limit(2).toList() : matched,
                                isActivityHit(p)), matched.size()));
                    }
                    return scored.stream()
                            .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed()
                                    .thenComparing(s -> s.candidate.price()))
                            .limit(maxCandidates)
                            .map(s -> s.candidate)
                            .toList();
                });
    }

    // ===== 内部 =====

    private List<String> extractTasteKeywords(String taste) {
        if (taste == null || taste.isBlank()) {
            return List.of();
        }
        return TASTE_KEYWORDS.stream().filter(taste::contains).toList();
    }

    /** MVP 活动标记：名称/描述含限定或新品尝鲜（无活动表，规则近似，M2 活动域接入后替换）。 */
    private boolean isActivityHit(Product p) {
        String text = p.getName() + p.getDescription();
        return text.contains("限定") || text.contains("新品") || text.contains("尝鲜");
    }

    private void currentUserId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("userId");
        if (v == null) {
            throw new com.zmh.atlantic.coffee.common.exception.BizException(40301, "缺少用户上下文");
        }
    }

    private record Scored(DrinkCandidate candidate, int score) {
    }
}
