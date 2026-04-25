package com.ciff.app.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ciff.app.config.GitHubOAuthProperties;
import com.ciff.app.dto.auth.LoginVO;
import com.ciff.app.dto.auth.UserInfoVO;
import com.ciff.app.entity.UserPO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private final GitHubOAuthProperties properties;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public String getAuthorizeUrl() {
        return properties.getAuthorizeUrl();
    }

    public LoginVO handleCallback(String code) {
        String accessToken = exchangeToken(code);
        JsonNode userInfo = fetchUserInfo(accessToken);
        Long githubId = userInfo.get("id").asLong();
        String username = userInfo.get("login").asText();

        UserPO user = userService.getByGithubId(githubId);
        if (user == null) {
            user = userService.createGithubUser(username, githubId, "user");
        }

        StpUtil.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(toUserInfoVO(user));
        return vo;
    }

    private String exchangeToken(String code) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://github.com")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String response = webClient.post()
                .uri("/login/oauth/access_token")
                .bodyValue(java.util.Map.of(
                        "client_id", properties.getClientId(),
                        "client_secret", properties.getClientSecret(),
                        "code", code
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.get("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to exchange GitHub token: {}", response, e);
            throw new RuntimeException("GitHub OAuth token exchange failed");
        }
    }

    private JsonNode fetchUserInfo(String accessToken) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String response = webClient.get()
                .uri("/user")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info: {}", response, e);
            throw new RuntimeException("Failed to fetch GitHub user info");
        }
    }

    private UserInfoVO toUserInfoVO(UserPO user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        return vo;
    }
}
