package com.zmh.atlantic.coffee.ai.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 聊天域请求结构。 */
public final class ChatDtos {

    private ChatDtos() {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 500) String content) {
    }
}
