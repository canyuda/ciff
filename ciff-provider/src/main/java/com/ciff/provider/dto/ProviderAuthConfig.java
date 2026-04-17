package com.ciff.provider.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProviderAuthConfig {

    /**
     * API version for authentication.
     * Used by: Claude (e.g., "2023-06-01"), Azure OpenAI (e.g., "2024-02-15-preview").
     */
    private String apiVersion;

    /**
     * Token refresh interval in seconds.
     * Used by: Gemini (service account token), Wenxin (access token).
     */
    private Integer tokenTtl;

    /**
     * Provider-specific extra auth parameters.
     * Examples: Azure OpenAI {"deployment_name": "...", "api_key_header": "ocp-apim-subscription-key"},
     *           Spark {"appid": "...", "api_secret": "..."}.
     */
    private Map<String, Object> extra;
}
