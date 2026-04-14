package com.ciff.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.app.convertor.DemoItemConvertor;
import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.app.entity.DemoItemPO;
import com.ciff.app.mapper.DemoItemMapper;
import com.ciff.app.service.DemoItemService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoItemServiceImpl implements DemoItemService {

    private final DemoItemMapper demoItemMapper;

    @Override
    public DemoItemVO create(DemoItemCreateRequest request) {
        DemoItemPO po = DemoItemConvertor.toPO(request);
        demoItemMapper.insert(po);
        return DemoItemConvertor.toVO(po);
    }

    @Override
    public DemoItemVO update(Long id, DemoItemUpdateRequest request) {
        DemoItemPO po = requireExists(id);
        DemoItemConvertor.updatePO(po, request);
        demoItemMapper.updateById(po);
        return DemoItemConvertor.toVO(po);
    }

    @Override
    public DemoItemVO getById(Long id) {
        return DemoItemConvertor.toVO(requireExists(id));
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        demoItemMapper.deleteById(id);
    }

    @Override
    public PageResult<DemoItemVO> page(Integer page, Integer pageSize) {
        Page<DemoItemPO> pageParam = PageHelper.toPage(page, pageSize);
        Page<DemoItemPO> result = demoItemMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DemoItemPO>().orderByDesc(DemoItemPO::getCreateTime));
        return PageHelper.toPageResult(result, DemoItemConvertor::toVO);
    }

    private DemoItemPO requireExists(Long id) {
        DemoItemPO po = demoItemMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "DemoItem not found: " + id);
        }
        return po;
    }
}
