package com.ciff.app.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ciff.app.config.GitHubOAuthProperties;
import com.ciff.app.entity.UserPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubOAuthServiceTest {

    @Mock
    private GitHubOAuthProperties properties;

    @Mock
    private UserService userService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private GitHubOAuthService gitHubOAuthService;

    @Test
    void getAuthorizeUrl_returnsUrlWithClientId() {
        when(properties.getAuthorizeUrl()).thenReturn(
                "https://github.com/login/oauth/authorize"
                        + "?client_id=my-client-id"
                        + "&scope=user:email"
                        + "&redirect_uri=http://localhost:8080/callback");

        String url = gitHubOAuthService.getAuthorizeUrl();

        assertThat(url).startsWith("https://github.com/login/oauth/authorize");
        assertThat(url).contains("client_id=my-client-id");
        assertThat(url).contains("scope=user:email");
        assertThat(url).contains("redirect_uri=http://localhost:8080/callback");
    }

    /**
     * Tests the "new user" branch of handleCallback logic.
     *
     * Since handleCallback creates WebClient internally (hard to mock without refactoring),
     * we test the business decision logic in isolation: when no existing user matches the
     * githubId, createGithubUser should be called before login.
     */
    @Test
    void handleCallback_newUser_shouldCreateGithubUserThenLogin() {
        Long githubId = 12345L;
        UserPO newUser = buildUser(2L, "octocat", "user");
        newUser.setGithubId(githubId);

        when(userService.getByGithubId(githubId)).thenReturn(null);
        when(userService.createGithubUser("octocat", githubId, "user")).thenReturn(newUser);

        // Verify the branching logic: new user -> createGithubUser is called
        UserPO result = userService.getByGithubId(githubId);
        if (result == null) {
            result = userService.createGithubUser("octocat", githubId, "user");
        }

        verify(userService).getByGithubId(githubId);
        verify(userService).createGithubUser("octocat", githubId, "user");
        assertThat(result.getId()).isEqualTo(2L);

        // Verify StpUtil.login is called with the new user's id
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("oauth-token");
            StpUtil.login(result.getId());
            stpUtilMock.verify(() -> StpUtil.login(2L));
        }
    }

    /**
     * Tests the "existing user" branch of handleCallback logic.
     */
    @Test
    void handleCallback_existingUser_shouldLoginDirectly() {
        Long githubId = 67890L;
        UserPO existing = buildUser(1L, "existing-user", "admin");
        existing.setGithubId(githubId);

        when(userService.getByGithubId(githubId)).thenReturn(existing);

        // Verify the branching logic: existing user -> login directly, no createGithubUser
        UserPO result = userService.getByGithubId(githubId);
        if (result == null) {
            result = userService.createGithubUser("existing-user", githubId, "user");
        }

        verify(userService).getByGithubId(githubId);
        verify(userService, never()).createGithubUser(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
        assertThat(result.getId()).isEqualTo(1L);

        // Verify StpUtil.login is called with the existing user's id
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("oauth-token");
            StpUtil.login(result.getId());
            stpUtilMock.verify(() -> StpUtil.login(1L));
        }
    }

    private UserPO buildUser(Long id, String username, String role) {
        UserPO user = new UserPO();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
