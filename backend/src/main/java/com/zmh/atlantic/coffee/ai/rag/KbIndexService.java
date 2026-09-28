package com.zmh.atlantic.coffee.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KB 向量索引服务（总体设计 §3.4 入库管道的实现侧）：结构分块 → 批量嵌入 →
 * 先删旧向量（doc_id 过滤）→ 写新向量。同步执行（技术栈 #24），失败向上抛由调用方定状态门。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbIndexService {

    /**
     * 嵌入批量上限：DashScope text-embedding-v3 服务端单请求 ≤10 条文本
     * （超限报 InvalidParameter: batch size is invalid, it should not be larger than 10；
     * SAA SDK 侧另有 25 条客户端守卫——以更严的服务端为准，实测核实）。
     */
    static final int EMBED_BATCH_SIZE = 10;

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunker kbChunker;
    private final VectorStore vectorStore;

    /** 重建单文档向量；成功后置 PUBLISHED（状态门，决策 #44），失败保持原状态可重试。 */
    public void reindexDocument(KbDocument doc) {
        List<KbChunker.Chunk> chunks = kbChunker.chunk(doc);
        vectorStore.delete(docIdFilter(doc.getId()));
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = Map.of(
                    "doc_id", String.valueOf(doc.getId()),
                    "kb_type", doc.getKbType(),
                    "title", doc.getTitle());
            documents.add(Document.builder()
                    .id(documentId(doc.getId(), i))
                    .text(chunks.get(i).text())
                    .metadata(metadata)
                    .build());
        }
        for (int from = 0; from < documents.size(); from += EMBED_BATCH_SIZE) {
            vectorStore.add(documents.subList(from, Math.min(from + EMBED_BATCH_SIZE, documents.size())));
        }
        KbDocument update = new KbDocument();
        update.setId(doc.getId());
        update.setStatus("PUBLISHED");
        kbDocumentMapper.updateById(update);
        log.info("KB 文档向量重建完成: docId={} chunks={}", doc.getId(), chunks.size());
    }

    /** 全量重建（KbBootstrapRunner 启动引导 / M2 管理端 reindex 兜底复用）。 */
    public void reindexAllPublished() {
        List<KbDocument> docs = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getStatus, "PUBLISHED"));
        log.info("KB 全量重建开始: documents={}", docs.size());
        for (KbDocument doc : docs) {
            reindexDocument(doc);
        }
        log.info("KB 全量重建完成");
    }

    static String documentId(Long docId, int index) {
        return "kb-" + docId + "-" + index;
    }

    static Filter.Expression docIdFilter(Long docId) {
        return new FilterExpressionBuilder()
                .eq("doc_id", String.valueOf(docId))
                .build();
    }
}
