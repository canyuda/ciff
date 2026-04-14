package com.ciff.app.service;

import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.common.dto.PageResult;

public interface DemoItemService {

    DemoItemVO create(DemoItemCreateRequest request);

    DemoItemVO update(Long id, DemoItemUpdateRequest request);

    DemoItemVO getById(Long id);

    void delete(Long id);

    PageResult<DemoItemVO> page(Integer page, Integer pageSize);
}
