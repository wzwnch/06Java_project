package com.shortlink.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改分组请求DTO")
public class GroupUpdateReqDTO {

    @Schema(description = "分组唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组标识不能为空")
    private String gid;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    @Size(min = 1, max = 64, message = "分组名称长度需在1-64个字符之间")
    private String name;

    @Schema(description = "排序值，越小越靠前")
    private Integer sortOrder;
}
