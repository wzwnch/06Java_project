package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.LinkStatsDO;
import com.shortlink.entity.LinkStatsTodayDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatsMapper extends BaseMapper<LinkStatsDO> {

    void insertStatsToday(LinkStatsTodayDO statsToday);

    void updateStatsToday(@Param("entity") LinkStatsTodayDO statsToday);

    LinkStatsTodayDO selectStatsTodayByShortCodeAndDate(@Param("shortCode") String shortCode, @Param("date") String date);

    List<LinkStatsTodayDO> selectStatsTodayByShortCode(@Param("shortCode") String shortCode);

    List<LinkStatsTodayDO> selectStatsTodayByGid(@Param("gid") String gid);

    List<LinkStatsTodayDO> selectStatsTodayByGidAndDateRange(@Param("gid") String gid, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
