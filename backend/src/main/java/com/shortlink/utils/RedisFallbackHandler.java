package com.shortlink.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RedisFallbackHandler {

    private static final AtomicBoolean REDIS_AVAILABLE = new AtomicBoolean(true);
    private static final long RETRY_INTERVAL_MS = 30_000L;
    private static volatile long lastRetryTime = 0;

    private RedisFallbackHandler() {
    }

    public static boolean isRedisAvailable() {
        return REDIS_AVAILABLE.get();
    }

    public static void markRedisUnavailable() {
        if (REDIS_AVAILABLE.compareAndSet(true, false)) {
            log.error("Redis标记为不可用，将启用降级模式");
            AlertUtils.alertRedisError("Redis连接失败，已启用降级模式", null);
        }
    }

    public static void markRedisAvailable() {
        if (REDIS_AVAILABLE.compareAndSet(false, true)) {
            log.info("Redis已恢复可用");
        }
    }

    public static boolean shouldRetryConnection() {
        long now = System.currentTimeMillis();
        if (now - lastRetryTime > RETRY_INTERVAL_MS) {
            lastRetryTime = now;
            return true;
        }
        return false;
    }

    public static <T> T executeWithFallback(RedisTemplate<String, Object> redisTemplate, 
                                             StringRedisTemplate stringRedisTemplate,
                                             RedisOperation<T> operation,
                                             FallbackOperation<T> fallback) {
        if (!isRedisAvailable()) {
            if (shouldRetryConnection()) {
                try {
                    redisTemplate.hasKey("test:connection");
                    markRedisAvailable();
                } catch (Exception e) {
                    log.debug("Redis重试连接失败: {}", e.getMessage());
                }
            }
            
            if (!isRedisAvailable()) {
                return fallback.execute();
            }
        }

        try {
            return operation.execute(redisTemplate, stringRedisTemplate);
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable();
            log.warn("Redis连接失败，启用降级模式: {}", e.getMessage());
            return fallback.execute();
        } catch (Exception e) {
            log.error("Redis操作异常: {}", e.getMessage());
            return fallback.execute();
        }
    }

    public static void executeWithFallbackVoid(RedisTemplate<String, Object> redisTemplate,
                                                StringRedisTemplate stringRedisTemplate,
                                                RedisOperationVoid operation,
                                                Runnable fallback) {
        if (!isRedisAvailable()) {
            if (shouldRetryConnection()) {
                try {
                    redisTemplate.hasKey("test:connection");
                    markRedisAvailable();
                } catch (Exception e) {
                    log.debug("Redis重试连接失败: {}", e.getMessage());
                }
            }
            
            if (!isRedisAvailable()) {
                fallback.run();
                return;
            }
        }

        try {
            operation.execute(redisTemplate, stringRedisTemplate);
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable();
            log.warn("Redis连接失败，启用降级模式: {}", e.getMessage());
            fallback.run();
        } catch (Exception e) {
            log.error("Redis操作异常: {}", e.getMessage());
            fallback.run();
        }
    }

    @FunctionalInterface
    public interface RedisOperation<T> {
        T execute(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate);
    }

    @FunctionalInterface
    public interface RedisOperationVoid {
        void execute(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate);
    }

    @FunctionalInterface
    public interface FallbackOperation<T> {
        T execute();
    }
}
