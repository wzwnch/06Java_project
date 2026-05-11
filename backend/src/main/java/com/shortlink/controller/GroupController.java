package com.shortlink.controller;

import com.shortlink.common.result.R;
import com.shortlink.dto.req.GroupCreateReqDTO;
import com.shortlink.dto.req.GroupSortReqDTO;
import com.shortlink.dto.req.GroupUpdateReqDTO;
import com.shortlink.dto.resp.GroupRespDTO;
import com.shortlink.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
@Validated
@Tag(name = "短链接分组管理", description = "分组增删改查、排序等接口")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @Operation(summary = "新增分组", description = "创建新的短链接分组，分组名称在同一用户下不可重复")
    public R<String> createGroup(@Valid @RequestBody GroupCreateReqDTO request) {
        String gid = groupService.createGroup(request);
        return R.ok(gid);
    }

    @GetMapping("/list")
    @Operation(summary = "查询分组列表", description = "查询当前用户所有分组，按排序值升序排列")
    public R<List<GroupRespDTO>> listGroups() {
        List<GroupRespDTO> groups = groupService.listGroups();
        return R.ok(groups);
    }

    @PutMapping
    @Operation(summary = "修改分组", description = "修改分组名称和排序值，分组名称在同一用户下不可重复")
    public R<Void> updateGroup(@Valid @RequestBody GroupUpdateReqDTO request) {
        groupService.updateGroup(request);
        return R.ok();
    }

    @DeleteMapping("/{gid}")
    @Operation(summary = "删除分组", description = "删除指定分组，分组下有短链接时无法删除")
    public R<Void> deleteGroup(
            @Parameter(description = "分组唯一标识", required = true)
            @PathVariable @NotBlank(message = "分组标识不能为空") String gid) {
        groupService.deleteGroup(gid);
        return R.ok();
    }

    @PutMapping("/sort")
    @Operation(summary = "分组排序", description = "批量更新分组排序，按传入的gid列表顺序设置排序值")
    public R<Void> sortGroups(@Valid @RequestBody GroupSortReqDTO request) {
        groupService.sortGroups(request);
        return R.ok();
    }
}
