package com.ciff.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.mcp.convertor.ToolConvertor;
import com.ciff.mcp.dto.ToolCreateRequest;
import com.ciff.mcp.dto.ToolUpdateRequest;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.entity.ToolPO;
import com.ciff.mcp.mapper.ToolMapper;
import com.ciff.mcp.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final ToolMapper toolMapper;

    private static final List<String> VALID_TYPES = List.of("api", "mcp");
    private static final List<String> VALID_STATUSES = List.of("enabled", "disabled");

    @Override
    public ToolVO create(ToolCreateRequest request) {
        validateType(request.getType());
        validateNameUnique(request.getName(), null);
        validateParamSchema(request.getParamSchema());

        ToolPO po = ToolConvertor.toPO(request);
        toolMapper.insert(po);
        return ToolConvertor.toVO(po);
    }

    @Override
    public ToolVO update(Long id, ToolUpdateRequest request) {
        ToolPO po = requireExists(id);

        if (request.getName() != null && !request.getName().equals(po.getName())) {
            validateNameUnique(request.getName(), id);
        }
        if (request.getType() != null) {
            validateType(request.getType());
        }
        if (request.getParamSchema() != null) {
            validateParamSchema(request.getParamSchema());
        }

        ToolConvertor.updatePO(po, request);
        toolMapper.updateById(po);
        return ToolConvertor.toVO(po);
    }

    @Override
    public ToolVO getById(Long id) {
        ToolPO po = requireExists(id);
        return ToolConvertor.toVO(po);
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        toolMapper.deleteById(id);
    }

    @Override
    public PageResult<ToolVO> page(Integer page, Integer pageSize, String type, String status) {
        Page<ToolPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ToolPO> wrapper = new LambdaQueryWrapper<ToolPO>()
                .eq(type != null && !type.isEmpty(), ToolPO::getType, type)
                .eq(status != null && !status.isEmpty(), ToolPO::getStatus, status)
                .orderByDesc(ToolPO::getCreateTime);

        Page<ToolPO> result = toolMapper.selectPage(pageParam, wrapper);
        List<ToolVO> records = result.getRecords().stream().map(ToolConvertor::toVO).toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    private void validateType(String type) {
        if (!VALID_TYPES.contains(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的工具类型: " + type + "，仅支持: " + VALID_TYPES);
        }
    }

    private void validateNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<ToolPO> wrapper = new LambdaQueryWrapper<ToolPO>()
                .eq(ToolPO::getName, name);
        if (excludeId != null) {
            wrapper.ne(ToolPO::getId, excludeId);
        }
        if (toolMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "工具名称已存在: " + name);
        }
    }

    private void validateParamSchema(Map<String, Object> schema) {
        if (schema == null) return;
        if (!"object".equals(schema.get("type"))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "paramSchema.type 必须为 object");
        }
        if (!(schema.get("properties") instanceof Map)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "paramSchema 必须包含 properties 对象");
        }
    }

    private ToolPO requireExists(Long id) {
        ToolPO po = toolMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工具不存在: " + id);
        }
        return po;
    }
}
