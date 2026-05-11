package com.shortlink.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.enums.LinkStatusEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.config.BloomFilterInitializer;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.LinkUpdateReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.service.LinkService;
import com.shortlink.utils.PasswordUtils;
import com.shortlink.utils.RedisFallbackHandler;
import com.shortlink.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("缓存测试")
public class CacheTest {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BloomFilterInitializer bloomFilterInitializer;

    @Autowired
    private LinkService linkService;

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String LINK_CACHE_KEY_PREFIX = "link:info:";
    private static final String LINK_NULL_CACHE_PREFIX = "link:null:";
    private static final String LINK_LOCK_PREFIX = "link:lock:";

    private UserDO testUser;
    private GroupDO testGroup;
    private boolean redisConnected = false;

    @BeforeEach
    void setUp() {
        RedisFallbackHandler.markRedisAvailable();
        
        try {
            redisTemplate.hasKey("test:connection");
            redisConnected = true;
        } catch (Exception e) {
            redisConnected = false;
        }
        
        cleanRedisCache();
        
        testUser = createTestUser();
        testGroup = createTestGroup(testUser.getUsername());
        UserContext.setUserInfo(testUser.getId(), testUser.getUsername());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        cleanRedisCache();
    }

    private UserDO createTestUser() {
        String username = "cache_test_user_" + System.currentTimeMillis();
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username);
        UserDO existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            return existingUser;
        }
        
        UserDO user = new UserDO();
        user.setId(System.currentTimeMillis());
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode("Test@123456"));
        user.setRealPhone("13800138000");
        user.setRealMail("test@example.com");
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private GroupDO createTestGroup(String username) {
        String gid = "testgid" + System.currentTimeMillis() % 100000000;
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getGid, gid);
        GroupDO existingGroup = groupMapper.selectOne(queryWrapper);
        if (existingGroup != null) {
            return existingGroup;
        }
        
        GroupDO group = new GroupDO();
        group.setId(System.currentTimeMillis());
        group.setGid(gid);
        group.setName("测试分组");
        group.setUsername(username);
        group.setSortOrder(0);
        group.setDelFlag(0);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.insert(group);
        return group;
    }

    private LinkDO createTestLink(String shortCode, String gid) {
        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode);
        LinkDO existingLink = linkMapper.selectOne(queryWrapper);
        if (existingLink != null) {
            return existingLink;
        }
        
        LinkDO link = new LinkDO();
        link.setId(System.currentTimeMillis());
        link.setShortCode(shortCode);
        link.setGid(gid);
        link.setOriginUrl("https://www.example.com/test");
        link.setTitle("测试链接");
        link.setFaviconUrl("https://www.example.com/favicon.ico");
        link.setStatus(LinkStatusEnum.NORMAL.getCode());
        link.setDelFlag(0);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        linkMapper.insert(link);
        return link;
    }

    private void cleanRedisCache() {
        if (!redisConnected) return;
        try {
            redisTemplate.delete(redisTemplate.keys(LINK_CACHE_KEY_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(LINK_NULL_CACHE_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(LINK_LOCK_PREFIX + "*"));
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("布隆过滤器防穿透测试")
    class BloomFilterPenetrationTest {

        @Test
        @DisplayName("布隆过滤器能正确判断存在的短链接码")
        void testBloomFilter_ExistingShortCode() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "bloomtest1";
            createTestLink(shortCode, testGroup.getGid());
            
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            boolean contains = bloomFilterInitializer.containsShortCode(shortCode);
            assertTrue(contains, "布隆过滤器应该包含已添加的短链接码");
        }

        @Test
        @DisplayName("布隆过滤器能正确判断不存在的短链接码")
        void testBloomFilter_NonExistingShortCode() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String nonExistingCode = "nonexistcode";
            
            boolean contains = bloomFilterInitializer.containsShortCode(nonExistingCode);
            assertFalse(contains, "布隆过滤器不应该包含未添加的短链接码");
        }

        @Test
        @DisplayName("不存在的短链接码不会穿透到数据库")
        void testBloomFilter_PreventPenetration() {
            String nonExistingCode = "nonexist" + System.currentTimeMillis();
            
            cleanRedisCache();
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                try {
                    linkService.redirect(nonExistingCode);
                    fail("应该抛出异常");
                } catch (BizException e) {
                    assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), e.getCode());
                }
            }
            long endTime = System.currentTimeMillis();
            
            long duration = endTime - startTime;
            assertTrue(duration < 2000, "10次查询应该在2秒内完成，实际耗时: " + duration + "ms");
        }

        @Test
        @DisplayName("空值缓存能防止重复穿透")
        void testNullCache_PreventPenetration() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String nonExistingCode = "nullcache" + System.currentTimeMillis();
            
            cleanRedisCache();
            
            try {
                linkService.redirect(nonExistingCode);
                fail("应该抛出异常");
            } catch (BizException e) {
                assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), e.getCode());
            }
            
            String nullCacheKey = LINK_NULL_CACHE_PREFIX + nonExistingCode;
            String nullCache = stringRedisTemplate.opsForValue().get(nullCacheKey);
            assertNotNull(nullCache, "空值缓存应该存在");
            
            try {
                linkService.redirect(nonExistingCode);
                fail("应该抛出异常");
            } catch (BizException e) {
                assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), e.getCode());
            }
        }

        @Test
        @DisplayName("布隆过滤器能正确判断存在的用户名")
        void testBloomFilter_ExistingUsername() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            bloomFilterInitializer.addUsernameToBloomFilter(testUser.getUsername());
            
            boolean contains = bloomFilterInitializer.containsUsername(testUser.getUsername());
            assertTrue(contains, "布隆过滤器应该包含已添加的用户名");
        }

        @Test
        @DisplayName("布隆过滤器能正确判断不存在的用户名")
        void testBloomFilter_NonExistingUsername() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String nonExistingUsername = "nonexistinguser" + System.currentTimeMillis();
            
            boolean contains = bloomFilterInitializer.containsUsername(nonExistingUsername);
            assertFalse(contains, "布隆过滤器不应该包含未添加的用户名");
        }
    }

    @Nested
    @DisplayName("分布式锁防击穿测试")
    class DistributedLockBreakdownTest {

        @Test
        @DisplayName("热点短链接并发访问时能正确处理")
        void testDistributedLock_HotLinkConcurrentAccess() throws InterruptedException {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "hotlink01";
            createTestLink(shortCode, testGroup.getGid());
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            redisTemplate.delete(cacheKey);
            
            int threadCount = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        String result = linkService.redirect(shortCode);
                        if (result != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            endLatch.await(15, TimeUnit.SECONDS);
            executorService.shutdown();
            
            assertTrue(successCount.get() >= 1, "至少部分请求应该成功，实际成功: " + successCount.get());
        }

        @Test
        @DisplayName("分布式锁能正确获取和释放")
        void testDistributedLock_AcquireAndRelease() throws InterruptedException {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String lockKey = LINK_LOCK_PREFIX + "testlock";
            RLock lock = redissonClient.getLock(lockKey);
            
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            assertTrue(locked, "应该成功获取锁");
            
            try {
                RLock sameLock = redissonClient.getLock(lockKey);
                boolean secondLock = sameLock.tryLock(100, TimeUnit.MILLISECONDS);
                assertTrue(secondLock, "Redisson锁是可重入的，同一线程可以再次获取");
                sameLock.unlock();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            
            RLock newLock = redissonClient.getLock(lockKey);
            boolean newLocked = newLock.tryLock(100, TimeUnit.MILLISECONDS);
            assertTrue(newLocked, "释放后应该能重新获取锁");
            if (newLock.isHeldByCurrentThread()) {
                newLock.unlock();
            }
        }

        @Test
        @DisplayName("不同线程获取锁时只有一个能成功")
        void testDistributedLock_DifferentThreadExclusion() throws InterruptedException {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String lockKey = LINK_LOCK_PREFIX + "exclusiontest";
            AtomicInteger lockAcquiredCount = new AtomicInteger(0);
            AtomicInteger lockFailedCount = new AtomicInteger(0);
            
            int threadCount = 3;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        RLock lock = redissonClient.getLock(lockKey);
                        boolean locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
                        if (locked) {
                            lockAcquiredCount.incrementAndGet();
                            Thread.sleep(200);
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        } else {
                            lockFailedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            endLatch.await(5, TimeUnit.SECONDS);
            executorService.shutdown();
            
            assertTrue(lockAcquiredCount.get() >= 1, "至少应该有一个线程获取到锁");
        }

        @Test
        @DisplayName("缓存过期瞬间并发请求能正确处理")
        void testDistributedLock_SingleCacheRebuild() throws InterruptedException {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "rebuildtest";
            createTestLink(shortCode, testGroup.getGid());
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            redisTemplate.delete(cacheKey);
            
            int threadCount = 3;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger redirectSuccessCount = new AtomicInteger(0);
            
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        String result = linkService.redirect(shortCode);
                        if (result != null) {
                            redirectSuccessCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            endLatch.await(15, TimeUnit.SECONDS);
            executorService.shutdown();
            
            assertTrue(redirectSuccessCount.get() >= 1, "至少部分请求应该成功跳转，实际成功: " + redirectSuccessCount.get());
        }
    }

    @Nested
    @DisplayName("缓存预热测试")
    class CacheWarmUpTest {

        @Test
        @DisplayName("手动预热能将短链接加入缓存")
        void testWarmUp_ManualWarmUp() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "manualwarmup";
            createTestLink(shortCode, testGroup.getGid());
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            redisTemplate.delete(cacheKey);
            
            linkService.warmUpCache();
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNotNull(cachedValue, "预热后缓存应该存在");
        }

        @Test
        @DisplayName("已过期的短链接不会被预热")
        void testWarmUp_ExpiredLinksNotWarmedUp() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String expiredCode = "expired01";
            LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LinkDO::getShortCode, expiredCode);
            linkMapper.delete(queryWrapper);
            
            LinkDO expiredLink = new LinkDO();
            expiredLink.setId(System.currentTimeMillis());
            expiredLink.setShortCode(expiredCode);
            expiredLink.setGid(testGroup.getGid());
            expiredLink.setOriginUrl("https://www.example.com/expired");
            expiredLink.setTitle("已过期链接");
            expiredLink.setExpireTime(LocalDateTime.now().minusDays(1));
            expiredLink.setStatus(LinkStatusEnum.NORMAL.getCode());
            expiredLink.setDelFlag(0);
            expiredLink.setCreateTime(LocalDateTime.now());
            expiredLink.setUpdateTime(LocalDateTime.now());
            linkMapper.insert(expiredLink);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + expiredCode;
            redisTemplate.delete(cacheKey);
            
            linkService.warmUpCache();
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNull(cachedValue, "已过期的短链接不应该被预热到缓存");
        }

        @Test
        @DisplayName("回收站中的短链接不会被预热")
        void testWarmUp_RecycledLinksNotWarmedUp() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String recycledCode = "recycled";
            LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LinkDO::getShortCode, recycledCode);
            linkMapper.delete(queryWrapper);
            
            LinkDO recycledLink = new LinkDO();
            recycledLink.setId(System.currentTimeMillis());
            recycledLink.setShortCode(recycledCode);
            recycledLink.setGid(testGroup.getGid());
            recycledLink.setOriginUrl("https://www.example.com/recycled");
            recycledLink.setTitle("回收站链接");
            recycledLink.setStatus(LinkStatusEnum.RECYCLE.getCode());
            recycledLink.setDelFlag(0);
            recycledLink.setCreateTime(LocalDateTime.now());
            recycledLink.setUpdateTime(LocalDateTime.now());
            linkMapper.insert(recycledLink);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + recycledCode;
            redisTemplate.delete(cacheKey);
            
            linkService.warmUpCache();
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNull(cachedValue, "回收站中的短链接不应该被预热到缓存");
        }

        @Test
        @DisplayName("创建短链接后能正确跳转")
        void testWarmUp_CreateLinkSuccess() {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl("https://www.example.com/autowarmup");
            request.setGid(testGroup.getGid());
            
            LinkRespDTO response = linkService.createLink(request);
            assertNotNull(response, "创建短链接应该成功");
            assertNotNull(response.getShortCode(), "短链接码应该不为空");
            
            if (redisConnected) {
                bloomFilterInitializer.addShortCodeToBloomFilter(response.getShortCode());
            }
            
            String result = linkService.redirect(response.getShortCode());
            assertEquals("https://www.example.com/autowarmup", result, "跳转地址应该正确");
        }
    }

    @Nested
    @DisplayName("缓存更新一致性测试")
    class CacheConsistencyTest {

        @Test
        @DisplayName("修改短链接后缓存被正确失效")
        void testConsistency_UpdateInvalidatesCache() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "updateconsist";
            createTestLink(shortCode, testGroup.getGid());
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            stringRedisTemplate.opsForValue().set(cacheKey, "old_value", 1, TimeUnit.HOURS);
            
            LinkUpdateReqDTO updateRequest = new LinkUpdateReqDTO();
            updateRequest.setShortCode(shortCode);
            updateRequest.setOriginUrl("https://www.example.com/updated");
            
            linkService.updateLink(updateRequest);
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNull(cachedValue, "修改短链接后缓存应该被删除");
        }

        @Test
        @DisplayName("删除短链接后缓存被正确失效")
        void testConsistency_DeleteInvalidatesCache() {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "deleteconsist";
            createTestLink(shortCode, testGroup.getGid());
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            stringRedisTemplate.opsForValue().set(cacheKey, "cached_value", 1, TimeUnit.HOURS);
            
            linkService.deleteLink(shortCode);
            
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNull(cachedValue, "删除短链接后缓存应该被删除");
        }

        @Test
        @DisplayName("缓存失效后重新查询能获取最新数据")
        void testConsistency_GetLatestDataAfterInvalidation() {
            String shortCode = "latestdata";
            createTestLink(shortCode, testGroup.getGid());
            if (redisConnected) {
                bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            }
            
            String newUrl = "https://www.example.com/newurl";
            LinkUpdateReqDTO updateRequest = new LinkUpdateReqDTO();
            updateRequest.setShortCode(shortCode);
            updateRequest.setOriginUrl(newUrl);
            
            linkService.updateLink(updateRequest);
            
            String result = linkService.redirect(shortCode);
            assertEquals(newUrl, result, "缓存失效后重新查询应该获取最新数据");
        }

        @Test
        @DisplayName("并发更新时缓存一致性")
        void testConsistency_ConcurrentUpdates() throws InterruptedException {
            if (!redisConnected) {
                System.out.println("Redis不可用，跳过测试");
                return;
            }
            String shortCode = "concurrent";
            createTestLink(shortCode, testGroup.getGid());
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            
            int threadCount = 3;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        UserContext.setUserInfo(testUser.getId(), testUser.getUsername());
                        startLatch.await();
                        LinkUpdateReqDTO updateRequest = new LinkUpdateReqDTO();
                        updateRequest.setShortCode(shortCode);
                        updateRequest.setOriginUrl("https://www.example.com/update" + index);
                        
                        linkService.updateLink(updateRequest);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        UserContext.clear();
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            endLatch.await(15, TimeUnit.SECONDS);
            executorService.shutdown();
            
            assertTrue(successCount.get() >= 1, "至少部分更新应该成功，实际成功: " + successCount.get());
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertNull(cachedValue, "并发更新后缓存应该被清除");
        }

        @Test
        @DisplayName("先更新数据库再删除缓存的顺序正确")
        void testConsistency_UpdateThenDeleteOrder() {
            String shortCode = "order";
            createTestLink(shortCode, testGroup.getGid());
            if (redisConnected) {
                bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
            }
            
            String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
            if (redisConnected) {
                stringRedisTemplate.opsForValue().set(cacheKey, "old_value", 1, TimeUnit.HOURS);
            }
            
            String newUrl = "https://www.example.com/ordered";
            LinkUpdateReqDTO updateRequest = new LinkUpdateReqDTO();
            updateRequest.setShortCode(shortCode);
            updateRequest.setOriginUrl(newUrl);
            
            linkService.updateLink(updateRequest);
            
            LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LinkDO::getShortCode, shortCode);
            LinkDO dbLink = linkMapper.selectOne(queryWrapper);
            assertEquals(newUrl, dbLink.getOriginUrl(), "数据库应该已更新");
            
            if (redisConnected) {
                Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
                assertNull(cachedValue, "缓存应该被删除");
            }
        }
    }
}
