package com.zmh.atlantic.coffee.ai.config;

import com.zmh.atlantic.coffee.ai.prompt.Prompts;
import com.zmh.atlantic.coffee.ai.tool.MemberTools;
import com.zmh.atlantic.coffee.ai.tool.OrderTools;
import com.zmh.atlantic.coffee.ai.tool.RecommendTools;
import com.zmh.atlantic.coffee.ai.tool.StoreTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 链路装配（详细设计 §3.1，决策 #48）：单 ChatModel（starter 自动配置的 DashScopeChatModel）
 * + 5 个 ChatClient bean；模型档位用调用级 options 局部覆盖（IntentAgent→qwen-turbo），
 * 未指定字段沿用 bean 级默认（qwen-plus）。
 *
 * <p>Advisor 顺序约定：记忆 20（组装历史）→ RAG 30（检索注入）→ 日志 40（记录最终 prompt 与耗时）。
 * conversationId 与 toolContext 都不在 bean 级绑定，由 AiSessionHandler 每次调用成对注入
 * （决策 #55：漏传 conversationId 则多轮记忆完全失效）。</p>
 */
@Configuration
public class AiClientConfig {

    @Bean   // IntentAgent：无记忆无 RAG 无工具，仅结构化输出
    public ChatClient intentAgentClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(Prompts.INTENT_CLASSIFIER_PROMPT)
                .build();
    }

    @Bean   // 订单售后：kb_type == 'FAQ'（FAQ 全量，总体设计 §4.1），工具 OrderTools
    public ChatClient orderAgentClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                                       OrderTools orderTools, AiTraceAdvisor traceAdvisor) {
        return businessAgent(chatModel, chatMemory, vectorStore, traceAdvisor,
                Prompts.ORDER_AGENT_PROMPT, orderTools, "kb_type == 'FAQ'");
    }

    @Bean   // 会员权益：kb_type in ['MEMBER_RULE','ACTIVITY']，工具 MemberTools
    public ChatClient couponAgentClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                                        MemberTools memberTools, AiTraceAdvisor traceAdvisor) {
        return businessAgent(chatModel, chatMemory, vectorStore, traceAdvisor,
                Prompts.COUPON_AGENT_PROMPT, memberTools, "kb_type in ['MEMBER_RULE','ACTIVITY']");
    }

    @Bean   // 饮品推荐：kb_type == 'MENU'，工具 RecommendTools
    public ChatClient recommendAgentClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                                           RecommendTools recommendTools, AiTraceAdvisor traceAdvisor) {
        return businessAgent(chatModel, chatMemory, vectorStore, traceAdvisor,
                Prompts.RECOMMEND_AGENT_PROMPT, recommendTools, "kb_type == 'MENU'");
    }

    @Bean   // 通用知识：全库不过滤，工具 StoreTools
    public ChatClient faqAgentClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                                     StoreTools storeTools, AiTraceAdvisor traceAdvisor) {
        return businessAgent(chatModel, chatMemory, vectorStore, traceAdvisor,
                Prompts.FAQ_AGENT_PROMPT, storeTools, null);
    }

    // ===== 内部 =====

    /** 业务 Agent 装配模板：Prompt + 记忆 → RAG → 日志；工具只挂本 Agent 专属。 */
    private ChatClient businessAgent(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                                     AiTraceAdvisor traceAdvisor, String systemPrompt, Object tools,
                                     String filterExpression) {
        SearchRequest.Builder search = SearchRequest.builder()
                .topK(5).similarityThreshold(0.5);
        if (filterExpression != null) {
            search.filterExpression(filterExpression);
        }
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(tools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).order(20).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(search.build())
                                .order(30)
                                .build(),
                        traceAdvisor)
                .build();
    }
}
