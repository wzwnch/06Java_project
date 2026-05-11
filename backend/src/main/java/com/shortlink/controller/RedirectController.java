package com.shortlink.controller;

import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.exception.BizException;
import com.shortlink.entity.LinkDO;
import com.shortlink.service.LinkService;
import com.shortlink.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "短链接跳转", description = "短链接跳转接口")
public class RedirectController {

    private final LinkService linkService;
    private final StatsService statsService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LINK_CACHE_KEY_PREFIX = "link:info:";

    @Value("${shortlink.redirect.page-404:/page/404.html}")
    private String page404;

    @Value("${shortlink.redirect.page-expired:/page/expired.html}")
    private String pageExpired;

    @GetMapping("/{shortCode:^(?!doc\\.html$|webjars|swagger|v3).+$}")
    @Operation(summary = "短链接跳转", description = "根据短链接码跳转到目标URL，支持302重定向")
    public void redirect(
            @Parameter(description = "短链接码", required = true)
            @PathVariable @NotBlank(message = "短链接码不能为空") String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {
            String originUrl = linkService.redirect(shortCode);
            
            String gid = getGidFromCache(shortCode);
            if (gid != null) {
                statsService.recordAccessLog(shortCode, gid, request);
            }
            
            response.setStatus(HttpStatus.FOUND.value());
            response.setHeader(HttpHeaders.LOCATION, originUrl);
            log.info("短链接跳转成功, shortCode: {}, originUrl: {}", shortCode, originUrl);
        } catch (BizException e) {
            log.warn("短链接跳转失败, shortCode: {}, error: {}", shortCode, e.getMessage());
            if (BizCodeEnum.LINK_NOT_EXIST.getCode().equals(e.getCode())) {
                redirectToPage(response, page404);
            } else if (BizCodeEnum.LINK_EXPIRED.getCode().equals(e.getCode())) {
                redirectToPage(response, pageExpired);
            } else {
                redirectToPage(response, page404);
            }
        } catch (Exception e) {
            log.error("短链接跳转异常, shortCode: {}", shortCode, e);
            redirectToPage(response, page404);
        }
    }

    private String getGidFromCache(String shortCode) {
        try {
            Object cachedObj = redisTemplate.opsForValue().get(LINK_CACHE_KEY_PREFIX + shortCode);
            if (cachedObj instanceof LinkDO) {
                return ((LinkDO) cachedObj).getGid();
            }
        } catch (Exception e) {
            log.warn("从缓存获取gid失败, shortCode: {}", shortCode, e);
        }
        return null;
    }

    private void redirectToPage(HttpServletResponse response, String page) throws IOException {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, page);
    }
}
