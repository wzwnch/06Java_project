package com.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.shortlink.service.LinkService;
import com.shortlink.utils.LinkUtils;
import com.shortlink.utils.RedisFallbackHandler;
import com.shortlink.utils.UrlUtils;
import com.shortlink.utils.UserContext;
import com.shortlink.utils.WebInfoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final LinkMapper linkMapper;
    private final GroupMapper groupMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final BloomFilterInitializer bloomFilterInitializer;

    @Value("${shortlink.domain:http://localhost:8080}")
    private String shortLinkDomain;

    private static final String LINK_CACHE_KEY_PREFIX = "link:info:";
    private static final String LINK_NULL_CACHE_PREFIX = "link:null:";
    private static final String LINK_LOCK_PREFIX = "link:lock:";
    private static final long LINK_CACHE_EXPIRE_HOURS = 1L;
    private static final long NULL_CACHE_EXPIRE_MINUTES = 5L;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 10L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LinkRespDTO createLink(LinkCreateReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String gid = request.getGid();
        validateGroup(gid, username);

        String originUrl = request.getOriginUrl();
        if (!UrlUtils.isValid(originUrl)) {
            throw new BizException(BizCodeEnum.LINK_URL_INVALID.getCode(), BizCodeEnum.LINK_URL_INVALID.getMessage());
        }

        WebInfoUtils.WebInfo webInfo = WebInfoUtils.fetch(originUrl);
        String title = webInfo.getTitle();
        String faviconUrl = webInfo.getFaviconUrl();

        String shortCode = generateUniqueShortCode(request.getCustomCode());

        LinkDO linkDO = new LinkDO();
        linkDO.setShortCode(shortCode);
        linkDO.setGid(gid);
        linkDO.setOriginUrl(originUrl);
        linkDO.setFaviconUrl(faviconUrl);
        linkDO.setTitle(title);
        linkDO.setExpireTime(request.getExpireTime());
        linkDO.setStatus(LinkStatusEnum.NORMAL.getCode());
        linkDO.setDelFlag(0);
        linkDO.setCreateTime(LocalDateTime.now());
        linkDO.setUpdateTime(LocalDateTime.now());

        linkMapper.insert(linkDO);

        String finalShortCode = shortCode;
        LinkDO finalLinkDO = linkDO;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        warmUpCache(finalShortCode, finalLinkDO);
                        bloomFilterInitializer.addShortCodeToBloomFilter(finalShortCode);
                    } catch (Exception e) {
                        log.error("事务提交后缓存操作失败, shortCode: {}", finalShortCode, e);
                    }
                }
            });
        } else {
            warmUpCache(shortCode, linkDO);
            bloomFilterInitializer.addShortCodeToBloomFilter(shortCode);
        }

        return buildLinkRespDTO(linkDO);
    }

    @Override
    public Page<LinkRespDTO> pageLinks(LinkPageReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String gid = request.getGid();
        if (StrUtil.isNotBlank(gid)) {
            validateGroup(gid, username);
        }

        List<String> userGids = getUserGroupIds(username);

        if (userGids.isEmpty()) {
            Page<LinkRespDTO> emptyPage = new Page<>();
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        Page<LinkDO> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode());
        
        if (StrUtil.isNotBlank(gid)) {
            queryWrapper.eq(LinkDO::getGid, gid);
        } else {
            queryWrapper.in(LinkDO::getGid, userGids);
        }

        queryWrapper.orderByDesc(LinkDO::getCreateTime);

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

    private List<String> getUserGroupIds(String username) {
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, username)
                .eq(GroupDO::getDelFlag, 0)
                .select(GroupDO::getGid);
        
        List<GroupDO> groups = groupMapper.selectList(queryWrapper);
        return groups.stream()
                .map(GroupDO::getGid)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLink(LinkUpdateReqDTO request) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        String shortCode = request.getShortCode();
        LinkDO existLink = validateLink(shortCode, username);

        String newGid = request.getGid();
        if (StrUtil.isNotBlank(newGid) && !newGid.equals(existLink.getGid())) {
            validateGroup(newGid, username);
        }

        String newOriginUrl = request.getOriginUrl();
        if (StrUtil.isNotBlank(newOriginUrl) && !newOriginUrl.equals(existLink.getOriginUrl())) {
            if (!UrlUtils.isValid(newOriginUrl)) {
                throw new BizException(BizCodeEnum.LINK_URL_INVALID.getCode(), BizCodeEnum.LINK_URL_INVALID.getMessage());
            }
        }

        LambdaUpdateWrapper<LinkDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LinkDO::getShortCode, shortCode);

        if (StrUtil.isNotBlank(newGid)) {
            updateWrapper.set(LinkDO::getGid, newGid);
        }
        if (StrUtil.isNotBlank(newOriginUrl)) {
            updateWrapper.set(LinkDO::getOriginUrl, newOriginUrl);
        }
        if (request.getExpireTime() != null) {
            updateWrapper.set(LinkDO::getExpireTime, request.getExpireTime());
        }

        updateWrapper.set(LinkDO::getUpdateTime, LocalDateTime.now());

        linkMapper.update(null, updateWrapper);

        invalidateCache(shortCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLink(String shortCode) {
        String username = UserContext.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        if (StrUtil.isBlank(shortCode)) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode());
        LinkDO existLink = linkMapper.selectOne(queryWrapper);

        if (existLink == null) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        validateGroup(existLink.getGid(), username);

        LambdaUpdateWrapper<LinkDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LinkDO::getShortCode, shortCode)
                .set(LinkDO::getStatus, LinkStatusEnum.RECYCLE.getCode())
                .set(LinkDO::getUpdateTime, LocalDateTime.now());

        linkMapper.update(null, updateWrapper);

        invalidateCache(shortCode);
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

    private LinkDO validateLink(String shortCode, String username) {
        if (StrUtil.isBlank(shortCode)) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode());
        LinkDO linkDO = linkMapper.selectOne(queryWrapper);

        if (linkDO == null) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        validateGroup(linkDO.getGid(), username);

        return linkDO;
    }

    private String generateUniqueShortCode(String customCode) {
        if (StrUtil.isNotBlank(customCode)) {
            String lockKey = LINK_LOCK_PREFIX + "create:" + customCode;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BizException(BizCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
                }
                try {
                    LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(LinkDO::getShortCode, customCode);
                    Long count = linkMapper.selectCount(queryWrapper);
                    if (count > 0) {
                        throw new BizException(BizCodeEnum.LINK_SHORT_CODE_EXIST.getCode(), BizCodeEnum.LINK_SHORT_CODE_EXIST.getMessage());
                    }
                    return customCode;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取分布式锁被中断, customCode: {}", customCode, e);
                throw new BizException(BizCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
            }
        }

        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            String shortCode = LinkUtils.generateShortCode();
            String lockKey = LINK_LOCK_PREFIX + "create:" + shortCode;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                if (!locked) {
                    retryCount++;
                    continue;
                }
                try {
                    LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(LinkDO::getShortCode, shortCode);
                    Long count = linkMapper.selectCount(queryWrapper);
                    
                    if (count == 0) {
                        return shortCode;
                    }
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取分布式锁被中断, shortCode: {}", shortCode, e);
            }
            retryCount++;
        }

        throw new BizException(BizCodeEnum.SYSTEM_ERROR.getCode(), "生成短链接码失败，请重试");
    }

    private void warmUpCache(String shortCode, LinkDO linkDO) {
        String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
        String nullCacheKey = LINK_NULL_CACHE_PREFIX + shortCode;
        redisTemplate.delete(nullCacheKey);
        redisTemplate.opsForValue().set(cacheKey, linkDO, LINK_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void invalidateCache(String shortCode) {
        String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
        redisTemplate.delete(cacheKey);
    }

    private LinkRespDTO buildLinkRespDTO(LinkDO linkDO) {
        LinkRespDTO respDTO = BeanUtil.copyProperties(linkDO, LinkRespDTO.class);
        respDTO.setShortUrl(shortLinkDomain + "/" + linkDO.getShortCode());
        return respDTO;
    }

    @Override
    public String redirect(String shortCode) {
        if (StrUtil.isBlank(shortCode)) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        if (!RedisFallbackHandler.isRedisAvailable()) {
            log.warn("Redis不可用，降级直接查询数据库, shortCode: {}", shortCode);
            return redirectFromDatabase(shortCode);
        }

        try {
            return redirectWithCache(shortCode);
        } catch (Exception e) {
            if (isRedisConnectionError(e)) {
                log.warn("Redis连接异常，降级直接查询数据库, shortCode: {}", shortCode);
                RedisFallbackHandler.markRedisUnavailable();
                return redirectFromDatabase(shortCode);
            }
            throw e;
        }
    }

    private String redirectWithCache(String shortCode) {
        if (!bloomFilterInitializer.containsShortCode(shortCode)) {
            String nullCacheKey = LINK_NULL_CACHE_PREFIX + shortCode;
            String nullCache = stringRedisTemplate.opsForValue().get(nullCacheKey);
            if (nullCache != null) {
                throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
            }
            LinkDO linkDO = queryLinkFromDb(shortCode);
            if (linkDO == null) {
                stringRedisTemplate.opsForValue().set(nullCacheKey, "1", NULL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
            }
            return processLinkRedirect(linkDO);
        }

        String cacheKey = LINK_CACHE_KEY_PREFIX + shortCode;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            if (cachedObj instanceof LinkDO) {
                LinkDO linkDO = (LinkDO) cachedObj;
                return processLinkRedirect(linkDO);
            }
        }

        String nullCacheKey = LINK_NULL_CACHE_PREFIX + shortCode;
        String nullCache = stringRedisTemplate.opsForValue().get(nullCacheKey);
        if (nullCache != null) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }

        String lockKey = LINK_LOCK_PREFIX + shortCode;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                LinkDO linkDO = queryLinkFromDb(shortCode);
                if (linkDO == null) {
                    throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
                }
                return processLinkRedirect(linkDO);
            }

            try {
                Object doubleCheckObj = redisTemplate.opsForValue().get(cacheKey);
                if (doubleCheckObj != null && doubleCheckObj instanceof LinkDO) {
                    LinkDO linkDO = (LinkDO) doubleCheckObj;
                    return processLinkRedirect(linkDO);
                }

                LinkDO linkDO = queryLinkFromDb(shortCode);
                if (linkDO == null) {
                    stringRedisTemplate.opsForValue().set(nullCacheKey, "1", NULL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                    throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
                }

                redisTemplate.opsForValue().set(cacheKey, linkDO, LINK_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                return processLinkRedirect(linkDO);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, shortCode: {}", shortCode, e);
            throw new BizException(BizCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
        }
    }

    private String redirectFromDatabase(String shortCode) {
        LinkDO linkDO = queryLinkFromDb(shortCode);
        if (linkDO == null) {
            throw new BizException(BizCodeEnum.LINK_NOT_EXIST.getCode(), BizCodeEnum.LINK_NOT_EXIST.getMessage());
        }
        return processLinkRedirect(linkDO);
    }

    private boolean isRedisConnectionError(Exception e) {
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("RedisConnectionFailureException") ||
               exceptionName.contains("RedisConnectionException") ||
               exceptionName.contains("JedisConnectionException") ||
               exceptionName.contains("ConnectionException") ||
               e.getCause() != null && isRedisConnectionError(e.getCause());
    }

    private boolean isRedisConnectionError(Throwable e) {
        if (e == null) {
            return false;
        }
        String exceptionName = e.getClass().getName();
        return exceptionName.contains("RedisConnectionFailureException") ||
               exceptionName.contains("RedisConnectionException") ||
               exceptionName.contains("JedisConnectionException") ||
               exceptionName.contains("ConnectionException") ||
               isRedisConnectionError(e.getCause());
    }

    private LinkDO queryLinkFromDb(String shortCode) {
        LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LinkDO::getShortCode, shortCode)
                .eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode());
        return linkMapper.selectOne(queryWrapper);
    }

    private String processLinkRedirect(LinkDO linkDO) {
        if (linkDO.getExpireTime() != null && linkDO.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(BizCodeEnum.LINK_EXPIRED.getCode(), BizCodeEnum.LINK_EXPIRED.getMessage());
        }
        return linkDO.getOriginUrl();
    }

    @Override
    public void warmUpCache() {
        log.info("开始预热短链接缓存...");
        try {
            LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LinkDO::getStatus, LinkStatusEnum.NORMAL.getCode())
                    .and(w -> w.isNull(LinkDO::getExpireTime)
                            .or()
                            .gt(LinkDO::getExpireTime, LocalDateTime.now()))
                    .orderByDesc(LinkDO::getCreateTime)
                    .last("LIMIT 1000");
            List<LinkDO> hotLinks = linkMapper.selectList(queryWrapper);

            int count = 0;
            for (LinkDO linkDO : hotLinks) {
                String cacheKey = LINK_CACHE_KEY_PREFIX + linkDO.getShortCode();
                Boolean hasKey = redisTemplate.hasKey(cacheKey);
                if (hasKey == null || !hasKey) {
                    redisTemplate.opsForValue().set(cacheKey, linkDO, LINK_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    count++;
                }
            }
            log.info("短链接缓存预热完成，预热数量: {}", count);
        } catch (Exception e) {
            log.error("短链接缓存预热失败", e);
        }
    }
}
