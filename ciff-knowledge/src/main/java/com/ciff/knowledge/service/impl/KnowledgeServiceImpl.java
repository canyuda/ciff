package com.ciff.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.knowledge.convertor.KnowledgeConvertor;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;
    private final DocumentMapper documentMapper;

    private static final int MIN_CHUNK_SIZE = 128;
    private static final int MAX_CHUNK_SIZE = 2048;
    private static final int DEFAULT_CHUNK_SIZE = 700;
    private static final List<String> VALID_STATUSES = List.of("active", "inactive");
    private static final List<String> VALID_EMBEDDING_MODELS = List.of("text-embedding-v3");

    @Override
    public KnowledgeVO create(KnowledgeCreateRequest request, Long userId) {
        validateChunkSize(request.getChunkSize());
        validateEmbeddingModel(request.getEmbeddingModel());
        validateNameUnique(request.getName(), null, userId);

        KnowledgePO po = KnowledgeConvertor.toPO(request, userId);
        knowledgeMapper.insert(po);
        return KnowledgeConvertor.toVO(po);
    }

    @Override
    public KnowledgeVO update(Long id, KnowledgeUpdateRequest request, Long userId) {
        KnowledgePO po = requireExists(id, userId);

        if (request.getName() != null && !request.getName().equals(po.getName())) {
            validateNameUnique(request.getName(), id, userId);
        }
        if (request.getChunkSize() != null) {
            validateChunkSize(request.getChunkSize());
        }
        if (request.getEmbeddingModel() != null) {
            validateEmbeddingModel(request.getEmbeddingModel());
        }
        if (request.getStatus() != null && !VALID_STATUSES.contains(request.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "不支持的状态: " + request.getStatus());
        }

        KnowledgeConvertor.updatePO(po, request);
        knowledgeMapper.updateById(po);
        return KnowledgeConvertor.toVO(po);
    }

    @Override
    public KnowledgeVO getById(Long id, Long userId) {
        KnowledgePO po = requireExists(id, userId);
        return KnowledgeConvertor.toVO(po);
    }

    @Override
    public void delete(Long id, Long userId) {
        requireExists(id, userId);
        knowledgeMapper.deleteById(id);
    }

    @Override
    public PageResult<KnowledgeVO> page(Integer page, Integer pageSize, String status, Long userId) {
        Page<KnowledgePO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<KnowledgePO> wrapper = new LambdaQueryWrapper<KnowledgePO>()
                .eq(userId != null, KnowledgePO::getUserId, userId)
                .eq(status != null && !status.isEmpty(), KnowledgePO::getStatus, status)
                .orderByDesc(KnowledgePO::getCreateTime);

        Page<KnowledgePO> result = knowledgeMapper.selectPage(pageParam, wrapper);
        List<KnowledgeVO> records = result.getRecords().stream()
                .map(KnowledgeConvertor::toVO)
                .toList();
        records.forEach(vo -> {
            long count = documentMapper.selectCount(
                    new LambdaQueryWrapper<DocumentPO>()
                            .eq(DocumentPO::getKnowledgeId, vo.getId()));
            vo.setDocumentCount((int) count);
        });
        return PageResult.of(records, result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }

    private void validateChunkSize(Integer chunkSize) {
        int size = chunkSize != null ? chunkSize : DEFAULT_CHUNK_SIZE;
        if (size < MIN_CHUNK_SIZE || size > MAX_CHUNK_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "chunk_size 必须在 " + MIN_CHUNK_SIZE + "-" + MAX_CHUNK_SIZE + " 之间，当前: " + size);
        }
    }

    private void validateEmbeddingModel(String modelName) {
        if (!VALID_EMBEDDING_MODELS.contains(modelName)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "V1 仅支持 embedding 模型: " + VALID_EMBEDDING_MODELS);
        }
    }

    private void validateNameUnique(String name, Long excludeId, Long userId) {
        LambdaQueryWrapper<KnowledgePO> wrapper = new LambdaQueryWrapper<KnowledgePO>()
                .eq(KnowledgePO::getName, name)
                .eq(KnowledgePO::getUserId, userId);
        if (excludeId != null) {
            wrapper.ne(KnowledgePO::getId, excludeId);
        }
        if (knowledgeMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "知识库名称已存在: " + name);
        }
    }

    private KnowledgePO requireExists(Long id, Long userId) {
        KnowledgePO po = knowledgeMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + id);
        }
        if (userId != null && !userId.equals(po.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + id);
        }
        return po;
    }
}
