package com.shortlink.service.impl;

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
import com.shortlink.utils.IpUtils;
import com.shortlink.utils.UserAgentUtils;
import com.shortlink.utils.UvUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("统计服务单元测试")
class StatsServiceImplTest {

    @Mock
    private StatsMapper statsMapper;

    @Mock
    private StatsLogProducer statsLogProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private StatsServiceImpl statsService;

    private static final String TEST_SHORT_CODE = "abc123";
    private static final String TEST_GID = "testgid01";
    private static final String TEST_IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Nested
    @DisplayName("记录访问日志方法测试")
    class RecordAccessLogTest {

        @Test
        @DisplayName("正常记录 - 成功")
        void recordAccessLog_Success() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(TEST_IP);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

            try (MockedStatic<IpUtils> ipUtilsMock = mockStatic(IpUtils.class);
                 MockedStatic<UserAgentUtils> uaUtilsMock = mockStatic(UserAgentUtils.class);
                 MockedStatic<UvUtils> uvUtilsMock = mockStatic(UvUtils.class)) {

                IpUtils.IpInfo ipInfo = new IpUtils.IpInfo("中国", "北京", "北京", "电信");
                ipUtilsMock.when(() -> IpUtils.parse(TEST_IP)).thenReturn(ipInfo);

                UserAgentUtils.UaInfo uaInfo = new UserAgentUtils.UaInfo("Chrome", "Windows", "电脑", "WiFi");
                uaUtilsMock.when(() -> UserAgentUtils.parse(anyString())).thenReturn(uaInfo);

                uvUtilsMock.when(() -> UvUtils.generateUvId(anyString(), anyString())).thenReturn("uv123");
                uvUtilsMock.when(() -> UvUtils.generateUipId(anyString())).thenReturn("uip123");

                assertDoesNotThrow(() -> statsService.recordAccessLog(TEST_SHORT_CODE, TEST_GID, request));

                verify(statsLogProducer).sendAccessLogAsync(any(LinkAccessLogDTO.class));
            }
        }

        @Test
        @DisplayName("记录失败 - 短链接码为空")
        void recordAccessLog_Fail_EmptyShortCode() {
            assertDoesNotThrow(() -> statsService.recordAccessLog(null, TEST_GID, request));

            verify(statsLogProducer, never()).sendAccessLogAsync(any());
        }
    }

    @Nested
    @DisplayName("更新统计数据方法测试")
    class UpdateStatsTest {

        @Test
        @DisplayName("正常更新 - 新增今日统计")
        void updateStats_Success_InsertNew() {
            LinkAccessLogDTO accessLog = new LinkAccessLogDTO();
            accessLog.setShortCode(TEST_SHORT_CODE);
            accessLog.setGid(TEST_GID);
            accessLog.setUv("uv123");
            accessLog.setUip("uip123");

            when(statsMapper.selectStatsTodayByShortCodeAndDate(eq(TEST_SHORT_CODE), anyString())).thenReturn(null);
            when(setOperations.add(anyString(), anyString())).thenReturn(1L);
            doNothing().when(statsMapper).insertStatsToday(any(LinkStatsTodayDO.class));

            assertDoesNotThrow(() -> statsService.updateStats(accessLog));

            verify(statsMapper).insertStatsToday(any(LinkStatsTodayDO.class));
        }

        @Test
        @DisplayName("正常更新 - 更新今日统计")
        void updateStats_Success_UpdateExisting() {
            LinkAccessLogDTO accessLog = new LinkAccessLogDTO();
            accessLog.setShortCode(TEST_SHORT_CODE);
            accessLog.setGid(TEST_GID);
            accessLog.setUv("uv123");
            accessLog.setUip("uip123");

            LinkStatsTodayDO existingStats = new LinkStatsTodayDO();
            existingStats.setShortCode(TEST_SHORT_CODE);
            existingStats.setDate(LocalDate.now());
            existingStats.setPv(10L);
            existingStats.setUv(5L);
            existingStats.setUip(3L);

            when(statsMapper.selectStatsTodayByShortCodeAndDate(eq(TEST_SHORT_CODE), anyString())).thenReturn(existingStats);
            when(setOperations.add(anyString(), anyString())).thenReturn(1L);
            doNothing().when(statsMapper).updateStatsToday(any(LinkStatsTodayDO.class));

            assertDoesNotThrow(() -> statsService.updateStats(accessLog));

            verify(statsMapper).updateStatsToday(any(LinkStatsTodayDO.class));
        }

        @Test
        @DisplayName("更新失败 - 访问日志为空")
        void updateStats_Fail_NullAccessLog() {
            assertDoesNotThrow(() -> statsService.updateStats(null));

            verify(statsMapper, never()).insertStatsToday(any());
            verify(statsMapper, never()).updateStatsToday(any());
        }

        @Test
        @DisplayName("更新失败 - 短链接码为空")
        void updateStats_Fail_EmptyShortCode() {
            LinkAccessLogDTO accessLog = new LinkAccessLogDTO();

            assertDoesNotThrow(() -> statsService.updateStats(accessLog));

            verify(statsMapper, never()).insertStatsToday(any());
        }
    }

    @Nested
    @DisplayName("获取单链接统计方法测试")
    class GetLinkStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getLinkStats_Success() {
            List<LinkStatsTodayDO> statsList = new ArrayList<>();
            LinkStatsTodayDO stats = new LinkStatsTodayDO();
            stats.setShortCode(TEST_SHORT_CODE);
            stats.setGid(TEST_GID);
            stats.setDate(LocalDate.now());
            stats.setPv(100L);
            stats.setUv(50L);
            stats.setUip(30L);
            statsList.add(stats);

            when(statsMapper.selectStatsTodayByShortCode(TEST_SHORT_CODE)).thenReturn(statsList);

            StatsRespDTO result = statsService.getLinkStats(TEST_SHORT_CODE);

            assertNotNull(result);
            assertEquals(TEST_SHORT_CODE, result.getShortCode());
            assertEquals(100L, result.getPv());
            assertEquals(50L, result.getUv());
            assertEquals(30L, result.getUip());
        }

        @Test
        @DisplayName("查询成功 - 空数据返回零值")
        void getLinkStats_Success_EmptyData() {
            when(statsMapper.selectStatsTodayByShortCode(TEST_SHORT_CODE)).thenReturn(Collections.emptyList());

            StatsRespDTO result = statsService.getLinkStats(TEST_SHORT_CODE);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
            assertEquals(0L, result.getUv());
            assertEquals(0L, result.getUip());
        }

        @Test
        @DisplayName("查询成功 - 短链接码为空返回零值")
        void getLinkStats_Success_EmptyShortCode() {
            StatsRespDTO result = statsService.getLinkStats(null);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
        }
    }

    @Nested
    @DisplayName("获取今日统计方法测试")
    class GetTodayStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getTodayStats_Success() {
            LinkStatsTodayDO statsToday = new LinkStatsTodayDO();
            statsToday.setShortCode(TEST_SHORT_CODE);
            statsToday.setDate(LocalDate.now());
            statsToday.setPv(50L);
            statsToday.setUv(25L);
            statsToday.setUip(15L);

            when(statsMapper.selectStatsTodayByShortCodeAndDate(eq(TEST_SHORT_CODE), anyString())).thenReturn(statsToday);

            StatsTodayRespDTO result = statsService.getTodayStats(TEST_SHORT_CODE);

            assertNotNull(result);
            assertEquals(50L, result.getPv());
            assertEquals(25L, result.getUv());
            assertEquals(15L, result.getUip());
        }

        @Test
        @DisplayName("查询成功 - 无数据返回零值")
        void getTodayStats_Success_NoData() {
            when(statsMapper.selectStatsTodayByShortCodeAndDate(eq(TEST_SHORT_CODE), anyString())).thenReturn(null);

            StatsTodayRespDTO result = statsService.getTodayStats(TEST_SHORT_CODE);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
            assertEquals(0L, result.getUv());
            assertEquals(0L, result.getUip());
        }

        @Test
        @DisplayName("查询成功 - 短链接码为空返回零值")
        void getTodayStats_Success_EmptyShortCode() {
            StatsTodayRespDTO result = statsService.getTodayStats(null);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
        }
    }

    @Nested
    @DisplayName("获取历史统计方法测试")
    class GetHistoryStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getHistoryStats_Success() {
            LocalDate start = LocalDate.now().minusDays(6);
            LocalDate end = LocalDate.now();

            List<LinkStatsTodayDO> statsList = new ArrayList<>();
            LinkStatsTodayDO stats = new LinkStatsTodayDO();
            stats.setShortCode(TEST_SHORT_CODE);
            stats.setDate(LocalDate.now());
            stats.setPv(10L);
            stats.setUv(5L);
            stats.setUip(3L);
            statsList.add(stats);

            when(statsMapper.selectStatsTodayByShortCode(TEST_SHORT_CODE)).thenReturn(statsList);

            List<StatsHistoryRespDTO> result = statsService.getHistoryStats(TEST_SHORT_CODE, start.toString(), end.toString());

            assertNotNull(result);
            assertEquals(7, result.size());
        }

        @Test
        @DisplayName("查询失败 - 时间范围超过限制")
        void getHistoryStats_Fail_TimeRangeExceeded() {
            LocalDate start = LocalDate.now().minusDays(40);
            LocalDate end = LocalDate.now();

            BizException exception = assertThrows(BizException.class, 
                    () -> statsService.getHistoryStats(TEST_SHORT_CODE, start.toString(), end.toString()));

            assertEquals(BizCodeEnum.STATS_TIME_RANGE_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("查询失败 - 开始时间晚于结束时间")
        void getHistoryStats_Fail_StartAfterEnd() {
            LocalDate start = LocalDate.now();
            LocalDate end = LocalDate.now().minusDays(5);

            BizException exception = assertThrows(BizException.class,
                    () -> statsService.getHistoryStats(TEST_SHORT_CODE, start.toString(), end.toString()));

            assertEquals(BizCodeEnum.STATS_TIME_RANGE_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("查询成功 - 短链接码为空返回空列表")
        void getHistoryStats_Success_EmptyShortCode() {
            List<StatsHistoryRespDTO> result = statsService.getHistoryStats(null, null, null);

            assertNotNull(result);
            assertEquals(7, result.size());
        }

        @Test
        @DisplayName("查询成功 - 无数据返回零值列表")
        void getHistoryStats_Success_NoData() {
            LocalDate start = LocalDate.now().minusDays(3);
            LocalDate end = LocalDate.now();

            when(statsMapper.selectStatsTodayByShortCode(TEST_SHORT_CODE)).thenReturn(Collections.emptyList());

            List<StatsHistoryRespDTO> result = statsService.getHistoryStats(TEST_SHORT_CODE, start.toString(), end.toString());

            assertNotNull(result);
            assertEquals(4, result.size());
            result.forEach(dto -> {
                assertEquals(0L, dto.getPv());
                assertEquals(0L, dto.getUv());
                assertEquals(0L, dto.getUip());
            });
        }
    }

    @Nested
    @DisplayName("获取分组统计方法测试")
    class GetGroupStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getGroupStats_Success() {
            List<LinkStatsTodayDO> statsList = new ArrayList<>();
            LinkStatsTodayDO stats = new LinkStatsTodayDO();
            stats.setGid(TEST_GID);
            stats.setPv(200L);
            stats.setUv(100L);
            stats.setUip(60L);
            statsList.add(stats);

            when(statsMapper.selectStatsTodayByGid(TEST_GID)).thenReturn(statsList);

            StatsRespDTO result = statsService.getGroupStats(TEST_GID);

            assertNotNull(result);
            assertEquals(TEST_GID, result.getGid());
            assertEquals(200L, result.getPv());
            assertEquals(100L, result.getUv());
            assertEquals(60L, result.getUip());
        }

        @Test
        @DisplayName("查询成功 - 空数据返回零值")
        void getGroupStats_Success_EmptyData() {
            when(statsMapper.selectStatsTodayByGid(TEST_GID)).thenReturn(Collections.emptyList());

            StatsRespDTO result = statsService.getGroupStats(TEST_GID);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
        }

        @Test
        @DisplayName("查询成功 - 分组ID为空返回零值")
        void getGroupStats_Success_EmptyGid() {
            StatsRespDTO result = statsService.getGroupStats(null);

            assertNotNull(result);
            assertEquals(0L, result.getPv());
        }
    }

    @Nested
    @DisplayName("获取高频IP方法测试")
    class GetHighFreqIpTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getHighFreqIp_Success() {
            Set<String> topIps = new HashSet<>();
            topIps.add("192.168.1.1");
            topIps.add("192.168.1.2");
            topIps.add("192.168.1.3");

            when(zSetOperations.reverseRange(anyString(), eq(0L), eq(9L))).thenReturn(topIps);

            List<String> result = statsService.getHighFreqIp(TEST_SHORT_CODE, 10);

            assertNotNull(result);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("查询成功 - 无数据返回空列表")
        void getHighFreqIp_Success_NoData() {
            when(zSetOperations.reverseRange(anyString(), eq(0L), eq(9L))).thenReturn(Collections.emptySet());

            List<String> result = statsService.getHighFreqIp(TEST_SHORT_CODE, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询成功 - 短链接码为空返回空列表")
        void getHighFreqIp_Success_EmptyShortCode() {
            List<String> result = statsService.getHighFreqIp(null, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询成功 - 使用默认limit")
        void getHighFreqIp_Success_DefaultLimit() {
            Set<String> topIps = new HashSet<>();
            topIps.add("192.168.1.1");

            when(zSetOperations.reverseRange(anyString(), eq(0L), eq(9L))).thenReturn(topIps);

            List<String> result = statsService.getHighFreqIp(TEST_SHORT_CODE, null);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("访问日志分页查询方法测试")
    class PageAccessLogTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void pageAccessLog_Success() {
            StatsLogPageReqDTO request = new StatsLogPageReqDTO();
            request.setShortCode(TEST_SHORT_CODE);
            request.setCurrent(1);
            request.setSize(10);

            Page<LinkStatsDO> statsPage = new Page<>(1, 10);
            statsPage.setRecords(Collections.emptyList());
            statsPage.setTotal(0);

            when(statsMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(statsPage);

            Page<LinkStatsDO> result = statsService.pageAccessLog(request);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("查询成功 - 请求为空返回空分页")
        void pageAccessLog_Success_NullRequest() {
            Page<LinkStatsDO> result = statsService.pageAccessLog(null);

            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("查询成功 - 短链接码和分组ID都为空返回空分页")
        void pageAccessLog_Success_EmptyParams() {
            StatsLogPageReqDTO request = new StatsLogPageReqDTO();
            request.setCurrent(1);
            request.setSize(10);

            Page<LinkStatsDO> result = statsService.pageAccessLog(request);

            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
        }
    }
}
