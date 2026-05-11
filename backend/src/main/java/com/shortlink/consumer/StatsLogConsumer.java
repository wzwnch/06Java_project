package com.shortlink.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.shortlink.dto.LinkAccessLogDTO;
import com.shortlink.entity.LinkStatsDO;
import com.shortlink.entity.LinkStatsTodayDO;
import com.shortlink.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsLogConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final StatsMapper statsMapper;

    private static final String STATS_LOG_QUEUE_KEY = "stats:log:queue";
    private static final int BATCH_SIZE = 100;
    private static final String STATS_TODAY_KEY_PREFIX = "stats:today:";

    private static final int QUEUE_WARNING_THRESHOLD = 1000;
    private static final int QUEUE_CRITICAL_THRESHOLD = 5000;

    private static final long ALERT_INTERVAL_MS = 60000L;
    private static final AtomicLong lastAlertTime = new AtomicLong(0);
    private static final AtomicBoolean alerting = new AtomicBoolean(false);

    @Scheduled(fixedRate = 100)
    public void consumeAccessLogs() {
        try {
            checkQueueBacklog();

            List<String> logJsonList = batchPopLogs();
            
            if (CollUtil.isEmpty(logJsonList)) {
                return;
            }

            List<LinkAccessLogDTO> accessLogs = parseLogs(logJsonList);
            
            if (CollUtil.isEmpty(accessLogs)) {
                return;
            }

            processAccessLogs(accessLogs);
            
        } catch (Exception e) {
            log.error("消费访问日志失败", e);
        }
    }

    private void checkQueueBacklog() {
        try {
            Long queueSize = stringRedisTemplate.opsForList().size(STATS_LOG_QUEUE_KEY);
            if (queueSize == null) {
                return;
            }

            if (queueSize >= QUEUE_CRITICAL_THRESHOLD) {
                sendAlert("严重", queueSize);
            } else if (queueSize >= QUEUE_WARNING_THRESHOLD) {
                sendAlert("警告", queueSize);
            } else {
                if (alerting.compareAndSet(true, false)) {
                    log.info("Redis队列积压已恢复正常，当前队列大小: {}", queueSize);
                }
            }
        } catch (Exception e) {
            log.error("检测队列积压失败", e);
        }
    }

    private void sendAlert(String level, Long queueSize) {
        long now = System.currentTimeMillis();
        long lastAlert = lastAlertTime.get();

        if (now - lastAlert < ALERT_INTERVAL_MS) {
            return;
        }

        if (lastAlertTime.compareAndSet(lastAlert, now)) {
            alerting.set(true);
            log.warn("[{}] Redis队列积压告警 - 队列大小: {}, 阈值: 警告={}, 严重={}", 
                    level, queueSize, QUEUE_WARNING_THRESHOLD, QUEUE_CRITICAL_THRESHOLD);
        }
    }

    private List<String> batchPopLogs() {
        List<String> logJsonList = new ArrayList<>();
        
        for (int i = 0; i < BATCH_SIZE; i++) {
            String logJson = stringRedisTemplate.opsForList().rightPop(STATS_LOG_QUEUE_KEY);
            if (StrUtil.isBlank(logJson)) {
                break;
            }
            logJsonList.add(logJson);
        }
        
        return logJsonList;
    }

    private List<LinkAccessLogDTO> parseLogs(List<String> logJsonList) {
        List<LinkAccessLogDTO> accessLogs = new ArrayList<>();
        
        for (String logJson : logJsonList) {
            try {
                LinkAccessLogDTO accessLog = JSONUtil.toBean(logJson, LinkAccessLogDTO.class);
                if (accessLog != null && StrUtil.isNotBlank(accessLog.getShortCode())) {
                    accessLogs.add(accessLog);
                }
            } catch (Exception e) {
                log.error("解析访问日志JSON失败: {}", logJson, e);
            }
        }
        
        return accessLogs;
    }

    private void processAccessLogs(List<LinkAccessLogDTO> accessLogs) {
        for (LinkAccessLogDTO accessLog : accessLogs) {
            try {
                saveAccessLog(accessLog);
                updateStatsToday(accessLog);
            } catch (Exception e) {
                log.error("处理访问日志失败: shortCode={}", accessLog.getShortCode(), e);
            }
        }
    }

    private void saveAccessLog(LinkAccessLogDTO accessLog) {
        LinkStatsDO statsDO = new LinkStatsDO();
        statsDO.setShortCode(accessLog.getShortCode());
        statsDO.setGid(accessLog.getGid());
        statsDO.setPv(accessLog.getPv());
        statsDO.setUv(accessLog.getUv());
        statsDO.setUip(accessLog.getUip());
        statsDO.setIp(accessLog.getIp());
        statsDO.setRegion(accessLog.getRegion());
        statsDO.setOs(accessLog.getOs());
        statsDO.setBrowser(accessLog.getBrowser());
        statsDO.setDevice(accessLog.getDevice());
        statsDO.setNetwork(accessLog.getNetwork());
        statsDO.setCreateTime(accessLog.getCreateTime() != null ? accessLog.getCreateTime() : LocalDateTime.now());
        
        statsMapper.insert(statsDO);
    }

    private void updateStatsToday(LinkAccessLogDTO accessLog) {
        String shortCode = accessLog.getShortCode();
        String gid = accessLog.getGid();
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        
        LinkStatsTodayDO existingStats = statsMapper.selectStatsTodayByShortCodeAndDate(shortCode, todayStr);
        
        if (existingStats == null) {
            LinkStatsTodayDO newStats = new LinkStatsTodayDO();
            newStats.setId(IdUtil.getSnowflakeNextId());
            newStats.setShortCode(shortCode);
            newStats.setGid(gid);
            newStats.setDate(today);
            newStats.setPv(1L);
            newStats.setUv(isNewUv(shortCode, accessLog.getUv()) ? 1L : 0L);
            newStats.setUip(isNewUip(shortCode, accessLog.getUip()) ? 1L : 0L);
            newStats.setCreateTime(LocalDateTime.now());
            newStats.setUpdateTime(LocalDateTime.now());
            
            try {
                statsMapper.insertStatsToday(newStats);
            } catch (Exception e) {
                log.warn("插入今日统计失败，可能已存在: shortCode={}, date={}", shortCode, todayStr);
                incrementStatsToday(shortCode, accessLog);
            }
        } else {
            incrementStatsToday(shortCode, accessLog);
        }
    }

    private void incrementStatsToday(String shortCode, LinkAccessLogDTO accessLog) {
        LinkStatsTodayDO updateStats = new LinkStatsTodayDO();
        updateStats.setShortCode(shortCode);
        updateStats.setDate(LocalDate.now());
        updateStats.setPv(1L);
        updateStats.setUv(isNewUv(shortCode, accessLog.getUv()) ? 1L : 0L);
        updateStats.setUip(isNewUip(shortCode, accessLog.getUip()) ? 1L : 0L);
        updateStats.setUpdateTime(LocalDateTime.now());
        
        statsMapper.updateStatsToday(updateStats);
    }

    private boolean isNewUv(String shortCode, String uv) {
        if (StrUtil.isBlank(uv)) {
            return false;
        }
        
        String uvKey = STATS_TODAY_KEY_PREFIX + shortCode + ":uv:" + LocalDate.now();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(uvKey, uv);
        
        if (Boolean.TRUE.equals(isMember)) {
            return false;
        }
        
        stringRedisTemplate.opsForSet().add(uvKey, uv);
        stringRedisTemplate.expireAt(uvKey, LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return true;
    }

    private boolean isNewUip(String shortCode, String uip) {
        if (StrUtil.isBlank(uip)) {
            return false;
        }
        
        String uipKey = STATS_TODAY_KEY_PREFIX + shortCode + ":uip:" + LocalDate.now();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(uipKey, uip);
        
        if (Boolean.TRUE.equals(isMember)) {
            return false;
        }
        
        stringRedisTemplate.opsForSet().add(uipKey, uip);
        stringRedisTemplate.expireAt(uipKey, LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return true;
    }
}
