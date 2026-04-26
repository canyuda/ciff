package com.ciff.app.controller;

import com.ciff.app.dto.auth.LoginRequest;
import com.ciff.app.dto.auth.LoginVO;
import com.ciff.app.dto.auth.RegisterRequest;
import com.ciff.app.dto.auth.UserInfoVO;
import com.ciff.app.service.AuthService;
import com.ciff.app.service.GitHubOAuthService;
import com.ciff.common.dto.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GitHubOAuthService gitHubOAuthService;

    @Value("${ciff.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<UserInfoVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/github")
    public void githubRedirect(HttpServletResponse response) throws IOException {
        response.sendRedirect(gitHubOAuthService.getAuthorizeUrl());
    }

    // todo [未被前端使用:设计性保留]
    @GetMapping("/github/callback")
    public void githubCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        LoginVO loginVO = gitHubOAuthService.handleCallback(code);
        String redirectUrl = frontendUrl + "/login?token=" + loginVO.getToken()
                + "&username=" + loginVO.getUser().getUsername()
                + "&role=" + loginVO.getUser().getRole();
        response.sendRedirect(redirectUrl);
    }
}
