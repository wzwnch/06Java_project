package com.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.dto.req.GroupCreateReqDTO;
import com.shortlink.dto.req.GroupSortReqDTO;
import com.shortlink.dto.req.GroupUpdateReqDTO;
import com.shortlink.dto.resp.GroupRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.service.GroupService;
import com.shortlink.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final LinkMapper linkMapper;

    @Override
    public String createGroup(GroupCreateReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String name = request.getName();
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, username)
                .eq(GroupDO::getName, name)
                .eq(GroupDO::getDelFlag, 0);
        Long count = groupMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BizException(BizCodeEnum.GROUP_EXIST.getCode(), BizCodeEnum.GROUP_EXIST.getMessage());
        }

        GroupDO groupDO = new GroupDO();
        String gid = IdUtil.fastSimpleUUID().substring(0, 8);
        groupDO.setGid(gid);
        groupDO.setName(name);
        groupDO.setUsername(username);
        groupDO.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        groupDO.setDelFlag(0);
        groupDO.setCreateTime(LocalDateTime.now());
        groupDO.setUpdateTime(LocalDateTime.now());

        groupMapper.insert(groupDO);

        return gid;
    }

    @Override
    public List<GroupRespDTO> listGroups() {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0)
                .orderByAsc(GroupDO::getSortOrder)
                .orderByDesc(GroupDO::getCreateTime);

        List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);

        return groupDOList.stream()
                .map(groupDO -> BeanUtil.copyProperties(groupDO, GroupRespDTO.class))
                .toList();
    }

    @Override
    public void updateGroup(GroupUpdateReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String gid = request.getGid();
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO existGroup = groupMapper.selectOne(queryWrapper);
        if (existGroup == null) {
            throw new BizException(BizCodeEnum.GROUP_NOT_EXIST.getCode(), BizCodeEnum.GROUP_NOT_EXIST.getMessage());
        }

        String newName = request.getName();
        if (!existGroup.getName().equals(newName)) {
            LambdaQueryWrapper<GroupDO> nameCheckWrapper = new LambdaQueryWrapper<>();
            nameCheckWrapper.eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getName, newName)
                    .eq(GroupDO::getDelFlag, 0);
            Long count = groupMapper.selectCount(nameCheckWrapper);
            if (count > 0) {
                throw new BizException(BizCodeEnum.GROUP_EXIST.getCode(), BizCodeEnum.GROUP_EXIST.getMessage());
            }
        }

        LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0)
                .set(GroupDO::getName, newName)
                .set(GroupDO::getUpdateTime, LocalDateTime.now());

        if (request.getSortOrder() != null) {
            updateWrapper.set(GroupDO::getSortOrder, request.getSortOrder());
        }

        groupMapper.update(null, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        if (StrUtil.isBlank(gid)) {
            throw new BizException(BizCodeEnum.GROUP_NOT_EXIST.getCode(), BizCodeEnum.GROUP_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO existGroup = groupMapper.selectOne(queryWrapper);
        if (existGroup == null) {
            throw new BizException(BizCodeEnum.GROUP_NOT_EXIST.getCode(), BizCodeEnum.GROUP_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<LinkDO> linkQueryWrapper = new LambdaQueryWrapper<>();
        linkQueryWrapper.eq(LinkDO::getGid, gid)
                .eq(LinkDO::getDelFlag, 0);
        Long linkCount = linkMapper.selectCount(linkQueryWrapper);
        if (linkCount > 0) {
            throw new BizException(BizCodeEnum.GROUP_DELETE_ERROR.getCode(), BizCodeEnum.GROUP_DELETE_ERROR.getMessage());
        }

        LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0)
                .set(GroupDO::getDelFlag, 1)
                .set(GroupDO::getUpdateTime, LocalDateTime.now());

        groupMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortGroups(GroupSortReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        List<String> gidList = request.getGidList();
        if (gidList == null || gidList.isEmpty()) {
            return;
        }

        for (int i = 0; i < gidList.size(); i++) {
            String gid = gidList.get(i);
            LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GroupDO::getGid, gid)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0)
                    .set(GroupDO::getSortOrder, i)
                    .set(GroupDO::getUpdateTime, LocalDateTime.now());

            groupMapper.update(null, updateWrapper);
        }
    }
}
