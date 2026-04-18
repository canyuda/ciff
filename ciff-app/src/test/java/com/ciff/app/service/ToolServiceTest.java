package com.ciff.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.mcp.dto.ToolCreateRequest;
import com.ciff.mcp.dto.ToolUpdateRequest;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.entity.ToolPO;
import com.ciff.mcp.mapper.ToolMapper;
import com.ciff.mcp.service.impl.ToolServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolMapper toolMapper;

    @InjectMocks
    private ToolServiceImpl toolService;

    @Test
    void create_whenValid_shouldSaveAndReturnVo() {
        when(toolMapper.selectCount(any())).thenReturn(0L);
        when(toolMapper.insert(any(ToolPO.class))).thenAnswer(invocation -> {
            ToolPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        });

        ToolCreateRequest request = new ToolCreateRequest();
        request.setName("weather-api");
        request.setType("api");
        request.setEndpoint("https://api.weather.com/v1");
        request.setDescription("weather lookup");

        ToolVO vo = toolService.create(request);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("weather-api");
        assertThat(vo.getType()).isEqualTo("api");
        assertThat(vo.getStatus()).isEqualTo("enabled");
    }

    @Test
    void create_whenNameDuplicate_shouldThrowBizException() {
        when(toolMapper.selectCount(any())).thenReturn(1L);

        ToolCreateRequest request = new ToolCreateRequest();
        request.setName("weather-api");
        request.setType("api");
        request.setEndpoint("https://api.weather.com/v1");

        assertThatThrownBy(() -> toolService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
                    assertThat(biz.getMessage()).contains("工具名称已存在");
                });

        verify(toolMapper, never()).insert(any(ToolPO.class));
    }

    @Test
    void create_whenInvalidType_shouldThrowBizException() {
        ToolCreateRequest request = new ToolCreateRequest();
        request.setName("test");
        request.setType("invalid");
        request.setEndpoint("https://example.com");

        assertThatThrownBy(() -> toolService.create(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的工具类型");
    }

    @Test
    void create_withMcpType_shouldSucceed() {
        when(toolMapper.selectCount(any())).thenReturn(0L);
        when(toolMapper.insert(any(ToolPO.class))).thenAnswer(invocation -> {
            ToolPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        });

        ToolCreateRequest request = new ToolCreateRequest();
        request.setName("mcp-tool");
        request.setType("mcp");
        request.setEndpoint("http://localhost:3000/sse");

        ToolVO vo = toolService.create(request);

        assertThat(vo.getType()).isEqualTo("mcp");
    }

    @Test
    void create_withParamSchema_shouldSaveCorrectly() {
        when(toolMapper.selectCount(any())).thenReturn(0L);
        when(toolMapper.insert(any(ToolPO.class))).thenAnswer(invocation -> {
            ToolPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        });

        ToolCreateRequest request = new ToolCreateRequest();
        request.setName("calc");
        request.setType("api");
        request.setEndpoint("https://calc.example.com");
        request.setParamSchema(Map.of("type", "object", "properties", Map.of("expr", Map.of("type", "string"))));

        ToolVO vo = toolService.create(request);

        assertThat(vo.getParamSchema()).isNotNull();
        assertThat(vo.getParamSchema()).containsEntry("type", "object");
    }

    @Test
    void update_whenValid_shouldUpdateAndReturnVo() {
        ToolPO existing = new ToolPO();
        existing.setId(1L);
        existing.setName("weather-api");
        existing.setType("api");
        existing.setEndpoint("https://api.weather.com/v1");
        existing.setStatus("enabled");

        when(toolMapper.selectById(1L)).thenReturn(existing);
        when(toolMapper.updateById(any(ToolPO.class))).thenReturn(1);

        ToolUpdateRequest request = new ToolUpdateRequest();
        request.setName("weather-api-v2");
        request.setEndpoint("https://api.weather.com/v2");

        ToolVO vo = toolService.update(1L, request);

        assertThat(vo.getName()).isEqualTo("weather-api-v2");
        assertThat(vo.getEndpoint()).isEqualTo("https://api.weather.com/v2");
    }

    @Test
    void update_whenNotFound_shouldThrowBizException() {
        when(toolMapper.selectById(999L)).thenReturn(null);

        ToolUpdateRequest request = new ToolUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> toolService.update(999L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
                });
    }

    @Test
    void update_whenNameDuplicate_shouldThrowBizException() {
        ToolPO existing = new ToolPO();
        existing.setId(1L);
        existing.setName("weather-api");

        when(toolMapper.selectById(1L)).thenReturn(existing);
        when(toolMapper.selectCount(any())).thenReturn(1L);

        ToolUpdateRequest request = new ToolUpdateRequest();
        request.setName("other-tool");

        assertThatThrownBy(() -> toolService.update(1L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具名称已存在");
    }

    @Test
    void getById_whenExists_shouldReturnVo() {
        ToolPO po = new ToolPO();
        po.setId(1L);
        po.setName("weather-api");
        po.setType("api");
        po.setEndpoint("https://api.weather.com/v1");
        po.setStatus("enabled");

        when(toolMapper.selectById(1L)).thenReturn(po);

        ToolVO vo = toolService.getById(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("weather-api");
    }

    @Test
    void getById_whenNotFound_shouldThrowBizException() {
        when(toolMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> toolService.getById(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具不存在");
    }

    @Test
    void delete_whenExists_shouldDelete() {
        ToolPO existing = new ToolPO();
        existing.setId(1L);
        when(toolMapper.selectById(1L)).thenReturn(existing);
        when(toolMapper.deleteById(1L)).thenReturn(1);

        toolService.delete(1L);

        verify(toolMapper).deleteById(1L);
    }

    @Test
    void delete_whenNotFound_shouldThrowBizException() {
        when(toolMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> toolService.delete(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具不存在");
    }

    @Test
    void page_whenHasRecords_shouldReturnPage() {
        ToolPO tool = new ToolPO();
        tool.setId(1L);
        tool.setName("weather-api");
        tool.setType("api");
        tool.setStatus("enabled");

        Page<ToolPO> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(tool));
        pageResult.setTotal(1L);

        when(toolMapper.selectPage(any(), any())).thenReturn(pageResult);

        PageResult<ToolVO> result = toolService.page(1, 20, "api", "enabled");

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getName()).isEqualTo("weather-api");
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void page_whenNoRecords_shouldReturnEmpty() {
        Page<ToolPO> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);

        when(toolMapper.selectPage(any(), any())).thenReturn(emptyPage);

        PageResult<ToolVO> result = toolService.page(1, 20, null, null);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }
}
