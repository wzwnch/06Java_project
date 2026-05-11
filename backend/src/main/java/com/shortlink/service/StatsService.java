package com.shortlink.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.dto.LinkAccessLogDTO;
import com.shortlink.dto.req.StatsLogPageReqDTO;
import com.shortlink.dto.resp.StatsHistoryRespDTO;
import com.shortlink.dto.resp.StatsRespDTO;
import com.shortlink.dto.resp.StatsTodayRespDTO;
import com.shortlink.entity.LinkStatsDO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface StatsService {

    void recordAccessLog(String shortCode, String gid, HttpServletRequest request);

    void updateStats(LinkAccessLogDTO accessLog);

    StatsRespDTO getLinkStats(String shortCode);

    Page<LinkStatsDO> pageAccessLog(StatsLogPageReqDTO request);

    StatsTodayRespDTO getTodayStats(String shortCode);

    List<StatsHistoryRespDTO> getHistoryStats(String shortCode, String startDate, String endDate);

    StatsRespDTO getGroupStats(String gid);

    List<String> getHighFreqIp(String shortCode, Integer limit);
}
