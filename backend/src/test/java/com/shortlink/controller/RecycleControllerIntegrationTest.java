package com.shortlink.controller;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.dto.req.RecycleRecoverReqDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.PasswordUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("回收站模块接口集成测试")
class RecycleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String BASE_URL = "/api/recycle";
    private static final String TEST_USERNAME = "recycletest";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_ORIGIN_URL = "https://www.example.com";

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
    @DisplayName("分页查询回收站接口测试")
    class PageRecycleTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void pageRecycle_Success() throws Exception {
            createTestLink("recycle01", 1);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/page?current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void pageRecycle_Success_EmptyList() throws Exception {
            performGetWithToken(
                    BASE_URL + "/page?current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void pageRecycle_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/page?current=1&size=10")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("恢复短链接接口测试")
    class RecoverTest {

        @Test
        @DisplayName("恢复失败 - 未登录")
        void recover_Fail_NotLogin() throws Exception {
            RecycleRecoverReqDTO request = new RecycleRecoverReqDTO();
            request.setShortCode("recycle01");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put(BASE_URL + "/recover")
                    .contentType("application/json")
                    .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("恢复失败 - 短链接不存在")
        void recover_Fail_LinkNotExist() throws Exception {
            RecycleRecoverReqDTO request = new RecycleRecoverReqDTO();
            request.setShortCode("notexist");

            performPutWithToken(BASE_URL + "/recover", request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(4002));
        }

        @Test
        @DisplayName("恢复失败 - 短链接不在回收站")
        void recover_Fail_LinkNotInRecycle() throws Exception {
            createTestLink("normal01", 0);

            RecycleRecoverReqDTO request = new RecycleRecoverReqDTO();
            request.setShortCode("normal01");

            performPutWithToken(BASE_URL + "/recover", request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(4002));
        }
    }

    @Nested
    @DisplayName("彻底删除接口测试")
    class RemoveTest {

        @Test
        @DisplayName("删除失败 - 未登录")
        void remove_Fail_NotLogin() throws Exception {
            performDeleteWithToken(BASE_URL + "/recycle01", null)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("删除失败 - 短链接不存在")
        void remove_Fail_LinkNotExist() throws Exception {
            performDeleteWithToken(BASE_URL + "/notexist", testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(4002));
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

    private LinkDO createTestLink(String shortCode, int status) {
        LinkDO link = new LinkDO();
        link.setShortCode(shortCode);
        link.setGid(testGroup.getGid());
        link.setOriginUrl(TEST_ORIGIN_URL);
        link.setStatus(status);
        link.setDelFlag(0);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        linkMapper.insert(link);
        return link;
    }

    private String generateGid() {
        return String.valueOf(System.currentTimeMillis() % 100000000);
    }
}
