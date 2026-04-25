package com.ciff.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri = "http://localhost:8080/api/auth/github/callback";

    public String getAuthorizeUrl() {
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&scope=user:email"
                + "&redirect_uri=" + redirectUri;
    }
}
