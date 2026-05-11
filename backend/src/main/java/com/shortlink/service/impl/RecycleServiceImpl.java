package com.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.enums.LinkStatusEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.dto.req.RecyclePageReqDTO;
import com.shortlink.dto.req.RecycleRecoverReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;
import com.shortlink.entity.GroupDO;
import com.shortlink.entity.LinkDO;
import com.shortlink.mapper.GroupMapper;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.service.RecycleService;
import com.shortlink.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleServiceImpl implements RecycleService {

    private final LinkMapper linkMapper;
    private final GroupMapper groupMapper;

    @Value("${shortlink.domain:http://localhost:8080}")
    private String shortLinkDomain;

    @Override
    public Page<LinkRespDTO> pageRecycle(RecyclePageReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String gid = request.getGid();
        if (StrUtil.isNotBlank(gid)) {
            validateGroup(gid, username);
        }

        Page<LinkDO> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode());

        if (StrUtil.isNotBlank(gid)) {
            queryWrapper.eq(LinkDO::getGid, gid);
        } else {
            LambdaQueryWrapper<GroupDO> groupQueryWrapper = new LambdaQueryWrapper<>();
            groupQueryWrapper.eq(GroupDO::getUsername, username);
            var groups = groupMapper.selectList(groupQueryWrapper);
            if (groups.isEmpty()) {
                Page<LinkRespDTO> emptyPage = new Page<>();
                emptyPage.setCurrent(request.getCurrent());
                emptyPage.setSize(request.getSize());
                emptyPage.setTotal(0);
                emptyPage.setRecords(java.util.Collections.emptyList());
                return emptyPage;
            }
            var gids = groups.stream().map(GroupDO::getGid).toList();
            queryWrapper.in(LinkDO::getGid, gids);
        }

        queryWrapper.orderByDesc(LinkDO::getUpdateTime);

        Page<LinkDO> linkDOPage = linkMapper.selectPage(page, queryWrapper);

        Page<LinkRespDTO> resultPage = new Page<>();
        resultPage.setCurrent(linkDOPage.getCurrent());
        resultPage.setSize(linkDOPage.getSize());
        resultPage.setTotal(linkDOPage.getTotal());
        resultPage.setRecords(linkDOPage.getRecords().stream()
                .map(this::buildLinkRespDTO)
                .toList());

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(RecycleRecoverReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String shortCode = request.getShortCode();
        if (StrUtil.isBlank(shortCode)) {
            throw new BizException(BizCodeEnum.RECYCLE_NOT_EXIST.getCode(), BizCodeEnum.RECYCLE_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode());
        LinkDO linkDO = linkMapper.selectOne(queryWrapper);

        if (linkDO == null) {
            throw new BizException(BizCodeEnum.RECYCLE_NOT_EXIST.getCode(), BizCodeEnum.RECYCLE_NOT_EXIST.getMessage());
        }

        validateGroup(linkDO.getGid(), username);

        LambdaQueryWrapper<LinkDO> existQueryWrapper = new LambdaQueryWrapper<>();
        existQueryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode());
        Long existCount = linkMapper.selectCount(existQueryWrapper);
        if (existCount > 0) {
            throw new BizException(BizCodeEnum.RECYCLE_RESTORE_ERROR.getCode(), BizCodeEnum.RECYCLE_RESTORE_ERROR.getMessage());
        }

        LambdaUpdateWrapper<LinkDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode())
                .set(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode())
                .set(LinkDO::getUpdateTime, LocalDateTime.now());

        linkMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String shortCode) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        if (StrUtil.isBlank(shortCode)) {
            throw new BizException(BizCodeEnum.RECYCLE_NOT_EXIST.getCode(), BizCodeEnum.RECYCLE_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode());
        LinkDO linkDO = linkMapper.selectOne(queryWrapper);

        if (linkDO == null) {
            throw new BizException(BizCodeEnum.RECYCLE_NOT_EXIST.getCode(), BizCodeEnum.RECYCLE_NOT_EXIST.getMessage());
        }

        validateGroup(linkDO.getGid(), username);

        LambdaQueryWrapper<LinkDO> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode());

        linkMapper.delete(deleteWrapper);
    }

    private void validateGroup(String gid, String username) {
        if (StrUtil.isBlank(gid)) {
            throw new BizException(BizCodeEnum.GROUP_NOT_EXIST.getCode(), BizCodeEnum.GROUP_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username);
        GroupDO groupDO = groupMapper.selectOne(queryWrapper);

        if (groupDO == null) {
            throw new BizException(BizCodeEnum.GROUP_NOT_EXIST.getCode(), BizCodeEnum.GROUP_NOT_EXIST.getMessage());
        }
    }

    private LinkRespDTO buildLinkRespDTO(LinkDO linkDO) {
        LinkRespDTO respDTO = BeanUtil.copyProperties(linkDO, LinkRespDTO.class);
        respDTO.setShortUrl(shortLinkDomain + "/" + linkDO.getShortCode());
        return respDTO;
    }
}
