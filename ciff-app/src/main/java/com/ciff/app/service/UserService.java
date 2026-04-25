package com.ciff.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.app.entity.UserPO;
import com.ciff.app.mapper.UserMapper;
import com.ciff.common.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserPO getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getUsername, username)
        );
    }

    public UserPO getById(Long id) {
        return userMapper.selectById(id);
    }

    public UserPO createUser(String username, String rawPassword, String role) {
        UserPO user = new UserPO();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(rawPassword));
        user.setRole(role);
        userMapper.insert(user);
        return user;
    }

    public boolean usernameExists(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getUsername, username)
        ) > 0;
    }

    public UserPO getByGithubId(Long githubId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>()
                        .eq(UserPO::getGithubId, githubId)
        );
    }

    public UserPO createGithubUser(String username, Long githubId, String role) {
        UserPO user = new UserPO();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(java.util.UUID.randomUUID().toString()));
        user.setGithubId(githubId);
        user.setRole(role);
        userMapper.insert(user);
        return user;
    }
}
