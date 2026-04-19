package com.ciff.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.exception.BizException;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.impl.KnowledgeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;

    @InjectMocks
    private KnowledgeServiceImpl knowledgeService;

    @Test
    void create_whenValid_shouldInsert() {
        KnowledgeCreateRequest request = new KnowledgeCreateRequest();
        request.setName("test-kb");
        request.setEmbeddingModel("text-embedding-v3");

        when(knowledgeMapper.selectCount(any())).thenReturn(0L);
        doReturn(1).when(knowledgeMapper).insert(any(KnowledgePO.class));

        KnowledgeVO vo = knowledgeService.create(request, 1L);

        assertThat(vo.getName()).isEqualTo("test-kb");
        assertThat(vo.getStatus()).isEqualTo("active");
    }

    @Test
    void create_withInvalidChunkSize_shouldThrow() {
        KnowledgeCreateRequest request = new KnowledgeCreateRequest();
        request.setName("test");
        request.setEmbeddingModel("text-embedding-v3");
        request.setChunkSize(50);

        assertThatThrownBy(() -> knowledgeService.create(request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("chunk_size");

        verify(knowledgeMapper, never()).insert(any(KnowledgePO.class));
    }

    @Test
    void create_withInvalidEmbeddingModel_shouldThrow() {
        KnowledgeCreateRequest request = new KnowledgeCreateRequest();
        request.setName("test");
        request.setEmbeddingModel("invalid-model");

        assertThatThrownBy(() -> knowledgeService.create(request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("embedding 模型");

        verify(knowledgeMapper, never()).insert(any(KnowledgePO.class));
    }

    @Test
    void create_whenNameDuplicate_shouldThrow() {
        KnowledgeCreateRequest request = new KnowledgeCreateRequest();
        request.setName("dup");
        request.setEmbeddingModel("text-embedding-v3");

        when(knowledgeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> knowledgeService.create(request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("名称已存在");
    }

    @Test
    void update_whenValid_shouldUpdate() {
        KnowledgePO existing = knowledgePO(1L, "old-name", "text-embedding-v3");
        when(knowledgeMapper.selectById(1L)).thenReturn(existing);

        KnowledgeUpdateRequest request = new KnowledgeUpdateRequest();
        request.setName("new-name");

        when(knowledgeMapper.selectCount(any())).thenReturn(0L);
        when(knowledgeMapper.updateById(any(KnowledgePO.class))).thenReturn(1);

        KnowledgeVO vo = knowledgeService.update(1L, request, 1L);

        assertThat(vo.getName()).isEqualTo("new-name");
    }

    @Test
    void update_withInvalidStatus_shouldThrow() {
        KnowledgePO existing = knowledgePO(1L, "name", "text-embedding-v3");
        when(knowledgeMapper.selectById(1L)).thenReturn(existing);

        KnowledgeUpdateRequest request = new KnowledgeUpdateRequest();
        request.setStatus("invalid");

        assertThatThrownBy(() -> knowledgeService.update(1L, request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的状态");
    }

    @Test
    void update_whenNotFound_shouldThrow() {
        when(knowledgeMapper.selectById(999L)).thenReturn(null);

        KnowledgeUpdateRequest request = new KnowledgeUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> knowledgeService.update(999L, request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void getById_whenFound_shouldReturnVO() {
        KnowledgePO po = knowledgePO(1L, "kb", "text-embedding-v3");
        when(knowledgeMapper.selectById(1L)).thenReturn(po);

        KnowledgeVO vo = knowledgeService.getById(1L, 1L);

        assertThat(vo.getName()).isEqualTo("kb");
    }

    @Test
    void getById_whenNotOwner_shouldThrow() {
        KnowledgePO po = knowledgePO(1L, "kb", "text-embedding-v3");
        po.setUserId(2L);
        when(knowledgeMapper.selectById(1L)).thenReturn(po);

        assertThatThrownBy(() -> knowledgeService.getById(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void delete_whenValid_shouldDelete() {
        KnowledgePO po = knowledgePO(1L, "kb", "text-embedding-v3");
        when(knowledgeMapper.selectById(1L)).thenReturn(po);

        knowledgeService.delete(1L, 1L);

        verify(knowledgeMapper).deleteById(1L);
    }

    @Test
    void page_shouldReturnPageResult() {
        KnowledgePO po = knowledgePO(1L, "kb1", "text-embedding-v3");
        Page<KnowledgePO> page = new Page<>(1, 10);
        page.setRecords(List.of(po));
        page.setTotal(1);

        when(knowledgeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        var result = knowledgeService.page(1, 10, null, 1L);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    // --- Helper ---

    private static KnowledgePO knowledgePO(Long id, String name, String embeddingModel) {
        KnowledgePO po = new KnowledgePO();
        po.setId(id);
        po.setUserId(1L);
        po.setName(name);
        po.setDescription("");
        po.setChunkSize(700);
        po.setEmbeddingModel(embeddingModel);
        po.setStatus("active");
        return po;
    }
}
