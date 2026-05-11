package com.shortlink.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "访问日志分页查询请求DTO")
public class StatsLogPageReqDTO {

    @Schema(description = "短链接码")
    @Size(max = 16, message = "短链接码长度不能超过16个字符")
    private String shortCode;

    @Schema(description = "分组唯一标识")
    @Size(max = 32, message = "分组标识长度不能超过32个字符")
    private String gid;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "当前页码，默认1")
    @Min(value = 1, message = "页码最小为1")
    private Integer current = 1;

    @Schema(description = "每页大小，默认10")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;
}
