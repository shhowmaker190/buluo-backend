package com.vote.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 统一缓存入口。
 *
 * 设计要点：
 * 1. 优先用 Redis（分布式、多实例共享），值统一序列化成 JSON，避免 JDK 序列化不可读；
 * 2. Redis 挂了不能让业务跟着挂 —— catch 到连接异常就降级到本地 ConcurrentHashMap，
 *    并熔断 30 秒后再探活（避免每个请求都去撞一次超时）；
 * 3. 只有「读多写少、短时间不一致可接受」的数据才缓存，写操作后主动 evict。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    /** Redis 失败后的熔断时间，期间直接走本地缓存，不再尝试连接 */
    private static final long REDIS_RETRY_AFTER_MS = 30_000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.redis.enabled:true}")
    private boolean redisEnabled;

    /** 降级用的本地缓存：key -> {值, 过期时间戳} */
    private final Map<String, LocalEntry> local = new ConcurrentHashMap<>();
    private record LocalEntry(String json, long expireAt) {
    }

    private volatile long redisUnavailableUntil = 0L;

    /**
     * 读缓存：命中返回对象，未命中执行 loader 并回填。
     * loader 返回 null 时不缓存（避免把"查不到"缓存成空）。
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) return cached;

        T value = loader.get();
        if (value != null) put(key, value, ttl);
        return value;
    }

    /** 泛型重载：缓存 List<Post> 这类集合必须用 TypeReference，否则会丢泛型 */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) return cached;

        T value = loader.get();
        if (value != null) put(key, value, ttl);
        return value;
    }

    public <T> T get(String key, Class<T> type) {
        String json = read(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，按未命中处理: {}", key, e);
            evict(key);
            return null;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        String json = read(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，按未命中处理: {}", key, e);
            evict(key);
            return null;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (value == null) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("缓存序列化失败: {}", key, e);
            return;
        }
        write(key, json, ttl);
    }

    /** 写操作后主动失效，保证「缓存里的数据一定不比数据库旧」 */
    public void evict(String key) {
        if (key == null) return;
        local.remove(key);
        if (redisAvailable()) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    /** 按前缀批量失效（例如某个部落的所有榜单缓存） */
    public void evictByPrefix(String prefix) {
        local.keySet().removeIf(k -> k.startsWith(prefix));
        if (redisAvailable()) {
            try {
                var keys = redisTemplate.keys(prefix + "*");
                if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    private String read(String key) {
        if (redisAvailable()) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) return value;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        LocalEntry entry = local.get(key);
        if (entry == null) return null;
        if (entry.expireAt() < System.currentTimeMillis()) {
            local.remove(key);
            return null;
        }
        return entry.json();
    }

    private void write(String key, String json, Duration ttl) {
        if (redisAvailable()) {
            try {
                redisTemplate.opsForValue().set(key, json, ttl);
                return;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.put(key, new LocalEntry(json, System.currentTimeMillis() + ttl.toMillis()));
        cleanLocal();
    }

    private boolean redisAvailable() {
        return redisEnabled && System.currentTimeMillis() > redisUnavailableUntil;
    }

    private void markRedisDown(Exception e) {
        redisUnavailableUntil = System.currentTimeMillis() + REDIS_RETRY_AFTER_MS;
        log.warn("Redis 不可用，接下来 30 秒降级为本地缓存：{}", e.getMessage());
    }

    /** 本地缓存兜底清理，防止 Map 无限增长 */
    private void cleanLocal() {
        if (local.size() < 500) return;
        long now = System.currentTimeMillis();
        local.entrySet().removeIf(e -> e.getValue().expireAt() < now);
    }
}
