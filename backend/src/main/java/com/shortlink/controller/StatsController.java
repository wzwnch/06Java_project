package com.shortlink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.result.R;
import com.shortlink.dto.req.StatsLogPageReqDTO;
import com.shortlink.dto.resp.StatsHistoryRespDTO;
import com.shortlink.dto.resp.StatsRespDTO;
import com.shortlink.dto.resp.StatsTodayRespDTO;
import com.shortlink.entity.LinkStatsDO;
import com.shortlink.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Validated
@Tag(name = "监控统计", description = "短链接访问统计与监控接口")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/link/{shortCode}")
    @Operation(summary = "单链接统计", description = "获取指定短链接的PV/UV/UIP汇总统计数据")
    public R<StatsRespDTO> getLinkStats(
            @Parameter(description = "短链接码", required = true)
            @PathVariable @NotBlank(message = "短链接码不能为空") String shortCode) {
        StatsRespDTO stats = statsService.getLinkStats(shortCode);
        return R.ok(stats);
    }

    @GetMapping("/log/page")
    @Operation(summary = "访问日志查询", description = "分页查询短链接访问日志，支持按短链接码、分组、时间范围筛选")
    public R<Page<LinkStatsDO>> pageAccessLog(@Valid StatsLogPageReqDTO request) {
        Page<LinkStatsDO> page = statsService.pageAccessLog(request);
        return R.ok(page);
    }

    @GetMapping("/today")
    @Operation(summary = "今日统计", description = "获取指定短链接今日的PV/UV/UIP统计数据")
    public R<StatsTodayRespDTO> getTodayStats(
            @Parameter(description = "短链接码", required = true)
            @RequestParam @NotBlank(message = "短链接码不能为空") String shortCode) {
        StatsTodayRespDTO stats = statsService.getTodayStats(shortCode);
        return R.ok(stats);
    }

    @GetMapping("/history")
    @Operation(summary = "历史统计", description = "获取指定短链接历史时间范围内的PV/UV/UIP趋势数据")
    public R<List<StatsHistoryRespDTO>> getHistoryStats(
            @Parameter(description = "短链接码", required = true)
            @RequestParam @NotBlank(message = "短链接码不能为空") String shortCode,
            @Parameter(description = "开始日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @NotBlank(message = "开始日期不能为空") String startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd", required = true)
            @RequestParam @NotBlank(message = "结束日期不能为空") String endDate) {
        List<StatsHistoryRespDTO> history = statsService.getHistoryStats(shortCode, startDate, endDate);
        return R.ok(history);
    }

    @GetMapping("/group/{gid}")
    @Operation(summary = "分组统计", description = "获取指定分组下所有短链接的PV/UV/UIP汇总统计数据")
    public R<StatsRespDTO> getGroupStats(
            @Parameter(description = "分组唯一标识", required = true)
            @PathVariable @NotBlank(message = "分组标识不能为空") String gid) {
        StatsRespDTO stats = statsService.getGroupStats(gid);
        return R.ok(stats);
    }

    @GetMapping("/high-freq-ip")
    @Operation(summary = "高频IP统计", description = "获取指定短链接访问频率最高的IP列表")
    public R<List<String>> getHighFreqIp(
            @Parameter(description = "短链接码", required = true)
            @RequestParam @NotBlank(message = "短链接码不能为空") String shortCode,
            @Parameter(description = "返回数量限制，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<String> highFreqIps = statsService.getHighFreqIp(shortCode, limit);
        return R.ok(highFreqIps);
    }
}
