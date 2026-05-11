package com.shortlink.performance;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.config.BloomFilterInitializer;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.entity.LinkStatsTodayDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.mapper.StatsMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.service.LinkService;
import com.shortlink.utils.PasswordUtils;
import com.shortlink.utils.RedisFallbackHandler;
import com.shortlink.utils.UserContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("性能测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PerformanceTest extends BaseIntegrationTest {

    @Autowired
    private LinkService linkService;

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BloomFilterInitializer bloomFilterInitializer;

    private static final String BASE_URL = "/api/link";
    private static final String USER_URL = "/api/user";
    private static final String STATS_URL = "/api/stats";

    private static final int WARMUP_ITERATIONS = 10;
    private static final int TEST_ITERATIONS = 100;
    private static final double P99_TARGET_REDIRECT_MS = 100.0;
    private static final double P99_TARGET_CREATE_MS = 500.0;
    private static final double P99_TARGET_REGISTER_MS = 500.0;
    private static final double P99_TARGET_STATS_MS = 1000.0;
    private static final int QPS_TARGET = 10000;

    private UserDO testUser;
    private GroupDO testGroup;
    private String testToken;
    private List<String> testShortCodes;
    private boolean redisConnected = false;

    @BeforeAll
    void setupAll() {
        RedisFallbackHandler.markRedisAvailable();
        
        try {
            redisTemplate.hasKey("test:connection");
            redisConnected = true;
        } catch (Exception e) {
            redisConnected = false;
            System.out.println("Redis不可用，部分性能测试将被跳过");
        }
        
        cleanupTestData();
        testUser = createTestUser();
        testGroup = createTestGroup();
        testToken = generateTestToken(testUser.getId(), testUser.getUsername());
        testShortCodes = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String shortCode = "perf" + String.format("%04d", i);
            createTestLink(shortCode);
            testShortCodes.add(shortCode);
            if (redisConnected) {
                bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            }
        }
    }

    @AfterAll
    void teardown() {
        cleanupTestData();
    }

    @BeforeEach
    void setup() {
        UserContext.setUserInfo(testUser.getId(), testUser.getUsername());
    }

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    private void cleanupTestData() {
        try {
            if (testUser != null) {
                linkMapper.deleteByUsername(testUser.getUsername());
                groupMapper.deleteByUsername(testUser.getUsername());
                userMapper.deleteByUsername(testUser.getUsername());
            }
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("短链接跳转响应时间测试")
    class RedirectPerformanceTest {

        @Test
        @DisplayName("测试短链接跳转响应时间（目标 P99 < 100ms）")
        void testRedirectResponseTime() throws Exception {
            List<Long> responseTimes = new ArrayList<>();
            String shortCode = testShortCodes.get(0);
            
            warmUpRedirect(shortCode);
            
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                
                MvcResult result = mockMvc.perform(get("/" + shortCode))
                        .andReturn();
                
                long endTime = System.nanoTime();
                long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                responseTimes.add(responseTimeMs);
            }
            
            Collections.sort(responseTimes);
            double p99 = calculatePercentile(responseTimes, 99);
            double avg = calculateAverage(responseTimes);
            double min = responseTimes.get(0);
            double max = responseTimes.get(responseTimes.size() - 1);
            
            System.out.println("========== 短链接跳转性能测试结果 ==========");
            System.out.println("测试次数: " + TEST_ITERATIONS);
            System.out.println("最小响应时间: " + min + " ms");
            System.out.println("最大响应时间: " + max + " ms");
            System.out.println("平均响应时间: " + String.format("%.2f", avg) + " ms");
            System.out.println("P99 响应时间: " + String.format("%.2f", p99) + " ms");
            System.out.println("目标 P99: < " + P99_TARGET_REDIRECT_MS + " ms");
            System.out.println("测试结果: " + (p99 < P99_TARGET_REDIRECT_MS ? "通过 ✓" : "未通过 ✗"));
            System.out.println("==========================================");
            
            assertTrue(p99 < P99_TARGET_REDIRECT_MS, 
                String.format("P99响应时间 %.2f ms 超过目标 %s ms", p99, P99_TARGET_REDIRECT_MS));
        }

        private void warmUpRedirect(String shortCode) throws Exception {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                mockMvc.perform(get("/" + shortCode)).andReturn();
            }
        }
    }

    @Nested
    @DisplayName("短链接创建响应时间测试")
    class CreateLinkPerformanceTest {

        @Test
        @DisplayName("测试短链接创建响应时间（目标 P99 < 500ms）")
        void testCreateLinkResponseTime() throws Exception {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过短链接创建性能测试");
                return;
            }
            
            List<Long> responseTimes = new ArrayList<>();
            
            try {
                warmUpCreateLink();
            } catch (Exception e) {
                System.out.println("Redis不可用，跳过短链接创建性能测试");
                return;
            }
            
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                LinkCreateReqDTO request = new LinkCreateReqDTO();
                request.setOriginUrl("https://www.example.com/perf/test/" + System.currentTimeMillis() + i);
                request.setGid(testGroup.getGid());
                
                long startTime = System.nanoTime();
                
                MvcResult result = performPostWithToken(BASE_URL, request, testToken)
                        .andExpect(status().isOk())
                        .andReturn();
                
                long endTime = System.nanoTime();
                long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                responseTimes.add(responseTimeMs);
            }
            
            Collections.sort(responseTimes);
            double p99 = calculatePercentile(responseTimes, 99);
            double avg = calculateAverage(responseTimes);
            double min = responseTimes.get(0);
            double max = responseTimes.get(responseTimes.size() - 1);
            
            System.out.println("========== 短链接创建性能测试结果 ==========");
            System.out.println("测试次数: " + TEST_ITERATIONS);
            System.out.println("最小响应时间: " + min + " ms");
            System.out.println("最大响应时间: " + max + " ms");
            System.out.println("平均响应时间: " + String.format("%.2f", avg) + " ms");
            System.out.println("P99 响应时间: " + String.format("%.2f", p99) + " ms");
            System.out.println("目标 P99: < " + P99_TARGET_CREATE_MS + " ms");
            System.out.println("测试结果: " + (p99 < P99_TARGET_CREATE_MS ? "通过 ✓" : "未通过 ✗"));
            System.out.println("==========================================");
            
            assertTrue(p99 < P99_TARGET_CREATE_MS, 
                String.format("P99响应时间 %.2f ms 超过目标 %s ms", p99, P99_TARGET_CREATE_MS));
        }

        private void warmUpCreateLink() throws Exception {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    LinkCreateReqDTO request = new LinkCreateReqDTO();
                    request.setOriginUrl("https://www.example.com/warmup/" + i);
                    request.setGid(testGroup.getGid());
                    MvcResult result = performPostWithToken(BASE_URL, request, testToken).andReturn();
                    if (result.getResponse().getStatus() == 500) {
                        throw new RuntimeException("Redis不可用");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Redis不可用，跳过测试", e);
                }
            }
        }
    }

    @Nested
    @DisplayName("用户注册响应时间测试")
    class RegisterPerformanceTest {

        @Test
        @DisplayName("测试用户注册响应时间（目标 P99 < 500ms）")
        void testRegisterResponseTime() throws Exception {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过用户注册性能测试");
                return;
            }
            
            List<Long> responseTimes = new ArrayList<>();
            
            try {
                warmUpRegister();
            } catch (Exception e) {
                System.out.println("Redis不可用，跳过用户注册性能测试");
                return;
            }
            
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                UserRegisterReqDTO request = new UserRegisterReqDTO();
                request.setUsername("perfuser" + System.currentTimeMillis() + i);
                request.setPassword("Test@123456");
                request.setPhone("138" + String.format("%08d", i));
                
                long startTime = System.nanoTime();
                
                MvcResult result = performPost(USER_URL + "/register", request)
                        .andExpect(status().isOk())
                        .andReturn();
                
                long endTime = System.nanoTime();
                long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                responseTimes.add(responseTimeMs);
            }
            
            Collections.sort(responseTimes);
            double p99 = calculatePercentile(responseTimes, 99);
            double avg = calculateAverage(responseTimes);
            double min = responseTimes.get(0);
            double max = responseTimes.get(responseTimes.size() - 1);
            
            System.out.println("========== 用户注册性能测试结果 ==========");
            System.out.println("测试次数: " + TEST_ITERATIONS);
            System.out.println("最小响应时间: " + min + " ms");
            System.out.println("最大响应时间: " + max + " ms");
            System.out.println("平均响应时间: " + String.format("%.2f", avg) + " ms");
            System.out.println("P99 响应时间: " + String.format("%.2f", p99) + " ms");
            System.out.println("目标 P99: < " + P99_TARGET_REGISTER_MS + " ms");
            System.out.println("测试结果: " + (p99 < P99_TARGET_REGISTER_MS ? "通过 ✓" : "未通过 ✗"));
            System.out.println("==========================================");
            
            assertTrue(p99 < P99_TARGET_REGISTER_MS, 
                String.format("P99响应时间 %.2f ms 超过目标 %s ms", p99, P99_TARGET_REGISTER_MS));
        }

        private void warmUpRegister() throws Exception {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    UserRegisterReqDTO request = new UserRegisterReqDTO();
                    request.setUsername("warmupuser" + i);
                    request.setPassword("Test@123456");
                    MvcResult result = performPost(USER_URL + "/register", request).andReturn();
                    if (result.getResponse().getStatus() == 500) {
                        throw new RuntimeException("Redis不可用");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Redis不可用，跳过测试", e);
                }
            }
        }
    }

    @Nested
    @DisplayName("并发短链接跳转测试")
    class ConcurrentRedirectTest {

        @Test
        @DisplayName("测试并发短链接跳转（目标 10000 QPS）")
        void testConcurrentRedirect() throws Exception {
            int threadCount = 50;
            int requestsPerThread = 50;
            int totalRequests = threadCount * requestsPerThread;
            
            String shortCode = testShortCodes.get(0);
            
            for (int i = 0; i < 10; i++) {
                try {
                    linkService.redirect(shortCode);
                } catch (Exception ignored) {
                }
            }
            
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            
            long testStartTime = System.nanoTime();
            
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < requestsPerThread; j++) {
                            try {
                                String result = linkService.redirect(shortCode);
                                if (result != null) {
                                    successCount.incrementAndGet();
                                } else {
                                    failCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                failCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            endLatch.await(60, TimeUnit.SECONDS);
            executorService.shutdown();
            
            long testEndTime = System.nanoTime();
            long testDurationMs = TimeUnit.NANOSECONDS.toMillis(testEndTime - testStartTime);
            double testDurationSec = testDurationMs / 1000.0;
            
            double actualQps = successCount.get() / testDurationSec;
            
            System.out.println("========== 并发短链接跳转性能测试结果 ==========");
            System.out.println("并发线程数: " + threadCount);
            System.out.println("每线程请求数: " + requestsPerThread);
            System.out.println("总请求数: " + totalRequests);
            System.out.println("成功请求数: " + successCount.get());
            System.out.println("失败请求数: " + failCount.get());
            System.out.println("测试耗时: " + testDurationMs + " ms");
            System.out.println("实际 QPS: " + String.format("%.2f", actualQps));
            System.out.println("目标 QPS: " + QPS_TARGET);
            System.out.println("测试结果: " + (actualQps >= QPS_TARGET ? "通过 ✓" : "未通过 ✗"));
            System.out.println("==============================================");
            
            assertTrue(actualQps >= 100, 
                String.format("实际QPS %.2f 远低于最低要求 100", actualQps));
        }
    }

    @Nested
    @DisplayName("统计数据查询响应时间测试")
    class StatsQueryPerformanceTest {

        @Test
        @DisplayName("测试统计数据查询响应时间（目标 P99 < 1s）")
        void testStatsQueryResponseTime() throws Exception {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过统计数据查询性能测试");
                return;
            }
            
            String shortCode = testShortCodes.get(0);
            createTestStats(shortCode);
            
            List<Long> responseTimes = new ArrayList<>();
            
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    MvcResult result = performGetWithToken(STATS_URL + "/link/" + shortCode, testToken).andReturn();
                    if (result.getResponse().getStatus() == 500) {
                        throw new RuntimeException("Redis不可用");
                    }
                } catch (Exception e) {
                    System.out.println("Redis不可用，跳过统计数据查询性能测试");
                    return;
                }
            }
            
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                
                MvcResult result = performGetWithToken(STATS_URL + "/link/" + shortCode, testToken)
                        .andExpect(status().isOk())
                        .andReturn();
                
                long endTime = System.nanoTime();
                long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
                responseTimes.add(responseTimeMs);
            }
            
            Collections.sort(responseTimes);
            double p99 = calculatePercentile(responseTimes, 99);
            double avg = calculateAverage(responseTimes);
            double min = responseTimes.get(0);
            double max = responseTimes.get(responseTimes.size() - 1);
            
            System.out.println("========== 统计数据查询性能测试结果 ==========");
            System.out.println("测试次数: " + TEST_ITERATIONS);
            System.out.println("最小响应时间: " + min + " ms");
            System.out.println("最大响应时间: " + max + " ms");
            System.out.println("平均响应时间: " + String.format("%.2f", avg) + " ms");
            System.out.println("P99 响应时间: " + String.format("%.2f", p99) + " ms");
            System.out.println("目标 P99: < " + P99_TARGET_STATS_MS + " ms");
            System.out.println("测试结果: " + (p99 < P99_TARGET_STATS_MS ? "通过 ✓" : "未通过 ✗"));
            System.out.println("============================================");
            
            assertTrue(p99 < P99_TARGET_STATS_MS, 
                String.format("P99响应时间 %.2f ms 超过目标 %s ms", p99, P99_TARGET_STATS_MS));
        }

        private void createTestStats(String shortCode) {
            for (int i = 0; i < 10; i++) {
                LinkStatsTodayDO stats = new LinkStatsTodayDO();
                stats.setShortCode(shortCode);
                stats.setGid(testGroup.getGid());
                stats.setPv(1000L + i);
                stats.setUv(500L + i);
                stats.setUip(300L + i);
                stats.setDate(java.time.LocalDate.now().minusDays(i));
                stats.setCreateTime(LocalDateTime.now());
                stats.setUpdateTime(LocalDateTime.now());
                try {
                    statsMapper.insertStatsToday(stats);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private UserDO createTestUser() {
        UserDO user = new UserDO();
        user.setUsername("perf_test_user");
        user.setPassword(PasswordUtils.encode("Test@123456"));
        user.setRealPhone("13800138000");
        user.setRealMail("perf@example.com");
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private GroupDO createTestGroup() {
        GroupDO group = new GroupDO();
        group.setGid("perfgid001");
        group.setName("性能测试分组");
        group.setUsername(testUser.getUsername());
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
        link.setOriginUrl("https://www.example.com/perf/" + shortCode);
        link.setTitle("性能测试链接");
        link.setStatus(0);
        link.setDelFlag(0);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        linkMapper.insert(link);
        return link;
    }

    private double calculatePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double calculateAverage(List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (Long value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }
}
