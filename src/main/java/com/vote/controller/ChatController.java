package com.vote.controller;

import com.vote.common.Result;
import com.vote.config.JwtUtils;
import com.vote.entity.User;
import com.vote.service.ChatService;
import com.vote.service.SseEmitterService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SseEmitterService sseEmitterService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * SSE 长连接：EventSource 无法带自定义请求头，token 只能放 URL 参数，
     * 所以该路径在 SecurityConfig 里放行，token 校验在代码内手动完成。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(value = "token", required = false) String token) {
        User me = null;
        if (token != null && !token.isEmpty() && jwtUtils.validateToken(token)) {
            // 与 JwtAuthenticationFilter 保持一致：优先用 uid 定位用户。
            // 若按 subject（用户名）查，用户改名后旧 token 就查不到人，SSE 会一直连不上。
            me = userService.findById(jwtUtils.extractUserId(token));
            if (me == null) {
                me = userService.findByUsername(jwtUtils.extractUsername(token));
            }
        }
        if (me == null) {
            SseEmitter denied = new SseEmitter(1000L);
            denied.completeWithError(new RuntimeException("unauthorized"));
            return denied;
        }
        log.info("SSE 连接建立: userId={}", me.getId());
        return sseEmitterService.connect(me.getId());
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody Map<String, Object> body, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Object toUserId = body.get("toUserId");
        String content = (String) body.get("content");
        if (toUserId == null) return Result.error("缺少接收者");
        try {
            chatService.send(me.getId(), Long.valueOf(toUserId.toString()), content);
            return Result.success("已发送", null);
        } catch (NumberFormatException e) {
            return Result.error("接收者不合法");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 分享帖子 / 部落给好友，生成可点击跳转的卡片消息 */
    @PostMapping("/share")
    public Result<?> share(@RequestBody Map<String, Object> body, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Object toUserId = body.get("toUserId");
        Object refId = body.get("refId");
        String msgType = body.get("msgType") == null ? null : body.get("msgType").toString();
        if (toUserId == null || refId == null) return Result.error("缺少接收者或分享对象");
        try {
            chatService.sendShare(me.getId(), Long.valueOf(toUserId.toString()),
                    msgType, Long.valueOf(refId.toString()));
            return Result.success("已发送", null);
        } catch (NumberFormatException e) {
            return Result.error("参数不合法");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/conversations")
    public Result<?> conversations(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(chatService.conversations(me.getId()));
    }

    @GetMapping("/history/{userId}")
    public Result<?> history(@PathVariable Long userId,
                             @RequestParam(defaultValue = "200") int limit,
                             Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(chatService.history(me.getId(), userId, limit));
    }

    @PostMapping("/read/{userId}")
    public Result<?> read(@PathVariable Long userId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        chatService.markRead(me.getId(), userId);
        return Result.success(null);
    }

    @GetMapping("/unread")
    public Result<?> unread(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("count", chatService.unreadTotal(me.getId()));
        return Result.success(data);
    }

    /** 非好友时剩余可发条数：0 表示已被陌生人限额挡住 */
    @GetMapping("/quota/{userId}")
    public Result<?> quota(@PathVariable Long userId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("quota", chatService.strangerQuota(me.getId(), userId));
        return Result.success(data);
    }

    /** 删除会话（单向，仅自己不再看到；对方记录不受影响） */
    @DeleteMapping("/conversation/{userId}")
    public Result<?> deleteConversation(@PathVariable Long userId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        try {
            chatService.deleteConversation(me.getId(), userId);
            return Result.success("会话已删除", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除单条消息（单向，仅自己不再看到） */
    @DeleteMapping("/message/{messageId}")
    public Result<?> deleteMessage(@PathVariable Long messageId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        try {
            chatService.deleteMessage(me.getId(), messageId);
            return Result.success("消息已删除", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
