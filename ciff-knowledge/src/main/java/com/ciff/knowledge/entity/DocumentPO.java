package com.ciff.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_document")
public class DocumentPO extends SoftDeletableEntity {

    private Long knowledgeId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private Integer chunkCount;

    /** uploading / processing / ready / failed */
    private String status;
}
