package com.shortlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.dto.req.UserLoginReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.dto.req.UserUpdateReqDTO;
import com.shortlink.dto.resp.UserInfoRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.EncryptUtils;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RBloomFilter<String> bloomFilter;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_PHONE = "13800138000";
    private static final String TEST_MAIL = "test@example.com";
    private static final String ENCRYPTED_PHONE = "encrypted_phone";
    private static final String ENCRYPTED_MAIL = "encrypted_mail";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().doReturn(bloomFilter).when(redissonClient).getBloomFilter(anyString());
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("用户注册方法测试")
    class RegisterTest {

        @Test
        @DisplayName("正常注册 - 成功")
        void register_Success() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setPhone(TEST_PHONE);
            request.setMail(TEST_MAIL);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(false);
            when(userMapper.insert(any(UserDO.class))).thenReturn(1);
            when(groupMapper.insert(any(GroupDO.class))).thenReturn(1);

            try (MockedStatic<EncryptUtils> encryptUtilsMock = mockStatic(EncryptUtils.class)) {
                encryptUtilsMock.when(() -> EncryptUtils.encrypt(TEST_PHONE)).thenReturn(ENCRYPTED_PHONE);
                encryptUtilsMock.when(() -> EncryptUtils.encrypt(TEST_MAIL)).thenReturn(ENCRYPTED_MAIL);

                assertDoesNotThrow(() -> userService.register(request));
            }

            verify(bloomFilter).add(TEST_USERNAME);
            verify(userMapper).insert(any(UserDO.class));
            verify(groupMapper).insert(any(GroupDO.class));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在（布隆过滤器判断存在）")
        void register_Fail_UsernameExistsInBloomFilter() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BizException exception = assertThrows(BizException.class, () -> userService.register(request));

            assertEquals(BizCodeEnum.USER_EXIST.getCode(), exception.getCode());
            verify(userMapper, never()).insert(any(UserDO.class));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在（数据库查询确认）")
        void register_Fail_UsernameExistsInDatabase() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BizException exception = assertThrows(BizException.class, () -> userService.register(request));

            assertEquals(BizCodeEnum.USER_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("注册失败 - 手机号已被绑定")
        void register_Fail_PhoneAlreadyBound() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setPhone(TEST_PHONE);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(false);
            when(userMapper.selectCount(any()))
                    .thenReturn(0L);

            try (MockedStatic<EncryptUtils> encryptUtilsMock = mockStatic(EncryptUtils.class)) {
                encryptUtilsMock.when(() -> EncryptUtils.encrypt(TEST_PHONE)).thenReturn(ENCRYPTED_PHONE);
                when(userMapper.selectCount(any())).thenReturn(1L);

                BizException exception = assertThrows(BizException.class, () -> userService.register(request));

                assertEquals(BizCodeEnum.USER_PHONE_EXIST.getCode(), exception.getCode());
            }
        }

        @Test
        @DisplayName("注册成功 - 不带手机号和邮箱")
        void register_Success_WithoutPhoneAndMail() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(false);
            when(userMapper.insert(any(UserDO.class))).thenReturn(1);
            when(groupMapper.insert(any(GroupDO.class))).thenReturn(1);

            assertDoesNotThrow(() -> userService.register(request));

            verify(userMapper).insert(any(UserDO.class));
        }

        @Test
        @DisplayName("注册成功 - 布隆过滤器判断存在但数据库不存在")
        void register_Success_BloomFilterFalsePositive() {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userMapper.insert(any(UserDO.class))).thenReturn(1);
            when(groupMapper.insert(any(GroupDO.class))).thenReturn(1);

            assertDoesNotThrow(() -> userService.register(request));

            verify(userMapper).insert(any(UserDO.class));
        }
    }

    @Nested
    @DisplayName("用户登录方法测试")
    class LoginTest {

        @Test
        @DisplayName("正常登录 - 成功")
        void login_Success() {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            UserDO userDO = new UserDO();
            userDO.setId(1L);
            userDO.setUsername(TEST_USERNAME);
            userDO.setPassword(PasswordUtils.encode(TEST_PASSWORD));

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userDO);

            try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.generateToken(anyLong(), anyString())).thenReturn("mockToken");

                String token = userService.login(request);

                assertNotNull(token);
                assertEquals("mockToken", token);
            }
        }

        @Test
        @DisplayName("登录失败 - 用户不存在（布隆过滤器判断不存在）")
        void login_Fail_UserNotExistsByBloomFilter() {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername("nonexistent");
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains("nonexistent")).thenReturn(false);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> userService.login(request));

            assertEquals(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), exception.getCode());
            verify(valueOperations).set(anyString(), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("登录失败 - 用户不存在（空值缓存命中）")
        void login_Fail_NullCacheHit() {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername("nonexistent");
            request.setPassword(TEST_PASSWORD);

            when(bloomFilter.contains("nonexistent")).thenReturn(false);
            when(valueOperations.get(anyString())).thenReturn("1");

            BizException exception = assertThrows(BizException.class, () -> userService.login(request));

            assertEquals(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), exception.getCode());
            verify(userMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_Fail_WrongPassword() {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongpassword");

            UserDO userDO = new UserDO();
            userDO.setId(1L);
            userDO.setUsername(TEST_USERNAME);
            userDO.setPassword(PasswordUtils.encode(TEST_PASSWORD));

            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userDO);

            BizException exception = assertThrows(BizException.class, () -> userService.login(request));

            assertEquals(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("用户退出方法测试")
    class LogoutTest {

        @Test
        @DisplayName("正常退出 - 成功")
        void logout_Success() {
            String token = "validToken";

            try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.getUserId(token)).thenReturn(1L);

                assertDoesNotThrow(() -> userService.logout(token));

                verify(valueOperations).set(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS));
            }
        }

        @Test
        @DisplayName("退出失败 - Token为空")
        void logout_Fail_EmptyToken() {
            assertDoesNotThrow(() -> userService.logout(null));
            assertDoesNotThrow(() -> userService.logout(""));

            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("退出失败 - 无效Token")
        void logout_Fail_InvalidToken() {
            String token = "invalidToken";

            try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
                jwtUtilsMock.when(() -> JwtUtils.getUserId(token)).thenReturn(null);

                assertDoesNotThrow(() -> userService.logout(token));

                verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
            }
        }
    }

    @Nested
    @DisplayName("查询用户信息方法测试")
    class GetUserInfoTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getUserInfo_Success() {
            Long userId = 1L;

            UserDO userDO = new UserDO();
            userDO.setId(userId);
            userDO.setUsername(TEST_USERNAME);
            userDO.setRealPhone("138****8000");
            userDO.setRealMail("t***@example.com");

            when(userMapper.selectById(userId)).thenReturn(userDO);

            UserInfoRespDTO result = userService.getUserInfo(userId);

            assertNotNull(result);
            assertEquals(userId, result.getId());
            assertEquals(TEST_USERNAME, result.getUsername());
        }

        @Test
        @DisplayName("查询失败 - 用户ID为空")
        void getUserInfo_Fail_NullUserId() {
            BizException exception = assertThrows(BizException.class, () -> userService.getUserInfo(null));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("查询失败 - 用户不存在")
        void getUserInfo_Fail_UserNotExist() {
            Long userId = 999L;

            when(userMapper.selectById(userId)).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> userService.getUserInfo(userId));

            assertEquals(BizCodeEnum.USER_NOT_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("修改用户信息方法测试")
    class UpdateUserInfoTest {

        @Test
        @DisplayName("正常修改 - 成功")
        void updateUserInfo_Success() {
            Long userId = 1L;
            UserUpdateReqDTO request = new UserUpdateReqDTO();
            request.setPhone("13900139000");

            UserDO userDO = new UserDO();
            userDO.setId(userId);
            userDO.setUsername(TEST_USERNAME);

            when(userMapper.selectById(userId)).thenReturn(userDO);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            try (MockedStatic<EncryptUtils> encryptUtilsMock = mockStatic(EncryptUtils.class)) {
                encryptUtilsMock.when(() -> EncryptUtils.encrypt("13900139000")).thenReturn("encrypted");

                assertDoesNotThrow(() -> userService.updateUserInfo(userId, request));
            }

            verify(userMapper).updateById(any(UserDO.class));
        }

        @Test
        @DisplayName("修改失败 - 用户ID为空")
        void updateUserInfo_Fail_NullUserId() {
            UserUpdateReqDTO request = new UserUpdateReqDTO();

            BizException exception = assertThrows(BizException.class, () -> userService.updateUserInfo(null, request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("修改失败 - 用户不存在")
        void updateUserInfo_Fail_UserNotExist() {
            Long userId = 999L;
            UserUpdateReqDTO request = new UserUpdateReqDTO();

            when(userMapper.selectById(userId)).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> userService.updateUserInfo(userId, request));

            assertEquals(BizCodeEnum.USER_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("修改密码 - 成功")
        void updateUserInfo_ChangePassword_Success() {
            Long userId = 1L;
            String oldPassword = "Old123456";
            String newPassword = "New123456";

            UserUpdateReqDTO request = new UserUpdateReqDTO();
            request.setOldPassword(oldPassword);
            request.setNewPassword(newPassword);

            UserDO userDO = new UserDO();
            userDO.setId(userId);
            userDO.setUsername(TEST_USERNAME);
            userDO.setPassword(PasswordUtils.encode(oldPassword));

            when(userMapper.selectById(userId)).thenReturn(userDO);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            assertDoesNotThrow(() -> userService.updateUserInfo(userId, request));
        }

        @Test
        @DisplayName("修改密码 - 旧密码错误")
        void updateUserInfo_ChangePassword_WrongOldPassword() {
            Long userId = 1L;

            UserUpdateReqDTO request = new UserUpdateReqDTO();
            request.setOldPassword("wrongpassword");
            request.setNewPassword("New123456");

            UserDO userDO = new UserDO();
            userDO.setId(userId);
            userDO.setUsername(TEST_USERNAME);
            userDO.setPassword(PasswordUtils.encode("correctpassword"));

            when(userMapper.selectById(userId)).thenReturn(userDO);

            BizException exception = assertThrows(BizException.class, () -> userService.updateUserInfo(userId, request));

            assertEquals(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("检查用户名方法测试")
    class IsUsernameAvailableTest {

        @Test
        @DisplayName("用户名可用 - 布隆过滤器判断不存在")
        void isUsernameAvailable_True_ByBloomFilter() {
            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(false);

            boolean result = userService.isUsernameAvailable(TEST_USERNAME);

            assertTrue(result);
        }

        @Test
        @DisplayName("用户名可用 - 空值缓存命中")
        void isUsernameAvailable_True_ByNullCache() {
            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn("1");

            boolean result = userService.isUsernameAvailable(TEST_USERNAME);

            assertTrue(result);
        }

        @Test
        @DisplayName("用户名可用 - 数据库查询不存在")
        void isUsernameAvailable_True_ByDatabase() {
            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            boolean result = userService.isUsernameAvailable(TEST_USERNAME);

            assertTrue(result);
            verify(valueOperations).set(anyString(), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("用户名不可用 - 已存在")
        void isUsernameAvailable_False_AlreadyExists() {
            when(bloomFilter.contains(TEST_USERNAME)).thenReturn(true);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            boolean result = userService.isUsernameAvailable(TEST_USERNAME);

            assertFalse(result);
        }

        @Test
        @DisplayName("用户名不可用 - 用户名为空")
        void isUsernameAvailable_Fail_EmptyUsername() {
            boolean result = userService.isUsernameAvailable(null);
            assertFalse(result);

            result = userService.isUsernameAvailable("");
            assertFalse(result);
        }
    }
}
