package com.shortlink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.common.result.R;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.LinkPageReqDTO;
import com.shortlink.dto.req.LinkUpdateReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;
import com.shortlink.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/link")
@RequiredArgsConstructor
@Validated
@Tag(name = "短链接管理", description = "短链接增删改查接口")
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    @Operation(summary = "新增短链接", description = "创建新的短链接，支持自定义短链接码，自动获取目标网站标题和图标")
    public R<LinkRespDTO> createLink(@Valid @RequestBody LinkCreateReqDTO request) {
        LinkRespDTO linkRespDTO = linkService.createLink(request);
        return R.ok(linkRespDTO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询短链接", description = "分页查询当前用户指定分组下的短链接列表")
    public R<Page<LinkRespDTO>> pageLinks(@Valid LinkPageReqDTO request) {
        Page<LinkRespDTO> page = linkService.pageLinks(request);
        return R.ok(page);
    }

    @PutMapping
    @Operation(summary = "修改短链接", description = "修改短链接的目标地址、分组归属、有效期等信息")
    public R<Void> updateLink(@Valid @RequestBody LinkUpdateReqDTO request) {
        linkService.updateLink(request);
        return R.ok();
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "删除短链接", description = "删除指定短链接，逻辑删除移入回收站")
    public R<Void> deleteLink(
            @Parameter(description = "短链接码", required = true)
            @PathVariable @NotBlank(message = "短链接码不能为空") String shortCode) {
        linkService.deleteLink(shortCode);
        return R.ok();
    }
}
