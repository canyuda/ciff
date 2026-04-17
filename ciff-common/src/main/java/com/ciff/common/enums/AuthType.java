package com.ciff.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthType {

    BEARER("bearer"),
    API_KEY_HEADER("api_key_header"),
    JWT("jwt"),
    DUAL_KEY("dual_key");

    private final String type;
}
