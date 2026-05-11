package com.shortlink.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortlink.dto.req.LinkCreateReqDTO;
import com.shortlink.dto.req.LinkPageReqDTO;
import com.shortlink.dto.req.LinkUpdateReqDTO;
import com.shortlink.dto.resp.LinkRespDTO;

public interface LinkService {

    LinkRespDTO createLink(LinkCreateReqDTO request);

    Page<LinkRespDTO> pageLinks(LinkPageReqDTO request);

    void updateLink(LinkUpdateReqDTO request);

    void deleteLink(String shortCode);

    String redirect(String shortCode);

    void warmUpCache();
}
