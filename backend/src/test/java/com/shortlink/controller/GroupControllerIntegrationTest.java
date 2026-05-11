package com.shortlink.controller;

import com.shortlink.BaseIntegrationTest;
import com.shortlink.dto.req.GroupCreateReqDTO;
import com.shortlink.dto.req.GroupSortReqDTO;
import com.shortlink.dto.req.GroupUpdateReqDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.UserMapper;
import com.shortlink.utils.PasswordUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("分组模块接口集成测试")
class GroupControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String BASE_URL = "/api/group";
    private static final String TEST_USERNAME = "grouptest";
    private static final String TEST_PASSWORD = "Test123456";
    private static final String TEST_GROUP_NAME = "测试分组";

    private UserDO testUser;
    private String testToken;

    @BeforeAll
    void setupAll() {
        cleanupTestData();
        testUser = createTestUser();
        testToken = generateTestToken(testUser.getId(), TEST_USERNAME);
    }

    @BeforeEach
    void setup() {
        cleanupGroupData();
    }

    @AfterAll
    void teardown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            groupMapper.deleteByUsername(TEST_USERNAME);
            userMapper.deleteByUsername(TEST_USERNAME);
        } catch (Exception ignored) {
        }
    }

    private void cleanupGroupData() {
        try {
            groupMapper.deleteByUsername(TEST_USERNAME);
        } catch (Exception ignored) {
        }
    }

    @Nested
    @DisplayName("新增分组接口测试")
    class CreateGroupTest {

        @Test
        @DisplayName("正常创建 - 成功")
        void createGroup_Success() throws Exception {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);
            request.setSortOrder(0);

            MvcResult result = performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").exists())
                    .andReturn();

            String gid = parseResponse(result, String.class);
            assertNotNull(gid);
            assertEquals(8, gid.length());
        }

        @Test
        @DisplayName("创建成功 - 不指定排序值")
        void createGroup_Success_DefaultSortOrder() throws Exception {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName("分组2");

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("创建失败 - 未登录")
        void createGroup_Fail_NotLogin() throws Exception {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);

            performPost(BASE_URL, request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("创建失败 - 分组名称已存在")
        void createGroup_Fail_NameExists() throws Exception {
            createTestGroup(TEST_GROUP_NAME);

            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3002));
        }

        @Test
        @DisplayName("创建失败 - 分组名称为空")
        void createGroup_Fail_EmptyName() throws Exception {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName("");

            performPostWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("查询分组列表接口测试")
    class ListGroupsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void listGroups_Success() throws Exception {
            createTestGroup("分组1");
            createTestGroup("分组2");

            MvcResult result = performGetWithToken(BASE_URL + "/list", testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void listGroups_Success_EmptyList() throws Exception {
            performGetWithToken(BASE_URL + "/list", testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("查询失败 - 未登录")
        void listGroups_Fail_NotLogin() throws Exception {
            performGet(BASE_URL + "/list")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("修改分组接口测试")
    class UpdateGroupTest {

        @Test
        @DisplayName("正常修改 - 成功")
        void updateGroup_Success() throws Exception {
            GroupDO group = createTestGroup(TEST_GROUP_NAME);

            GroupUpdateReqDTO request = new GroupUpdateReqDTO();
            request.setGid(group.getGid());
            request.setName("新分组名");

            performPutWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("修改失败 - 未登录")
        void updateGroup_Fail_NotLogin() throws Exception {
            GroupUpdateReqDTO request = new GroupUpdateReqDTO();
            request.setGid("testgid");
            request.setName("新分组名");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put(BASE_URL)
                    .contentType("application/json")
                    .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("修改失败 - 分组不存在")
        void updateGroup_Fail_GroupNotExist() throws Exception {
            GroupUpdateReqDTO request = new GroupUpdateReqDTO();
            request.setGid("notexist");
            request.setName("新分组名");

            performPutWithToken(BASE_URL, request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001));
        }
    }

    @Nested
    @DisplayName("删除分组接口测试")
    class DeleteGroupTest {

        @Test
        @DisplayName("删除失败 - 未登录")
        void deleteGroup_Fail_NotLogin() throws Exception {
            performDeleteWithToken(BASE_URL + "/testgid", null)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("删除失败 - 分组不存在")
        void deleteGroup_Fail_GroupNotExist() throws Exception {
            performDeleteWithToken(BASE_URL + "/notexist", testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(3001));
        }
    }

    @Nested
    @DisplayName("分组排序接口测试")
    class SortGroupsTest {

        @Test
        @DisplayName("正常排序 - 成功")
        void sortGroups_Success() throws Exception {
            GroupDO group1 = createTestGroup("分组1");
            GroupDO group2 = createTestGroup("分组2");

            GroupSortReqDTO request = new GroupSortReqDTO();
            request.setGidList(Arrays.asList(group2.getGid(), group1.getGid()));

            performPutWithToken(BASE_URL + "/sort", request, testToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("排序失败 - 空列表")
        void sortGroups_Fail_EmptyList() throws Exception {
            GroupSortReqDTO request = new GroupSortReqDTO();
            request.setGidList(List.of());

            performPutWithToken(BASE_URL + "/sort", request, testToken)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("排序失败 - 未登录")
        void sortGroups_Fail_NotLogin() throws Exception {
            GroupSortReqDTO request = new GroupSortReqDTO();
            request.setGidList(Arrays.asList("gid1"));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put(BASE_URL + "/sort")
                    .contentType("application/json")
                    .content(asJsonString(request)))
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

    private GroupDO createTestGroup(String name) {
        GroupDO group = new GroupDO();
        group.setGid(generateGid());
        group.setName(name);
        group.setUsername(TEST_USERNAME);
        group.setSortOrder(0);
        group.setDelFlag(0);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.insert(group);
        return group;
    }

    private String generateGid() {
        return String.valueOf(System.currentTimeMillis() % 100000000);
    }
}
