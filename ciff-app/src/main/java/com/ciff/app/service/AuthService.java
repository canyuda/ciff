package com.ciff.app.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ciff.app.dto.auth.LoginRequest;
import com.ciff.app.dto.auth.LoginVO;
import com.ciff.app.dto.auth.RegisterRequest;
import com.ciff.app.dto.auth.UserInfoVO;
import com.ciff.app.entity.UserPO;
import com.ciff.common.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    public LoginVO login(LoginRequest request) {
        UserPO user = userService.getByUsername(request.getUsername());
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        StpUtil.login(user.getId());
        return buildLoginVO(user);
    }

    public UserInfoVO register(RegisterRequest request) {
        if (userService.usernameExists(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        UserPO user = userService.createUser(request.getUsername(), request.getPassword(), request.getRole());
        return toUserInfoVO(user);
    }

    public UserInfoVO getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserPO user = userService.getById(userId);
        if (user == null) {
            throw new IllegalStateException("User not found");
        }
        return toUserInfoVO(user);
    }

    public void logout() {
        StpUtil.logout();
    }

    private LoginVO buildLoginVO(UserPO user) {
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(toUserInfoVO(user));
        return vo;
    }

    private UserInfoVO toUserInfoVO(UserPO user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        return vo;
    }
}
