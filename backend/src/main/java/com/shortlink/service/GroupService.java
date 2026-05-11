package com.shortlink.service;

import com.shortlink.dto.req.GroupCreateReqDTO;
import com.shortlink.dto.req.GroupSortReqDTO;
import com.shortlink.dto.req.GroupUpdateReqDTO;
import com.shortlink.dto.resp.GroupRespDTO;

import java.util.List;

public interface GroupService {

    String createGroup(GroupCreateReqDTO request);

    List<GroupRespDTO> listGroups();

    void updateGroup(GroupUpdateReqDTO request);

    void deleteGroup(String gid);

    void sortGroups(GroupSortReqDTO request);
}
