package com.shortlink.service;

import com.shortlink.dto.req.UserLoginReqDTO;
import com.shortlink.dto.req.UserRegisterReqDTO;
import com.shortlink.dto.req.UserUpdateReqDTO;
import com.shortlink.dto.resp.UserInfoRespDTO;

public interface UserService {

    void register(UserRegisterReqDTO request);

    String login(UserLoginReqDTO request);

    void logout(String token);

    UserInfoRespDTO getUserInfo(Long userId);

    void updateUserInfo(Long userId, UserUpdateReqDTO request);

    boolean isUsernameAvailable(String username);
}
