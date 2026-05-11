package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_group")
public class GroupDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String gid;

    private String name;

    private String username;

    private Integer sortOrder;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
