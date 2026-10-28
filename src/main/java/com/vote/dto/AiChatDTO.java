package com.vote.dto;

import lombok.Data;

import java.util.List;

/** AI 助手对话请求 */
@Data
public class AiChatDTO {
    /** 历史消息（前端带上最近若干条），role: user / assistant */
    private List<Message> messages;
    /** 用户当前所在页面，给模型一点上下文（可选） */
    private String page;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
