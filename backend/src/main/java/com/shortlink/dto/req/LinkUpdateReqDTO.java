package com.shortlink.dto.req;

import com.shortlink.validation.Url;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "修改短链接请求DTO")
public class LinkUpdateReqDTO {

    @Schema(description = "短链接码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "短链接码不能为空")
    private String shortCode;

    @Schema(description = "分组唯一标识")
    private String gid;

    @Schema(description = "原始URL")
    @Url(message = "原始URL格式不合法，必须以http://或https://开头")
    private String originUrl;

    @Schema(description = "过期时间，为空表示永不过期")
    private LocalDateTime expireTime;
}
