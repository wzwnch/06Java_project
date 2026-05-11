package com.shortlink.dto.req;

import com.shortlink.validation.Url;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "新增短链接请求DTO")
public class LinkCreateReqDTO {

    @Schema(description = "原始URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始URL不能为空")
    @Url(message = "原始URL格式不合法，必须以http://或https://开头")
    private String originUrl;

    @Schema(description = "分组唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组标识不能为空")
    private String gid;

    @Schema(description = "过期时间，为空表示永不过期")
    private LocalDateTime expireTime;

    @Schema(description = "自定义短链接码，为空则自动生成")
    @Size(min = 4, max = 16, message = "自定义短链接码长度需在4-16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "自定义短链接码只能包含字母和数字")
    private String customCode;
}
