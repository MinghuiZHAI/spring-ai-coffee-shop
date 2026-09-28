package com.zmh.atlantic.coffee.ai.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * 向量库 bean（详细设计 v1.1 §3.1.1，决策 #51）：
 * 所有参与 FilterExpression 过滤的元数据字段必须在索引 Schema 中显式声明，
 * 否则过滤静默失效；starter 自动配置不支持经属性声明 metadata 字段，故自定义 bean。
 * 该 bean 存在后自动配置退避（含其 JedisPooled），连接 bean 一并在本类提供。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.ai.vectorstore.redis.uri:redis://localhost:63791}") String uri) {
        return new JedisPooled(java.net.URI.create(uri));
    }

    @Bean
    public RedisVectorStore vectorStore(JedisPooled jedis, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName("atlantic-kb")
                .prefix("atlantic:kb:")
                .initializeSchema(true)
                // kb_type：四个业务 Agent 的 RAG 过滤字段（§3.1）
                // doc_id：按文档删旧向量用（§3.4 重建管道"先删后写"，决策 #51）
                .metadataFields(RedisVectorStore.MetadataField.tag("kb_type"),
                        RedisVectorStore.MetadataField.tag("doc_id"))   // 1.1.2 实际方法名（javap 核实），非 addMetadataFields
                .build();
    }
}
