package com.shortlink.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "历史统计响应DTO")
public class StatsHistoryRespDTO {

    @Schema(description = "统计日期")
    private LocalDate date;

    @Schema(description = "当日PV")
    private Long pv;

    @Schema(description = "当日UV")
    private Long uv;

    @Schema(description = "当日UIP")
    private Long uip;
}
