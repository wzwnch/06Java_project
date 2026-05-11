package com.shortlink.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.dto.req.RecyclePageReqDTO;
import com.shortlink.dto.req.RecycleRecoverReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;

public interface RecycleService {

    Page<LinkRespDTO> pageRecycle(RecyclePageReqDTO request);

    void recover(RecycleRecoverReqDTO request);

    void remove(String shortCode);
}
