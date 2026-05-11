package com.shortlink.config;

import com.shortlink.service.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CacheWarmUpRunner implements ApplicationRunner {

    private final LinkService linkService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始执行缓存预热...");
        try {
            linkService.warmUpCache();
            log.info("缓存预热执行完成");
        } catch (Exception e) {
            log.error("缓存预热执行失败", e);
        }
    }
}
