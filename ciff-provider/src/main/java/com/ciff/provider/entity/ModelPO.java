package com.ciff.provider.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.SoftDeletableEntity;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.provider.dto.ModelDefaultParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_model", autoResultMap = true)
public class ModelPO extends SoftDeletableEntity {

    private Long providerId;

    private String name;

    private String displayName;

    private Integer maxTokens;

    @TableField(value = "default_params", typeHandler = JacksonTypeHandler.class)
    private ModelDefaultParam defaultParams;

    private ProviderStatus status;
}
