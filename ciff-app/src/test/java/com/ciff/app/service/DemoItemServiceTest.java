package com.ciff.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.app.entity.DemoItemPO;
import com.ciff.app.mapper.DemoItemMapper;
import com.ciff.app.service.impl.DemoItemServiceImpl;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DemoItemServiceTest {

    @Mock
    private DemoItemMapper demoItemMapper;

    @InjectMocks
    private DemoItemServiceImpl demoItemService;

    @Test
    void create_whenInputValid_shouldReturnVO() {
        // Given
        DemoItemCreateRequest request = new DemoItemCreateRequest();
        request.setName("test");
        request.setStatus(0);

        given(demoItemMapper.insert(any(DemoItemPO.class))).willReturn(1);

        // When
        DemoItemVO vo = demoItemService.create(request);

        // Then
        assertThat(vo.getName()).isEqualTo("test");
        assertThat(vo.getStatus()).isEqualTo(0);
        verify(demoItemMapper).insert(any(DemoItemPO.class));
    }

    @Test
    void update_whenExists_shouldReturnUpdatedVO() {
        // Given
        DemoItemPO po = buildPO(1L, "old", 0);
        given(demoItemMapper.selectById(1L)).willReturn(po);
        given(demoItemMapper.updateById(any(DemoItemPO.class))).willReturn(1);

        DemoItemUpdateRequest request = new DemoItemUpdateRequest();
        request.setName("new");

        // When
        DemoItemVO vo = demoItemService.update(1L, request);

        // Then
        assertThat(vo.getName()).isEqualTo("new");
    }

    @Test
    void update_whenNotExists_shouldThrowBizException() {
        // Given
        given(demoItemMapper.selectById(999L)).willReturn(null);
        DemoItemUpdateRequest request = new DemoItemUpdateRequest();

        // When & Then
        assertThatThrownBy(() -> demoItemService.update(999L, request))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void getById_whenExists_shouldReturnVO() {
        // Given
        DemoItemPO po = buildPO(1L, "test", 0);
        given(demoItemMapper.selectById(1L)).willReturn(po);

        // When
        DemoItemVO vo = demoItemService.getById(1L);

        // Then
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("test");
    }

    @Test
    void getById_whenNotExists_shouldThrowBizException() {
        // Given
        given(demoItemMapper.selectById(999L)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> demoItemService.getById(999L))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void delete_whenExists_shouldSuccess() {
        // Given
        given(demoItemMapper.selectById(1L)).willReturn(buildPO(1L, "test", 0));
        given(demoItemMapper.deleteById(1L)).willReturn(1);

        // When
        demoItemService.delete(1L);

        // Then
        verify(demoItemMapper).deleteById(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrowBizException() {
        // Given
        given(demoItemMapper.selectById(999L)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> demoItemService.delete(999L))
                .isInstanceOf(BizException.class);
    }

    @Test
    void page_shouldReturnPageResult() {
        // Given
        DemoItemPO po = buildPO(1L, "test", 0);
        Page<DemoItemPO> page = new Page<>(1, 20);
        page.setRecords(java.util.List.of(po));
        page.setTotal(1);

        given(demoItemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).willReturn(page);

        // When
        PageResult<DemoItemVO> result = demoItemService.page(1, 20);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getName()).isEqualTo("test");
    }

    private DemoItemPO buildPO(Long id, String name, Integer status) {
        DemoItemPO po = new DemoItemPO();
        po.setId(id);
        po.setName(name);
        po.setStatus(status);
        po.setCreateTime(LocalDateTime.of(2026, 4, 14, 12, 0, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 4, 14, 12, 0, 0));
        return po;
    }
}
