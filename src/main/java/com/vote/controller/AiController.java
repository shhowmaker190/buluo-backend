package com.vote.controller;

import com.vote.common.Result;
import com.vote.dto.AiChatDTO;
import com.vote.entity.User;
import com.vote.service.AiService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    /** AI 小助手对话：DeepSeek → 免费 AI → 本地知识库，逐级降级 */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody AiChatDTO dto, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        try {
            return Result.success(aiService.chat(me.getId(), dto));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 当前生效的回答来源，前端用来显示角标 */
    @GetMapping("/status")
    public Result<?> status() {
        Map<String, Object> data = new HashMap<>();
        data.put("mode", aiService.currentMode());
        data.put("deepseek", aiService.isDeepSeekConfigured());
        return Result.success(data);
    }
}
