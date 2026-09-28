package com.zmh.atlantic.coffee.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 结构感知分块（§3.4）：FAQ 一问一答独立成块、超长 Section ≤500 字切分与 50 字重叠。 */
class KbChunkerTest {

    private final KbChunker chunker = new KbChunker();

    private KbDocument doc(String kbType, String content) {
        KbDocument d = new KbDocument();
        d.setId(9L);
        d.setTitle("测试文档");
        d.setKbType(kbType);
        d.setContent(content);
        return d;
    }

    @Test
    @DisplayName("FAQ：每个 ### 问答独立成块，块文本携带标题路径")
    void faqBlocks() {
        KbDocument doc = doc("FAQ", """
                ## 常见问题
                ### 怎么下单？
                首页选择饮品加入购物车后结算。
                ### 取餐码在哪里看？
                支付成功后自动生成取餐码，在订单详情页展示。
                """);
        List<KbChunker.Chunk> chunks = chunker.chunk(doc);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains("怎么下单？").contains("首页选择饮品");
        assertThat(chunks.get(0).text()).startsWith("测试文档 > 怎么下单？");
        assertThat(chunks.get(1).text()).contains("取餐码在哪里看？");
    }

    @Test
    @DisplayName("普通文档：≤500 字的 Section 整块保留")
    void smallSectionKeptWhole() {
        KbDocument doc = doc("MENU", """
                ## 菜单总览
                ### 经典咖啡
                - 深海美式：12 元。微苦回甘。
                - 冷萃远航：15 元。顺滑低酸。
                """);
        List<KbChunker.Chunk> chunks = chunker.chunk(doc);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("深海美式").contains("冷萃远航");
    }

    @Test
    @DisplayName("超长 Section：按段落打包 ≤500 字，相邻块 50 字重叠")
    void longSectionSplitWithOverlap() {
        String paragraph = "这是一段用于撑长内容的测试文本，内容本身没有业务含义。";
        StringBuilder body = new StringBuilder("## 规则\n### 长规则\n");
        for (int i = 0; i < 30; i++) {
            body.append(paragraph).append("\n");
        }
        List<KbChunker.Chunk> chunks = chunker.chunk(doc("MEMBER_RULE", body.toString()));
        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
        for (KbChunker.Chunk chunk : chunks) {
            assertThat(chunk.text().length()).isLessThanOrEqualTo(KbChunker.MAX_CHUNK_CHARS + 60);
        }
        // 相邻块重叠：第二块开头应是第一块结尾的尾部片段
        String firstTail = chunks.get(0).text()
                .substring(chunks.get(0).text().length() - KbChunker.OVERLAP_CHARS);
        assertThat(chunks.get(1).text()).contains(firstTail.strip());
    }
}
