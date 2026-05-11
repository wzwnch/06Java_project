package com.shortlink.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增分组请求DTO")
public class GroupCreateReqDTO {

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    @Size(min = 1, max = 64, message = "分组名称长度需在1-64个字符之间")
    private String name;

    @Schema(description = "排序值，越小越靠前，默认为0")
    private Integer sortOrder;
}
