package com.ciff.common.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SoftDeletableEntity extends BaseEntity {

    @TableLogic
    private Boolean deleted = false;
}