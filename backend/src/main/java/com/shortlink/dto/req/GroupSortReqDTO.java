package com.shortlink.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分组排序请求DTO")
public class GroupSortReqDTO {

    @Schema(description = "分组ID排序列表，按顺序排列", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "排序列表不能为空")
    private List<String> gidList;
}
