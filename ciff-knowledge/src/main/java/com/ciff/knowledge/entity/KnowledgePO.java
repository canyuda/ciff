package com.ciff.knowledge.entity;

import com.ciff.common.entity.SoftDeletableEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge")
public class KnowledgePO extends SoftDeletableEntity {

    private Long userId;

    private String name;

    private String description;

    /** fixed chunk length */
    private Integer chunkSize;

    /** embedding model name */
    private String embeddingModel;

    /** active / inactive */
    private String status;
}
