package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("link_user")
public class UserDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String password;

    private String phone;

    private String mail;

    private String realPhone;

    private String realMail;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
