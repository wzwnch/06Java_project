package com.shortlink.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.enums.LinkStatusEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.config.BloomFilterInitializer;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.LinkPageReqDTO;
import com.shortlink.dto.req.LinkUpdateReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.utils.LinkUtils;
import com.shortlink.utils.RedisFallbackHandler;
import com.shortlink.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("短链接服务单元测试")
class LinkServiceImplTest {

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private BloomFilterInitializer bloomFilterInitializer;

    @Mock
    private RLock rLock;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ValueOperations<String, String> stringValueOperations;

    @InjectMocks
    private LinkServiceImpl linkService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_GID = "testgid01";
    private static final String TEST_SHORT_CODE = "abc123";
    private static final String TEST_ORIGIN_URL = "https://www.example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "shortLinkDomain", "http://localhost:8080");
        UserContext.setUserInfo(1L, TEST_USERNAME);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("短链接生成方法测试")
    class CreateLinkTest {

        @Test
        @DisplayName("创建失败 - 用户未登录")
        void createLink_Fail_UserNotLogin() {
            UserContext.clear();

            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl(TEST_ORIGIN_URL);
            request.setGid(TEST_GID);

            BizException exception = assertThrows(BizException.class, () -> linkService.createLink(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("创建失败 - 分组不存在")
        void createLink_Fail_GroupNotExist() {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl(TEST_ORIGIN_URL);
            request.setGid(TEST_GID);

            when(groupMapper.selectOne(any())).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> linkService.createLink(request));

            assertEquals(BizCodeEnum.GROUP_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("创建失败 - URL格式不合法")
        void createLink_Fail_InvalidUrl() {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl("invalid-url");
            request.setGid(TEST_GID);

            GroupDO groupDO = new GroupDO();
            groupDO.setGid(TEST_GID);
            groupDO.setUsername(TEST_USERNAME);

            when(groupMapper.selectOne(any())).thenReturn(groupDO);

            BizException exception = assertThrows(BizException.class, () -> linkService.createLink(request));

            assertEquals(BizCodeEnum.LINK_URL_INVALID.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("短链接跳转方法测试")
    class RedirectTest {

        @Test
        @DisplayName("跳转失败 - 短链接码为空")
        void redirect_Fail_EmptyShortCode() {
            BizException exception = assertThrows(BizException.class, () -> linkService.redirect(null));

            assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("跳转失败 - 短链接不存在（空值缓存命中）")
        void redirect_Fail_NullCacheHit() {
            try (MockedStatic<RedisFallbackHandler> fallbackMock = mockStatic(RedisFallbackHandler.class)) {
                fallbackMock.when(RedisFallbackHandler::isRedisAvailable).thenReturn(true);

                when(bloomFilterInitializer.containsShortCode(TEST_SHORT_CODE)).thenReturn(false);
                when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
                when(stringValueOperations.get(anyString())).thenReturn("1");

                BizException exception = assertThrows(BizException.class, () -> linkService.redirect(TEST_SHORT_CODE));

                assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("短链接分页查询方法测试")
    class PageLinksTest {

        @Test
        @DisplayName("查询失败 - 用户未登录")
        void pageLinks_Fail_UserNotLogin() {
            UserContext.clear();

            LinkPageReqDTO request = new LinkPageReqDTO();
            request.setCurrent(1);
            request.setSize(10);

            BizException exception = assertThrows(BizException.class, () -> linkService.pageLinks(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("短链接修改方法测试")
    class UpdateLinkTest {

        @Test
        @DisplayName("修改失败 - 用户未登录")
        void updateLink_Fail_UserNotLogin() {
            UserContext.clear();

            LinkUpdateReqDTO request = new LinkUpdateReqDTO();
            request.setShortCode(TEST_SHORT_CODE);

            BizException exception = assertThrows(BizException.class, () -> linkService.updateLink(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("修改失败 - 短链接不存在")
        void updateLink_Fail_LinkNotExist() {
            LinkUpdateReqDTO request = new LinkUpdateReqDTO();
            request.setShortCode(TEST_SHORT_CODE);

            when(linkMapper.selectOne(any())).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> linkService.updateLink(request));

            assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("短链接删除方法测试")
    class DeleteLinkTest {

        @Test
        @DisplayName("删除失败 - 用户未登录")
        void deleteLink_Fail_UserNotLogin() {
            UserContext.clear();

            BizException exception = assertThrows(BizException.class, () -> linkService.deleteLink(TEST_SHORT_CODE));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("删除失败 - 短链接不存在")
        void deleteLink_Fail_LinkNotExist() {
            when(linkMapper.selectOne(any())).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> linkService.deleteLink(TEST_SHORT_CODE));

            assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("删除失败 - 短链接码为空")
        void deleteLink_Fail_EmptyShortCode() {
            BizException exception = assertThrows(BizException.class, () -> linkService.deleteLink(null));

            assertEquals(BizCodeEnum.LINK_NOT_EXIST.getCode(), exception.getCode());
        }
    }
}
