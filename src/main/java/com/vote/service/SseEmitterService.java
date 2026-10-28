package com.vote.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 SseEmitter 的单向推送：浏览器用 EventSource 长连接，服务端有新消息时立即下发。
 * 选 SSE 而不是 WebSocket 的原因：本项目离线构建（mvnw -o），本地仓库没有 spring-websocket，
 * 而 SSE 由 spring-webmvc 原生提供，零新增依赖；私聊只需要服务端→客户端的单向推送。
 * 客户端断开/超时后 EventSource 会自动重连，重连时会重新注册 emitter。
 */
@Slf4j
@Service
public class SseEmitterService {

    /** 30 分钟超时，超时后浏览器会自动重连 */
    private static final long TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        // 同一用户开多个标签页时，后连的覆盖先连的；旧连接会在下次发送失败时被清理
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("userId", userId)));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }
        return emitter;
    }

    /** 给指定用户推送事件；用户不在线（未建立连接）时静默跳过，消息已落库不会丢 */
    public void send(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("用户 {} 未建立 SSE 连接，事件 {} 仅落库", userId, eventName);
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.warn("SSE 推送失败，移除连接: userId={}", userId, e);
            emitters.remove(userId, emitter);
        }
    }

    public int onlineCount() {
        return emitters.size();
    }
}
