package com.shortlink.service.impl;

import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.dto.req.GroupCreateReqDTO;
import com.shortlink.dto.req.GroupSortReqDTO;
import com.shortlink.dto.req.GroupUpdateReqDTO;
import com.shortlink.dto.resp.GroupRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("分组服务单元测试")
class GroupServiceImplTest {

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private LinkMapper linkMapper;

    @InjectMocks
    private GroupServiceImpl groupService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_GID = "testgid01";
    private static final String TEST_GROUP_NAME = "测试分组";

    @BeforeEach
    void setUp() {
        UserContext.setUserInfo(1L, TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("分组创建方法测试")
    class CreateGroupTest {

        @Test
        @DisplayName("正常创建 - 成功")
        void createGroup_Success() {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);
            request.setSortOrder(0);

            when(groupMapper.selectCount(any())).thenReturn(0L);
            when(groupMapper.insert(any(GroupDO.class))).thenReturn(1);

            String gid = groupService.createGroup(request);

            assertNotNull(gid);
            verify(groupMapper).insert(any(GroupDO.class));
        }

        @Test
        @DisplayName("创建成功 - 不指定排序值，使用默认值")
        void createGroup_Success_DefaultSortOrder() {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);

            when(groupMapper.selectCount(any())).thenReturn(0L);
            when(groupMapper.insert(any(GroupDO.class))).thenReturn(1);

            String gid = groupService.createGroup(request);

            assertNotNull(gid);
        }

        @Test
        @DisplayName("创建失败 - 用户未登录")
        void createGroup_Fail_UserNotLogin() {
            UserContext.clear();

            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);

            BizException exception = assertThrows(BizException.class, () -> groupService.createGroup(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("创建失败 - 分组名称已存在")
        void createGroup_Fail_GroupNameExists() {
            GroupCreateReqDTO request = new GroupCreateReqDTO();
            request.setName(TEST_GROUP_NAME);

            when(groupMapper.selectCount(any())).thenReturn(1L);

            BizException exception = assertThrows(BizException.class, () -> groupService.createGroup(request));

            assertEquals(BizCodeEnum.GROUP_EXIST.getCode(), exception.getCode());
            verify(groupMapper, never()).insert(any(GroupDO.class));
        }
    }

    @Nested
    @DisplayName("分组查询方法测试")
    class ListGroupsTest {

        @Test
        @DisplayName("正常查询 - 成功")
        void listGroups_Success() {
            List<GroupDO> groupDOList = new ArrayList<>();
            GroupDO groupDO = new GroupDO();
            groupDO.setGid(TEST_GID);
            groupDO.setName(TEST_GROUP_NAME);
            groupDO.setUsername(TEST_USERNAME);
            groupDO.setSortOrder(0);
            groupDOList.add(groupDO);

            when(groupMapper.selectList(any())).thenReturn(groupDOList);

            List<GroupRespDTO> result = groupService.listGroups();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(TEST_GROUP_NAME, result.get(0).getName());
        }

        @Test
        @DisplayName("查询成功 - 空列表")
        void listGroups_Success_EmptyList() {
            when(groupMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<GroupRespDTO> result = groupService.listGroups();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("查询失败 - 用户未登录")
        void listGroups_Fail_UserNotLogin() {
            UserContext.clear();

            BizException exception = assertThrows(BizException.class, () -> groupService.listGroups());

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("分组修改方法测试")
    class UpdateGroupTest {

        @Test
        @DisplayName("修改失败 - 用户未登录")
        void updateGroup_Fail_UserNotLogin() {
            UserContext.clear();

            GroupUpdateReqDTO request = new GroupUpdateReqDTO();
            request.setGid(TEST_GID);
            request.setName("新分组名称");

            BizException exception = assertThrows(BizException.class, () -> groupService.updateGroup(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("修改失败 - 分组不存在")
        void updateGroup_Fail_GroupNotExist() {
            GroupUpdateReqDTO request = new GroupUpdateReqDTO();
            request.setGid(TEST_GID);
            request.setName("新分组名称");

            when(groupMapper.selectOne(any())).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> groupService.updateGroup(request));

            assertEquals(BizCodeEnum.GROUP_NOT_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("分组删除方法测试")
    class DeleteGroupTest {

        @Test
        @DisplayName("删除失败 - 用户未登录")
        void deleteGroup_Fail_UserNotLogin() {
            UserContext.clear();

            BizException exception = assertThrows(BizException.class, () -> groupService.deleteGroup(TEST_GID));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("删除失败 - 分组不存在")
        void deleteGroup_Fail_GroupNotExist() {
            when(groupMapper.selectOne(any())).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> groupService.deleteGroup(TEST_GID));

            assertEquals(BizCodeEnum.GROUP_NOT_EXIST.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("分组排序方法测试")
    class SortGroupsTest {

        @Test
        @DisplayName("排序失败 - 用户未登录")
        void sortGroups_Fail_UserNotLogin() {
            UserContext.clear();

            GroupSortReqDTO request = new GroupSortReqDTO();
            request.setGidList(Arrays.asList(TEST_GID));

            BizException exception = assertThrows(BizException.class, () -> groupService.sortGroups(request));

            assertEquals(BizCodeEnum.USER_NOT_LOGIN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("排序成功 - 空列表")
        void sortGroups_Success_EmptyList() {
            GroupSortReqDTO request = new GroupSortReqDTO();
            request.setGidList(Collections.emptyList());

            assertDoesNotThrow(() -> groupService.sortGroups(request));

            verify(groupMapper, never()).update(any(), any());
        }
    }
}
