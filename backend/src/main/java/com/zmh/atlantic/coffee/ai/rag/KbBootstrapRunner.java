package com.zmh.atlantic.coffee.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.util.Map;

/**
 * KB 种子向量索引启动构建（详细设计 §1.4，决策 #49）：应用就绪后检查索引是否为空，
 * 为空则对全部 PUBLISHED 文档执行一次全量重建——"一键起库即有可用的 RAG"（技术栈 #34）。
 *
 * <p>监听 ApplicationReadyEvent 而非 ApplicationRunner：ReadyEvent 触发时 VectorStore bean
 * 的 FT.CREATE 已完成（InitializingBean.afterPropertiesSet），避免把"索引未建好"误判为"索引为空"。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbBootstrapRunner implements ApplicationListener<ApplicationReadyEvent> {

    private final JedisPooled jedis;
    private final KbIndexService kbIndexService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 幂等：应用上下文刷新（如 devtools）或集群多实例时只构建一次
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        try {
            if (indexHasDocuments()) {
                log.info("KB 向量索引已有数据，跳过启动构建");
                return;
            }
            log.info("KB 向量索引为空，开始种子构建…");
            kbIndexService.reindexAllPublished();
        } catch (Exception e) {
            // 启动引导失败不阻断应用：RAG 检索退化为无结果，管理端可手动重建
            log.error("KB 向量索引启动构建失败", e);
        }
    }

    private boolean indexHasDocuments() {
        try {
            Map<String, Object> info = jedis.ftInfo("atlantic-kb");
            Object numDocs = info.get("num_docs");
            return numDocs instanceof Number n && n.longValue() > 0;
        } catch (Exception e) {
            // 索引不存在等情况视同空索引，触发重建
            log.warn("FT.INFO 查询失败（视同空索引）: {}", String.valueOf(e.getMessage()));
            return false;
        }
    }
}
