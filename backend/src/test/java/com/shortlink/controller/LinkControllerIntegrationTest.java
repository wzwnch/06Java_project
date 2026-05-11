package com.shortlink.controller;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.LinkPageReqDTO;
import com.shortlink.dto.req.LinkUpdateReqDTO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("短链接模块接口集成测试")
class LinkControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkMapper linkMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String BASE_URL = "/api/link";
    private static final String TEST_USERNAME = "linktest";
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
    @DisplayName("新增短链接接口测试")
    class CreateLinkTest {

        @Test
        @DisplayName("创建失败 - 未登录")
        void createLink_Fail_NotLogin() throws Exception {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl(TEST_ORIGIN_URL);
            request.setGid(testGroup.getGid());

            performPost(BASE_URL, request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("创建失败 - 分组不存在")
        void createLink_Fail_GroupNotExist() throws Exception {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl(TEST_ORIGIN_URL);
            request.setGid("notexist");

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001));
        }

        @Test
        @DisplayName("创建失败 - URL格式不合法")
        void createLink_Fail_InvalidUrl() throws Exception {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl("invalid-url");
            request.setGid(testGroup.getGid());

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建失败 - URL为空")
        void createLink_Fail_EmptyUrl() throws Exception {
            LinkCreateReqDTO request = new LinkCreateReqDTO();
            request.setOriginUrl("");
            request.setGid(testGroup.getGid());

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("分页查询短链接接口测试")
    class PageLinksTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void pageLinks_Success() throws Exception {
            createTestLink("abc123");

            LinkPageReqDTO request = new LinkPageReqDTO();
            request.setGid(testGroup.getGid());
            request.setCurrent(1);
            request.setSize(10);

            MvcResult result = performGetWithToken(
                    BASE_URL + "/page?gid=" + testGroup.getGid() + "&current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void pageLinks_Success_EmptyList() throws Exception {
            performGetWithToken(
                    BASE_URL + "/page?gid=" + testGroup.getGid() + "&current=1&size=10",
                    testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void pageLinks_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/page?gid=" + testGroup.getGid() + "&current=1&size=10")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改短链接接口测试")
    class UpdateLinkTest {

        @Test
        @DisplayName("修改失败 - 未登录")
        void updateLink_Fail_NotLogin() throws Exception {
            LinkUpdateReqDTO request = new LinkUpdateReqDTO();
            request.setShortCode("abc123");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put(BASE_URL)
                    .contentType("application/json")
                    .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("修改失败 - 短链接不存在")
        void updateLink_Fail_LinkNotExist() throws Exception {
            LinkUpdateReqDTO request = new LinkUpdateReqDTO();
            request.setShortCode("notexist");
            request.setOriginUrl(TEST_ORIGIN_URL);

            performPutWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003));
        }
    }

    @Nested
    @DisplayName("删除短链接接口测试")
    class DeleteLinkTest {

        @Test
        @DisplayName("删除失败 - 未登录")
        void deleteLink_Fail_NotLogin() throws Exception {
            performDeleteWithToken(BASE_URL + "/abc123", null)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("删除失败 - 短链接不存在")
        void deleteLink_Fail_LinkNotExist() throws Exception {
            performDeleteWithToken(BASE_URL + "/notexist", testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(2003));
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

    private String generateGid() {
        return String.valueOf(System.currentTimeMillis() % 100000000);
    }
}
