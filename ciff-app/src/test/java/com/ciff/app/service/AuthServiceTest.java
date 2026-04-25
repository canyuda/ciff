package com.ciff.app.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ciff.app.dto.auth.LoginRequest;
import com.ciff.app.dto.auth.LoginVO;
import com.ciff.app.dto.auth.RegisterRequest;
import com.ciff.app.dto.auth.UserInfoVO;
import com.ciff.app.entity.UserPO;
import com.ciff.common.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_returnsLoginVO() {
        UserPO user = buildUser(1L, "testuser", "hashedPwd", "user");
        when(userService.getByUsername("testuser")).thenReturn(user);

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class);
             MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {

            passwordUtilMock.when(() -> PasswordUtil.matches("password123", "hashedPwd"))
                    .thenReturn(true);
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("token-abc");

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            LoginVO result = authService.login(request);

            assertThat(result.getToken()).isEqualTo("token-abc");
            assertThat(result.getUser().getId()).isEqualTo(1L);
            assertThat(result.getUser().getUsername()).isEqualTo("testuser");
            assertThat(result.getUser().getRole()).isEqualTo("user");

            stpUtilMock.verify(() -> StpUtil.login(1L));
        }
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userService.getByUsername("unknown")).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_wrongPassword_throwsException() {
        UserPO user = buildUser(1L, "testuser", "hashedPwd", "user");
        when(userService.getByUsername("testuser")).thenReturn(user);

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.matches("wrongPwd", "hashedPwd"))
                    .thenReturn(false);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongPwd");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid username or password");
        }
    }

    @Test
    void register_success_returnsUserInfoVO() {
        when(userService.usernameExists("newuser")).thenReturn(false);
        UserPO created = buildUser(1L, "newuser", "hashed", "user");
        when(userService.createUser("newuser", "password123", "user")).thenReturn(created);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setRole("user");

        UserInfoVO result = authService.register(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getRole()).isEqualTo("user");
    }

    @Test
    void register_usernameExists_throwsException() {
        when(userService.usernameExists("existing")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void getCurrentUser_returnsLoggedInUser() {
        UserPO user = buildUser(1L, "testuser", "hashed", "admin");

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(userService.getById(1L)).thenReturn(user);

            UserInfoVO result = authService.getCurrentUser();

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getRole()).isEqualTo("admin");
        }
    }

    @Test
    void logout_callsStpUtilLogout() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            authService.logout();

            stpUtilMock.verify(StpUtil::logout);
        }
    }

    private UserPO buildUser(Long id, String username, String password, String role) {
        UserPO user = new UserPO();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
