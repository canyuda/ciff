package com.ciff.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class UserPO extends SoftDeletableEntity {

    private String username;

    private String password;

    private String role;

    private Long githubId;
}
