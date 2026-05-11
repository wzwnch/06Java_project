package com.shortlink.controller;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.entity.LinkStatsDO;
import com.shortlink.entity.LinkStatsTodayDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.mapper.StatsMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.PasswordUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("监控统计模块接口集成测试")
class StatsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StatsMapper statsMapper;

    private static final String BASE_URL = "/api/stats";
    private static final String TEST_USERNAME = "statstest";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_ORIGIN_URL = "https://www.example.com";
    private static final String TEST_SHORT_CODE = "stats01";

    private UserDO testUser;
    private GroupDO testGroup;
    private String testToken;

    @BeforeAll
    void setupAll() {
        cleanupTestData();
        testUser = createTestUser();
        testGroup = createTestGroup();
        testToken = generateTestToken(testUser.getId(), TEST_USERNAME);
    }

    @BeforeEach
    void setup() {
        cleanupLinkData();
    }

    @AfterAll
    void teardown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            linkMapper.deleteByUsername(TEST_USERNAME);
            groupMapper.deleteByUsername(TEST_USERNAME);
            userMapper.deleteByUsername(TEST_USERNAME);
        } catch (Exception ignored) {
        }
    }

    private void cleanupLinkData() {
        try {
            linkMapper.deleteByUsername(TEST_USERNAME);
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("单链接统计接口测试")
    class GetLinkStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getLinkStats_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/link/" + TEST_SHORT_CODE,
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getLinkStats_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/link/" + TEST_SHORT_CODE)
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("访问日志查询接口测试")
    class PageAccessLogTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void pageAccessLog_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);
            createTestStats(TEST_SHORT_CODE);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/log/page?shortCode=" + TEST_SHORT_CODE + "&current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void pageAccessLog_Success_EmptyList() throws Exception {
            createTestLink(TEST_SHORT_CODE);

            performGetWithToken(
                    BASE_URL + "/log/page?shortCode=" + TEST_SHORT_CODE + "&current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void pageAccessLog_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/log/page?shortCode=" + TEST_SHORT_CODE + "&current=1&size=10")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("今日统计接口测试")
    class GetTodayStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getTodayStats_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);
            createTestStatsToday(TEST_SHORT_CODE);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/today?shortCode=" + TEST_SHORT_CODE,
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getTodayStats_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/today?shortCode=" + TEST_SHORT_CODE)
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("历史统计接口测试")
    class GetHistoryStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getHistoryStats_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);
            createTestStatsToday(TEST_SHORT_CODE);

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            MvcResult result = performGetWithToken(
                    BASE_URL + "/history?shortCode=" + TEST_SHORT_CODE + "&startDate=" + yesterday + "&endDate=" + today,
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getHistoryStats_Fail_NotLogin() throws Exception {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            performGet(BASE_URL + "/history?shortCode=" + TEST_SHORT_CODE + "&startDate=" + today + "&endDate=" + today)
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("分组统计接口测试")
    class GetGroupStatsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getGroupStats_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/group/" + testGroup.getGid(),
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getGroupStats_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/group/" + testGroup.getGid())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("高频IP统计接口测试")
    class GetHighFreqIpTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getHighFreqIp_Success() throws Exception {
            createTestLink(TEST_SHORT_CODE);
            createTestStats(TEST_SHORT_CODE);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/high-freq-ip?shortCode=" + TEST_SHORT_CODE,
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void getHighFreqIp_Success_EmptyList() throws Exception {
            createTestLink(TEST_SHORT_CODE);

            performGetWithToken(
                    BASE_URL + "/high-freq-ip?shortCode=" + TEST_SHORT_CODE,
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getHighFreqIp_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/high-freq-ip?shortCode=" + TEST_SHORT_CODE)
                    .andExpect(status().isUnauthorized());
        }
    }

    private UserDO createTestUser() {
        UserDO user = new UserDO();
        user.setUsername(TEST_USERNAME);
        user.setPassword(PasswordUtils.encode(TEST_PASSWORD));
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private GroupDO createTestGroup() {
        GroupDO group = new GroupDO();
        group.setGid(generateGid());
        group.setName("测试分组");
        group.setUsername(TEST_USERNAME);
        group.setSortOrder(0);
        group.setDelFlag(0);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.insert(group);
        return group;
    }

    private LinkDO createTestLink(String shortCode) {
        LinkDO link = new LinkDO();
        link.setShortCode(shortCode);
        link.setGid(testGroup.getGid());
        link.setOriginUrl(TEST_ORIGIN_URL);
        link.setStatus(0);
        link.setDelFlag(0);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        linkMapper.insert(link);
        return link;
    }

    private LinkStatsDO createTestStats(String shortCode) {
        LinkStatsDO stats = new LinkStatsDO();
        stats.setShortCode(shortCode);
        stats.setGid(testGroup.getGid());
        stats.setPv(1L);
        stats.setIp("127.0.0.1");
        stats.setRegion("北京");
        stats.setOs("Windows");
        stats.setBrowser("Chrome");
        stats.setDevice("PC");
        stats.setCreateTime(LocalDateTime.now());
        statsMapper.insert(stats);
        return stats;
    }

    private LinkStatsTodayDO createTestStatsToday(String shortCode) {
        LinkStatsTodayDO statsToday = new LinkStatsTodayDO();
        statsToday.setId(System.currentTimeMillis());
        statsToday.setShortCode(shortCode);
        statsToday.setGid(testGroup.getGid());
        statsToday.setDate(LocalDate.now());
        statsToday.setPv(10L);
        statsToday.setUv(5L);
        statsToday.setUip(3L);
        statsToday.setCreateTime(LocalDateTime.now());
        statsToday.setUpdateTime(LocalDateTime.now());
        statsMapper.insertStatsToday(statsToday);
        return statsToday;
    }

    private String generateGid() {
        return String.valueOf(System.currentTimeMillis() % 100000000);
    }
}
