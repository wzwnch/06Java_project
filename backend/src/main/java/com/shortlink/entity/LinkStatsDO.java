package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_link_stats")
public class LinkStatsDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shortCode;

    private String gid;

    private Long pv;

    private String uv;

    private String uip;

    private String ip;

    private String region;

    private String os;

    private String browser;

    private String device;

    private String network;

    private LocalDateTime createTime;
}
