package com.shortlink.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "分组响应DTO")
public class GroupRespDTO {

    @Schema(description = "分组唯一标识")
    private String gid;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "排序值，越小越靠前")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
