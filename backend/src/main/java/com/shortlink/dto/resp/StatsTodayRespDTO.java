package com.shortlink.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "今日统计响应DTO")
public class StatsTodayRespDTO {

    @Schema(description = "统计日期")
    private LocalDate date;

    @Schema(description = "今日PV")
    private Long pv;

    @Schema(description = "今日UV")
    private Long uv;

    @Schema(description = "今日UIP")
    private Long uip;
}
