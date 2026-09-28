package com.zmh.atlantic.coffee.ai.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 结构感知分块（详细设计 §3.4 / 总体设计 §3.4 入库管道）：
 * 标题树解析 → 结构分块，块 ≤500 字、相邻块 50 字重叠；FAQ 一问一答独立成块。
 * 块文本前缀携带文档标题与标题路径，保证检索命中后模型有上下文定位。
 */
@Component
public class KbChunker {

    static final int MAX_CHUNK_CHARS = 500;
    static final int OVERLAP_CHARS = 50;

    /** 分块结果：text 为最终嵌入文本，heading 为来源标题路径（调试/展示用）。 */
    public record Chunk(String text, String heading) {
    }

    public List<Chunk> chunk(KbDocument doc) {
        List<Section> sections = parseSections(doc.getContent());
        List<Chunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            String header = section.heading().isBlank()
                    ? doc.getTitle() : doc.getTitle() + " > " + section.heading();
            String body = section.body().strip();
            if (body.isEmpty()) {
                continue;
            }
            // FAQ 类型一问一答独立成块：### 小节即问答对，不再做超长切分（种子语料单问答 <500 字）
            if ("FAQ".equals(doc.getKbType()) || body.length() <= MAX_CHUNK_CHARS) {
                chunks.add(new Chunk(header + "\n" + body, header));
                continue;
            }
            chunks.addAll(splitLongSection(header, body));
        }
        return chunks;
    }

    // ===== 内部 =====

    private record Section(String heading, String body) {
    }

    /** 按行扫描：#/##/### 标题行开启新 Section，其余行累积为该 Section 的 body。 */
    private List<Section> parseSections(String markdown) {
        List<Section> sections = new ArrayList<>();
        String currentHeading = "";
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#")) {
                if (!body.isEmpty()) {
                    sections.add(new Section(currentHeading, body.toString()));
                    body.setLength(0);
                }
                currentHeading = trimmed.replaceAll("^#+\\s*", "");
            } else {
                body.append(line).append("\n");
            }
        }
        if (!body.isEmpty()) {
            sections.add(new Section(currentHeading, body.toString()));
        }
        return sections;
    }

    /** 超长 Section：按段落打包到 ≤500 字，相邻块保留 50 字尾部重叠。 */
    private List<Chunk> splitLongSection(String header, String body) {
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : body.split("\n")) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (current.length() + p.length() + 1 > MAX_CHUNK_CHARS && current.length() > 0) {
                chunks.add(new Chunk(header + "\n" + current, header));
                String tail = current.length() > OVERLAP_CHARS
                        ? current.substring(current.length() - OVERLAP_CHARS) : current.toString();
                current = new StringBuilder(tail);
            }
            current.append(p).append("\n");
        }
        if (!current.isEmpty()) {
            chunks.add(new Chunk(header + "\n" + current, header));
        }
        return chunks;
    }
}
