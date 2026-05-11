package com.shortlink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.result.R;
import com.shortlink.dto.req.RecyclePageReqDTO;
import com.shortlink.dto.req.RecycleRecoverReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;
import com.shortlink.service.RecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recycle")
@RequiredArgsConstructor
@Validated
@Tag(name = "回收站管理", description = "回收站查询、恢复、彻底删除接口")
public class RecycleController {

    private final RecycleService recycleService;

    @GetMapping("/page")
    @Operation(summary = "分页查询回收站", description = "分页查询当前用户回收站中的短链接列表，支持按分组筛选")
    public R<Page<LinkRespDTO>> pageRecycle(@Valid RecyclePageReqDTO request) {
        Page<LinkRespDTO> page = recycleService.pageRecycle(request);
        return R.ok(page);
    }

    @PutMapping("/recover")
    @Operation(summary = "恢复短链接", description = "将回收站中的短链接恢复为正常状态")
    public R<Void> recover(@Valid @RequestBody RecycleRecoverReqDTO request) {
        recycleService.recover(request);
        return R.ok();
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "彻底删除", description = "彻底删除回收站中的短链接，删除后不可恢复")
    public R<Void> remove(
            @Parameter(description = "短链接码", required = true)
            @PathVariable @NotBlank(message = "短链接码不能为空") String shortCode) {
        recycleService.remove(shortCode);
        return R.ok();
    }
}
