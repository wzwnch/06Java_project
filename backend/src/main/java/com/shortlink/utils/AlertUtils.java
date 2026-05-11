package com.shortlink.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AlertUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final Map<String, Long> ALERT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> ALERT_COUNT = new ConcurrentHashMap<>();
    
    private static final long ALERT_INTERVAL_MS = 60_000L;
    private static final int MAX_ALERT_COUNT = 10;

    private AlertUtils() {
    }

    public static void alertDatabaseError(String message, Throwable throwable) {
        String alertKey = "database_error";
        alert(alertKey, "数据库异常告警", message, throwable);
    }

    public static void alertRedisError(String message, Throwable throwable) {
        String alertKey = "redis_error";
        alert(alertKey, "Redis异常告警", message, throwable);
    }

    public static void alertSystemError(String message, Throwable throwable) {
        String alertKey = "system_error";
        alert(alertKey, "系统异常告警", message, throwable);
    }

    public static void alertQueueBacklog(String queueName, int backlogSize) {
        String alertKey = "queue_backlog_" + queueName;
        if (backlogSize > 10000) {
            alert(alertKey, "队列积压告警", 
                    String.format("队列 %s 积压数量: %d", queueName, backlogSize), null);
        }
    }

    private static void alert(String alertKey, String alertType, String message, Throwable throwable) {
        long now = System.currentTimeMillis();
        Long lastAlertTime = ALERT_CACHE.get(alertKey);
        
        if (lastAlertTime != null && (now - lastAlertTime) < ALERT_INTERVAL_MS) {
            AtomicInteger count = ALERT_COUNT.computeIfAbsent(alertKey, k -> new AtomicInteger(0));
            int currentCount = count.incrementAndGet();
            if (currentCount > MAX_ALERT_COUNT) {
                return;
            }
        } else {
            ALERT_CACHE.put(alertKey, now);
            ALERT_COUNT.put(alertKey, new AtomicInteger(1));
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        StringBuilder alertMessage = new StringBuilder();
        alertMessage.append("【").append(alertType).append("】\n");
        alertMessage.append("时间: ").append(timestamp).append("\n");
        alertMessage.append("详情: ").append(message).append("\n");
        
        if (throwable != null) {
            alertMessage.append("异常类型: ").append(throwable.getClass().getName()).append("\n");
            alertMessage.append("异常信息: ").append(throwable.getMessage());
        }

        log.error("告警通知: {}", alertMessage);

        sendAlert(alertType, alertMessage.toString());
    }

    private static void sendAlert(String alertType, String message) {
        log.warn("[告警发送] 类型: {}, 内容: {}", alertType, message);
    }

    public static void clearAlertCache() {
        ALERT_CACHE.clear();
        ALERT_COUNT.clear();
    }
}
