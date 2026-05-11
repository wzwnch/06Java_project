package com.shortlink.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "恢复短链接请求DTO")
public class RecycleRecoverReqDTO {

    @Schema(description = "短链接码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "短链接码不能为空")
    private String shortCode;
}
