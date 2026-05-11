package com.shortlink.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
import com.shortlink.service.UserService;
import com.shortlink.utils.EncryptUtils;
import com.shortlink.utils.JwtUtils;
import com.shortlink.utils.PasswordUtils;
import com.shortlink.utils.SensitiveUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String USER_BLOOM_FILTER_KEY = "link:user:bloom";
    private static final String TOKEN_BLACKLIST_PREFIX = "link:token:blacklist:";
    private static final String USERNAME_NULL_CACHE_PREFIX = "link:user:null:";
    private static final long NULL_CACHE_EXPIRE_MINUTES = 5;
    private static final String DEFAULT_GROUP_NAME = "默认分组";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterReqDTO request) {
        String username = request.getUsername();

        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);
        if (bloomFilter.contains(username)) {
            LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserDO::getUsername, username);
            Long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BizException(BizCodeEnum.USER_EXIST.getCode(), BizCodeEnum.USER_EXIST.getMessage());
            }
        }

        if (StrUtil.isNotBlank(request.getPhone())) {
            String encryptedPhone = EncryptUtils.encrypt(request.getPhone());
            LambdaQueryWrapper<UserDO> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(UserDO::getPhone, encryptedPhone);
            Long phoneCount = userMapper.selectCount(phoneQuery);
            if (phoneCount > 0) {
                throw new BizException(BizCodeEnum.USER_PHONE_EXIST.getCode(), BizCodeEnum.USER_PHONE_EXIST.getMessage());
            }
        }

        UserDO userDO = new UserDO();
        userDO.setUsername(username);
        userDO.setPassword(PasswordUtils.encode(request.getPassword()));

        if (StrUtil.isNotBlank(request.getPhone())) {
            userDO.setPhone(EncryptUtils.encrypt(request.getPhone()));
            userDO.setRealPhone(SensitiveUtils.phone(request.getPhone()));
        }

        if (StrUtil.isNotBlank(request.getMail())) {
            userDO.setMail(EncryptUtils.encrypt(request.getMail()));
            userDO.setRealMail(SensitiveUtils.email(request.getMail()));
        }

        userDO.setDelFlag(0);
        userDO.setCreateTime(LocalDateTime.now());
        userDO.setUpdateTime(LocalDateTime.now());

        userMapper.insert(userDO);

        bloomFilter.add(username);

        createDefaultGroup(username);

        log.info("用户注册成功: {}", username);
    }

    private void createDefaultGroup(String username) {
        GroupDO groupDO = new GroupDO();
        groupDO.setGid(IdUtil.fastSimpleUUID().substring(0, 8));
        groupDO.setName(DEFAULT_GROUP_NAME);
        groupDO.setUsername(username);
        groupDO.setSortOrder(0);
        groupDO.setDelFlag(0);
        groupDO.setCreateTime(LocalDateTime.now());
        groupDO.setUpdateTime(LocalDateTime.now());

        groupMapper.insert(groupDO);
    }

    @Override
    public String login(UserLoginReqDTO request) {
        String username = request.getUsername();

        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);
        if (!bloomFilter.contains(username)) {
            String nullCacheKey = USERNAME_NULL_CACHE_PREFIX + username;
            String nullCache = stringRedisTemplate.opsForValue().get(nullCacheKey);
            if (nullCache != null) {
                log.warn("登录失败，用户不存在（空值缓存命中）: {}", username);
                throw new BizException(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), BizCodeEnum.LOGIN_PASSWORD_ERROR.getMessage());
            }
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(queryWrapper);

        if (userDO == null) {
            String nullCacheKey = USERNAME_NULL_CACHE_PREFIX + username;
            stringRedisTemplate.opsForValue().set(nullCacheKey, "1", NULL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.warn("登录失败，用户不存在: {}", username);
            throw new BizException(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), BizCodeEnum.LOGIN_PASSWORD_ERROR.getMessage());
        }

        if (!PasswordUtils.check(request.getPassword(), userDO.getPassword())) {
            log.warn("登录失败，密码错误: {}", username);
            throw new BizException(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), BizCodeEnum.LOGIN_PASSWORD_ERROR.getMessage());
        }

        log.info("用户登录成功: {}", username);
        return JwtUtils.generateToken(userDO.getId(), userDO.getUsername());
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }

        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            log.warn("退出登录失败，无效的token");
            return;
        }

        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        stringRedisTemplate.opsForValue().set(blacklistKey, String.valueOf(userId), 24, TimeUnit.HOURS);
        log.info("用户退出登录成功: userId={}", userId);
    }

    @Override
    public UserInfoRespDTO getUserInfo(Long userId) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST.getCode(), BizCodeEnum.USER_NOT_EXIST.getMessage());
        }

        UserInfoRespDTO respDTO = new UserInfoRespDTO();
        respDTO.setId(userDO.getId());
        respDTO.setUsername(userDO.getUsername());
        respDTO.setPhone(userDO.getRealPhone());
        respDTO.setMail(userDO.getRealMail());
        respDTO.setCreateTime(userDO.getCreateTime());
        respDTO.setUpdateTime(userDO.getUpdateTime());

        return respDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateReqDTO request) {
        if (userId == null) {
            throw new BizException(BizCodeEnum.USER_NOT_LOGIN.getCode(), BizCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BizException(BizCodeEnum.USER_NOT_EXIST.getCode(), BizCodeEnum.USER_NOT_EXIST.getMessage());
        }

        boolean needUpdate = false;

        if (StrUtil.isNotBlank(request.getPhone())) {
            userDO.setPhone(EncryptUtils.encrypt(request.getPhone()));
            userDO.setRealPhone(SensitiveUtils.phone(request.getPhone()));
            needUpdate = true;
        }

        if (StrUtil.isNotBlank(request.getMail())) {
            userDO.setMail(EncryptUtils.encrypt(request.getMail()));
            userDO.setRealMail(SensitiveUtils.email(request.getMail()));
            needUpdate = true;
        }

        if (StrUtil.isNotBlank(request.getOldPassword()) && StrUtil.isNotBlank(request.getNewPassword())) {
            if (!PasswordUtils.check(request.getOldPassword(), userDO.getPassword())) {
                throw new BizException(BizCodeEnum.LOGIN_PASSWORD_ERROR.getCode(), "旧密码错误");
            }
            userDO.setPassword(PasswordUtils.encode(request.getNewPassword()));
            needUpdate = true;
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(userDO);
            log.info("用户信息更新成功: userId={}", userId);
        }
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }

        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);
        if (!bloomFilter.contains(username)) {
            log.debug("用户名可用（布隆过滤器判断不存在）: {}", username);
            return true;
        }

        String nullCacheKey = USERNAME_NULL_CACHE_PREFIX + username;
        String nullCache = stringRedisTemplate.opsForValue().get(nullCacheKey);
        if (nullCache != null) {
            log.debug("用户名可用（空值缓存命中）: {}", username);
            return true;
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username);
        Long count = userMapper.selectCount(queryWrapper);

        if (count == 0) {
            stringRedisTemplate.opsForValue().set(nullCacheKey, "1", NULL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.debug("用户名可用（数据库查询不存在）: {}", username);
            return true;
        }

        log.debug("用户名已存在: {}", username);
        return false;
    }
}
