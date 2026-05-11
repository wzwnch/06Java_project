package com.shortlink.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "短链接响应DTO")
public class LinkRespDTO {

    @Schema(description = "短链接码")
    private String shortCode;

    @Schema(description = "完整短链接")
    private String shortUrl;

    @Schema(description = "分组唯一标识")
    private String gid;

    @Schema(description = "原始URL")
    private String originUrl;

    @Schema(description = "网站图标URL")
    private String faviconUrl;

    @Schema(description = "网站标题")
    private String title;

    @Schema(description = "过期时间，为空表示永不过期")
    private LocalDateTime expireTime;

    @Schema(description = "状态：0-正常，1-回收站")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
