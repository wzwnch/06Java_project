package com.shortlink.producer;

import cn.hutool.json.JSONUtil;
import com.shortlink.dto.LinkAccessLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsLogProducer {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STATS_LOG_QUEUE_KEY = "stats:log:queue";

    public void sendAccessLog(LinkAccessLogDTO accessLog) {
        if (accessLog == null) {
            log.warn("访问日志为空，跳过发送");
            return;
        }

        try {
            String logJson = JSONUtil.toJsonStr(accessLog);
            stringRedisTemplate.opsForList().leftPush(STATS_LOG_QUEUE_KEY, logJson);
            log.debug("访问日志已推送到队列: shortCode={}, ip={}", accessLog.getShortCode(), accessLog.getIp());
        } catch (Exception e) {
            log.error("推送访问日志到Redis队列失败: shortCode={}", accessLog.getShortCode(), e);
        }
    }

    public void sendAccessLogAsync(LinkAccessLogDTO accessLog) {
        if (accessLog == null) {
            log.warn("访问日志为空，跳过发送");
            return;
        }

        try {
            String logJson = JSONUtil.toJsonStr(accessLog);
            stringRedisTemplate.opsForList().leftPush(STATS_LOG_QUEUE_KEY, logJson);
        } catch (Exception e) {
            log.error("异步推送访问日志到Redis队列失败: shortCode={}", accessLog.getShortCode(), e);
        }
    }

    public Long getQueueSize() {
        try {
            Long size = stringRedisTemplate.opsForList().size(STATS_LOG_QUEUE_KEY);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("获取队列大小失败", e);
            return 0L;
        }
    }
}
