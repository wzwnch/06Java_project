package com.shortlink.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "统计响应DTO")
public class StatsRespDTO {

    @Schema(description = "短链接码")
    private String shortCode;

    @Schema(description = "分组唯一标识")
    private String gid;

    @Schema(description = "PV总数")
    private Long pv;

    @Schema(description = "UV总数")
    private Long uv;

    @Schema(description = "UIP总数")
    private Long uip;
}
