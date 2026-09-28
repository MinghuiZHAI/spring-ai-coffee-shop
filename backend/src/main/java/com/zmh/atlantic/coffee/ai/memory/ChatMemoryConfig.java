package com.zmh.atlantic.coffee.ai.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory 装配（详细设计 §3.4）：自定义仓储 + 内置 MessageWindowChatMemory，
 * 窗口大小取 atlantic.chat.memory-window（技术栈 #15，默认 20 条）。
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(MySqlRedisChatMemoryRepository repository,
                                 @Value("${atlantic.chat.memory-window:20}") int memoryWindow) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(memoryWindow)
                .build();
    }
}
