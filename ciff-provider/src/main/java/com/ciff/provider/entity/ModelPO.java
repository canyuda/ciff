package com.ciff.provider.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_model")
public class ModelPO extends SoftDeletableEntity {

    private Long providerId;

    private String name;

    private String displayName;

    private Integer maxTokens;

    private String defaultParams;

    private String status;
}
