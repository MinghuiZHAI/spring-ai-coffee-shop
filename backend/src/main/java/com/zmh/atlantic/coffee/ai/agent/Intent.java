package com.zmh.atlantic.coffee.ai.agent;

/** 意图五枚举（详细设计 §3.2）：FALLBACK 不走业务 Agent，由 AiSessionHandler 直出兜底。 */
public enum Intent {
    ORDER, MEMBER, RECOMMEND, KNOWLEDGE, FALLBACK
}
