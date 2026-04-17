package com.ciff.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthType {

    BEARER("bearer"),
    API_KEY_HEADER("api_key_header"),
    URL("url"),
    JWT("jwt"),
    DUAL_KEY("dual_key"),
    NONE("none");
    @JsonValue
    @EnumValue
    private final String type;
}
