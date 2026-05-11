package com.shortlink.controller;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.dto.req.UserLoginReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.dto.req.UserUpdateReqDTO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.PasswordUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("用户模块接口集成测试")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    private static final String BASE_URL = "/api/user";
    private static final String TEST_USERNAME = "integrationtest";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_PHONE = "13800138001";
    private static final String TEST_MAIL = "integration@test.com";

    @BeforeAll
    void setupAll() {
        cleanupTestData();
    }

    @BeforeEach
    void setup() {
        cleanupTestData();
    }

    @AfterAll
    void teardown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            userMapper.deleteByUsername(TEST_USERNAME);
            userMapper.deleteByUsername(TEST_USERNAME + "2");
            userMapper.deleteByUsername("newuser");
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("用户注册接口测试")
    class RegisterTest {

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_Fail_UsernameExists() throws Exception {
            createTestUser(TEST_USERNAME);

            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("注册失败 - 用户名为空")
        void register_Fail_EmptyUsername() throws Exception {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername("");
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/register", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("注册失败 - 密码为空")
        void register_Fail_EmptyPassword() throws Exception {
            UserRegisterReqDTO request = new UserRegisterReqDTO();
            request.setUsername("newuser");
            request.setPassword("");

            performPost(BASE_URL + "/register", request)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("用户登录接口测试")
    class LoginTest {

        @Test
        @DisplayName("正常登录 - 成功")
        void login_Success() throws Exception {
            createTestUser(TEST_USERNAME);

            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            MvcResult result = performPost(BASE_URL + "/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").exists())
                    .andExpect(jsonPath("$.data.user.username").value(TEST_USERNAME))
                    .andReturn();
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void login_Fail_UserNotExist() throws Exception {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername("nonexistent");
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1005));
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_Fail_WrongPassword() throws Exception {
            createTestUser(TEST_USERNAME);

            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongpassword");

            performPost(BASE_URL + "/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1005));
        }

        @Test
        @DisplayName("登录失败 - 用户名为空")
        void login_Fail_EmptyUsername() throws Exception {
            UserLoginReqDTO request = new UserLoginReqDTO();
            request.setUsername("");
            request.setPassword(TEST_PASSWORD);

            performPost(BASE_URL + "/login", request)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("用户退出接口测试")
    class LogoutTest {

        @Test
        @DisplayName("正常退出 - 成功")
        void logout_Success() throws Exception {
            UserDO user = createTestUser(TEST_USERNAME);
            String token = generateTestToken(user.getId(), TEST_USERNAME);

            performPostWithToken(BASE_URL + "/logout", null, token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("查询用户信息接口测试")
    class GetUserInfoTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void getUserInfo_Success() throws Exception {
            UserDO user = createTestUser(TEST_USERNAME);
            String token = generateTestToken(user.getId(), TEST_USERNAME);

            MvcResult result = performGetWithToken(BASE_URL + "/info", token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                    .andReturn();
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void getUserInfo_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/info")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改用户信息接口测试")
    class UpdateUserInfoTest {

        @Test
        @DisplayName("修改失败 - 未登录")
        void updateUserInfo_Fail_NotLogin() throws Exception {
            UserUpdateReqDTO request = new UserUpdateReqDTO();
            request.setPhone("13900139000");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put(BASE_URL + "/info")
                    .contentType("application/json")
                    .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("检查用户名接口测试")
    class CheckUsernameTest {

        @Test
        @DisplayName("用户名可用 - 不存在")
        void checkUsername_Available() throws Exception {
            performGet(BASE_URL + "/check-username?username=newuser")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("检查失败 - 用户名为空")
        void checkUsername_Fail_EmptyUsername() throws Exception {
            performGet(BASE_URL + "/check-username?username=")
                    .andExpect(status().isBadRequest());
        }
    }

    private UserDO createTestUser(String username) {
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode(TEST_PASSWORD));
        user.setRealPhone(TEST_PHONE);
        user.setRealMail(TEST_MAIL);
        user.setDelFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
