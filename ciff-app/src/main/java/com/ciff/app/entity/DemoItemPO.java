package com.ciff.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_demo_item")
public class DemoItemPO extends BaseEntity {

    private String name;

    private Integer status;
}
