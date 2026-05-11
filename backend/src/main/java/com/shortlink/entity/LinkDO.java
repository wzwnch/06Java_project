package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_link")
public class LinkDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shortCode;

    private String gid;

    private String originUrl;

    private String faviconUrl;

    private String title;

    private LocalDateTime expireTime;

    private Integer status;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
