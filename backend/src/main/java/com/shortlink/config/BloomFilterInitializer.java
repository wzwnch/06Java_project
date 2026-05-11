package com.shortlink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shortlink.entity.LinkDO;
import com.shortlink.entity.UserDO;
import com.shortlink.mapper.LinkMapper;
import com.shortlink.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements ApplicationRunner {

    private final RedissonClient redissonClient;
    private final UserMapper userMapper;
    private final LinkMapper linkMapper;

    private static final String USER_BLOOM_FILTER_KEY = "link:user:bloom";
    private static final String SHORT_LINK_BLOOM_FILTER_KEY = "link:short:bloom";
    private static final long EXPECTED_INSERTIONS = 10000000L;
    private static final double FALSE_PROBABILITY = 0.0001;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化布隆过滤器...");
        initUserBloomFilter();
        initShortLinkBloomFilter();
        log.info("布隆过滤器初始化完成");
    }

    private void initUserBloomFilter() {
        log.info("初始化用户名布隆过滤器...");
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);

        try {
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
                log.info("用户名布隆过滤器创建成功，预期插入量: {}, 误判率: {}", EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            }

            LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(UserDO::getUsername);
            List<UserDO> users = userMapper.selectList(queryWrapper);

            int count = 0;
            for (UserDO user : users) {
                if (user.getUsername() != null) {
                    bloomFilter.add(user.getUsername());
                    count++;
                }
            }
            log.info("用户名布隆过滤器初始化完成，已添加 {} 个用户名", count);
        } catch (Exception e) {
            log.error("用户名布隆过滤器初始化失败", e);
        }
    }

    private void initShortLinkBloomFilter() {
        log.info("初始化短链接码布隆过滤器...");
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(SHORT_LINK_BLOOM_FILTER_KEY);

        try {
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
                log.info("短链接码布隆过滤器创建成功，预期插入量: {}, 误判率: {}", EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            }

            LambdaQueryWrapper<LinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(LinkDO::getShortCode);
            List<LinkDO> links = linkMapper.selectList(queryWrapper);

            int count = 0;
            for (LinkDO link : links) {
                if (link.getShortCode() != null) {
                    bloomFilter.add(link.getShortCode());
                    count++;
                }
            }
            log.info("短链接码布隆过滤器初始化完成，已添加 {} 个短链接码", count);
        } catch (Exception e) {
            log.error("短链接码布隆过滤器初始化失败", e);
        }
    }

    public void addShortCodeToBloomFilter(String shortCode) {
        if (shortCode == null) {
            return;
        }
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(SHORT_LINK_BLOOM_FILTER_KEY);
        bloomFilter.add(shortCode);
    }

    public boolean containsShortCode(String shortCode) {
        if (shortCode == null) {
            return false;
        }
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(SHORT_LINK_BLOOM_FILTER_KEY);
        return bloomFilter.contains(shortCode);
    }

    public void addUsernameToBloomFilter(String username) {
        if (username == null) {
            return;
        }
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);
        bloomFilter.add(username);
    }

    public boolean containsUsername(String username) {
        if (username == null) {
            return false;
        }
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(USER_BLOOM_FILTER_KEY);
        return bloomFilter.contains(username);
    }
}
