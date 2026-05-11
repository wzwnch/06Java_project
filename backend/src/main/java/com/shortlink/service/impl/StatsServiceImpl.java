package com.shortlink.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.dto.LinkAccessLogDTO;
import com.shortlink.dto.req.StatsLogPageReqDTO;
import com.shortlink.dto.resp.StatsHistoryRespDTO;
import com.shortlink.dto.resp.StatsRespDTO;
import com.shortlink.dto.resp.StatsTodayRespDTO;
import com.shortlink.entity.LinkStatsDO;
import com.shortlink.entity.LinkStatsTodayDO;
import com.shortlink.mapper.StatsMapper;
import com.shortlink.producer.StatsLogProducer;
import com.shortlink.service.StatsService;
import com.shortlink.utils.IpUtils;
import com.shortlink.utils.UserAgentUtils;
import com.shortlink.utils.UvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;
    private final StatsLogProducer statsLogProducer;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String STATS_TODAY_KEY_PREFIX = "stats:today:";
    private static final String HIGH_FREQ_IP_KEY_PREFIX = "stats:highfreq:";
    private static final int MAX_HISTORY_DAYS = 30;
    private static final int DEFAULT_HIGH_FREQ_LIMIT = 10;

    @Override
    public void recordAccessLog(String shortCode, String gid, HttpServletRequest request) {
        if (StrUtil.isBlank(shortCode)) {
            log.warn("短链接码为空，跳过记录访问日志");
            return;
        }

        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        IpUtils.IpInfo ipInfo = IpUtils.parse(ip);
        String region = ipInfo.getProvince();
        if ("未知".equals(region)) {
            region = ipInfo.getCountry();
        }

        UserAgentUtils.UaInfo uaInfo = UserAgentUtils.parse(userAgent);
        String browser = uaInfo.getBrowser();
        String os = uaInfo.getOs();
        String device = uaInfo.getDevice();
        String network = uaInfo.getNetwork();

        String uv = UvUtils.generateUvId(ip, userAgent);
        String uip = UvUtils.generateUipId(ip);

        LinkAccessLogDTO accessLog = LinkAccessLogDTO.create(
                shortCode, gid, ip, uv, uip, region, os, browser, device, network
        );

        statsLogProducer.sendAccessLogAsync(accessLog);

        recordHighFreqIp(shortCode, ip);

        log.debug("访问日志已记录: shortCode={}, ip={}, region={}", shortCode, ip, region);
    }

    @Override
    public void updateStats(LinkAccessLogDTO accessLog) {
        if (accessLog == null || StrUtil.isBlank(accessLog.getShortCode())) {
            log.warn("访问日志为空或短链接码为空，跳过更新统计");
            return;
        }

        String shortCode = accessLog.getShortCode();
        String gid = accessLog.getGid();
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        LinkStatsTodayDO existingStats = statsMapper.selectStatsTodayByShortCodeAndDate(shortCode, todayStr);

        if (existingStats == null) {
            LinkStatsTodayDO newStats = new LinkStatsTodayDO();
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
                log.warn("插入今日统计失败，可能已存在，尝试更新: shortCode={}", shortCode);
                incrementStatsToday(shortCode, accessLog);
            }
        } else {
            incrementStatsToday(shortCode, accessLog);
        }
    }

    @Override
    public StatsRespDTO getLinkStats(String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            return buildEmptyStatsRespDTO();
        }

        List<LinkStatsTodayDO> statsList = statsMapper.selectStatsTodayByShortCode(shortCode);

        if (statsList == null || statsList.isEmpty()) {
            return buildEmptyStatsRespDTO();
        }

        long totalPv = 0L;
        long totalUv = 0L;
        long totalUip = 0L;
        String gid = null;

        for (LinkStatsTodayDO stats : statsList) {
            totalPv += stats.getPv() != null ? stats.getPv() : 0L;
            totalUv += stats.getUv() != null ? stats.getUv() : 0L;
            totalUip += stats.getUip() != null ? stats.getUip() : 0L;
            if (gid == null && StrUtil.isNotBlank(stats.getGid())) {
                gid = stats.getGid();
            }
        }

        StatsRespDTO respDTO = new StatsRespDTO();
        respDTO.setShortCode(shortCode);
        respDTO.setGid(gid);
        respDTO.setPv(totalPv);
        respDTO.setUv(totalUv);
        respDTO.setUip(totalUip);

        return respDTO;
    }

    @Override
    public Page<LinkStatsDO> pageAccessLog(StatsLogPageReqDTO request) {
        if (request == null) {
            return new Page<>();
        }

        String shortCode = request.getShortCode();
        String gid = request.getGid();

        if (StrUtil.isBlank(shortCode) && StrUtil.isBlank(gid)) {
            return new Page<>();
        }

        Page<LinkStatsDO> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<LinkStatsDO> queryWrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(shortCode)) {
            queryWrapper.eq(LinkStatsDO::getShortCode, shortCode);
        }

        if (StrUtil.isNotBlank(gid)) {
            queryWrapper.eq(LinkStatsDO::getGid, gid);
        }

        if (request.getStartTime() != null) {
            queryWrapper.ge(LinkStatsDO::getCreateTime, request.getStartTime());
        }

        if (request.getEndTime() != null) {
            queryWrapper.le(LinkStatsDO::getCreateTime, request.getEndTime());
        }

        queryWrapper.orderByDesc(LinkStatsDO::getCreateTime);

        return statsMapper.selectPage(page, queryWrapper);
    }

    @Override
    public StatsTodayRespDTO getTodayStats(String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            return buildEmptyTodayRespDTO();
        }

        String todayStr = LocalDate.now().toString();
        LinkStatsTodayDO statsToday = statsMapper.selectStatsTodayByShortCodeAndDate(shortCode, todayStr);

        if (statsToday == null) {
            return buildEmptyTodayRespDTO();
        }

        StatsTodayRespDTO respDTO = new StatsTodayRespDTO();
        respDTO.setDate(statsToday.getDate());
        respDTO.setPv(statsToday.getPv() != null ? statsToday.getPv() : 0L);
        respDTO.setUv(statsToday.getUv() != null ? statsToday.getUv() : 0L);
        respDTO.setUip(statsToday.getUip() != null ? statsToday.getUip() : 0L);

        return respDTO;
    }

    @Override
    public List<StatsHistoryRespDTO> getHistoryStats(String shortCode, String startDate, String endDate) {
        if (StrUtil.isBlank(shortCode)) {
            return buildEmptyHistoryList(LocalDate.now().minusDays(6), LocalDate.now());
        }

        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        if (start == null || end == null) {
            end = LocalDate.now();
            start = end.minusDays(7);
        }

        if (start.isAfter(end)) {
            throw new BizException(BizCodeEnum.STATS_TIME_RANGE_ERROR.getCode(), "开始时间不能晚于结束时间");
        }

        long daysBetween = ChronoUnit.DAYS.between(start, end);
        if (daysBetween > MAX_HISTORY_DAYS) {
            throw new BizException(BizCodeEnum.STATS_TIME_RANGE_ERROR.getCode(), 
                    BizCodeEnum.STATS_TIME_RANGE_ERROR.getMessage());
        }

        List<LinkStatsTodayDO> statsList = statsMapper.selectStatsTodayByShortCode(shortCode);

        if (statsList == null || statsList.isEmpty()) {
            return buildEmptyHistoryList(start, end);
        }

        Map<LocalDate, LinkStatsTodayDO> statsMap = statsList.stream()
                .collect(Collectors.toMap(LinkStatsTodayDO::getDate, s -> s, (a, b) -> a));

        List<StatsHistoryRespDTO> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            StatsHistoryRespDTO dto = new StatsHistoryRespDTO();
            dto.setDate(current);

            LinkStatsTodayDO stats = statsMap.get(current);
            if (stats != null) {
                dto.setPv(stats.getPv() != null ? stats.getPv() : 0L);
                dto.setUv(stats.getUv() != null ? stats.getUv() : 0L);
                dto.setUip(stats.getUip() != null ? stats.getUip() : 0L);
            } else {
                dto.setPv(0L);
                dto.setUv(0L);
                dto.setUip(0L);
            }

            result.add(dto);
            current = current.plusDays(1);
        }

        return result;
    }

    @Override
    public StatsRespDTO getGroupStats(String gid) {
        if (StrUtil.isBlank(gid)) {
            return buildEmptyStatsRespDTO();
        }

        List<LinkStatsTodayDO> statsList = statsMapper.selectStatsTodayByGid(gid);

        if (statsList == null || statsList.isEmpty()) {
            return buildEmptyStatsRespDTO();
        }

        long totalPv = 0L;
        long totalUv = 0L;
        long totalUip = 0L;

        for (LinkStatsTodayDO stats : statsList) {
            totalPv += stats.getPv() != null ? stats.getPv() : 0L;
            totalUv += stats.getUv() != null ? stats.getUv() : 0L;
            totalUip += stats.getUip() != null ? stats.getUip() : 0L;
        }

        StatsRespDTO respDTO = new StatsRespDTO();
        respDTO.setGid(gid);
        respDTO.setPv(totalPv);
        respDTO.setUv(totalUv);
        respDTO.setUip(totalUip);

        return respDTO;
    }

    @Override
    public List<String> getHighFreqIp(String shortCode, Integer limit) {
        if (StrUtil.isBlank(shortCode)) {
            return Collections.emptyList();
        }

        if (limit == null || limit <= 0) {
            limit = DEFAULT_HIGH_FREQ_LIMIT;
        }

        String key = HIGH_FREQ_IP_KEY_PREFIX + shortCode + ":" + LocalDate.now();

        Set<String> topIps = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);

        if (topIps == null || topIps.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(topIps);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "127.0.0.1";
    }

    private void recordHighFreqIp(String shortCode, String ip) {
        if (StrUtil.isBlank(ip)) {
            return;
        }

        String key = HIGH_FREQ_IP_KEY_PREFIX + shortCode + ":" + LocalDate.now();
        stringRedisTemplate.opsForZSet().incrementScore(key, ip, 1);
        stringRedisTemplate.expire(key, 2, TimeUnit.DAYS);
    }

    private boolean isNewUv(String shortCode, String uv) {
        if (StrUtil.isBlank(uv)) {
            return false;
        }

        String uvKey = STATS_TODAY_KEY_PREFIX + shortCode + ":uv:" + LocalDate.now();
        Long added = stringRedisTemplate.opsForSet().add(uvKey, uv);

        if (added != null && added > 0) {
            stringRedisTemplate.expire(uvKey, 2, TimeUnit.DAYS);
            return true;
        }
        return false;
    }

    private boolean isNewUip(String shortCode, String uip) {
        if (StrUtil.isBlank(uip)) {
            return false;
        }

        String uipKey = STATS_TODAY_KEY_PREFIX + shortCode + ":uip:" + LocalDate.now();
        Long added = stringRedisTemplate.opsForSet().add(uipKey, uip);

        if (added != null && added > 0) {
            stringRedisTemplate.expire(uipKey, 2, TimeUnit.DAYS);
            return true;
        }
        return false;
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

    private StatsRespDTO buildEmptyStatsRespDTO() {
        StatsRespDTO respDTO = new StatsRespDTO();
        respDTO.setPv(0L);
        respDTO.setUv(0L);
        respDTO.setUip(0L);
        return respDTO;
    }

    private StatsTodayRespDTO buildEmptyTodayRespDTO() {
        StatsTodayRespDTO respDTO = new StatsTodayRespDTO();
        respDTO.setDate(LocalDate.now());
        respDTO.setPv(0L);
        respDTO.setUv(0L);
        respDTO.setUip(0L);
        return respDTO;
    }

    private LocalDate parseDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            log.warn("日期格式解析失败: {}", dateStr);
            return null;
        }
    }

    private List<StatsHistoryRespDTO> buildEmptyHistoryList(LocalDate start, LocalDate end) {
        List<StatsHistoryRespDTO> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            StatsHistoryRespDTO dto = new StatsHistoryRespDTO();
            dto.setDate(current);
            dto.setPv(0L);
            dto.setUv(0L);
            dto.setUip(0L);
            result.add(dto);
            current = current.plusDays(1);
        }
        return result;
    }
}
