package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("link_link_stats_today")
public class LinkStatsTodayDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shortCode;

    private String gid;

    private LocalDate date;

    private Long pv;

    private Long uv;

    private Long uip;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
