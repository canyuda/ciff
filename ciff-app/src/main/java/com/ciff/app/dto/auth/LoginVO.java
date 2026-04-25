package com.ciff.app.dto.auth;

import lombok.Data;

@Data
public class LoginVO {

    private String token;
    private UserInfoVO user;
}
