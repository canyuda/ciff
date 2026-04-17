package com.ciff.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.HealthStatus;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.ApiKeyEncryptor;
import com.ciff.provider.dto.ProviderCreateRequest;
import com.ciff.provider.dto.ProviderUpdateRequest;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderHealthPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderHealthMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.impl.ProviderDetailCacheHelper;
import com.ciff.provider.service.impl.ProviderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderMapper providerMapper;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ProviderHealthMapper healthMapper;

    @Mock
    private ApiKeyEncryptor apiKeyEncryptor;

    @Mock
    private ProviderDetailCacheHelper detailCacheHelper;

    @InjectMocks
    private ProviderServiceImpl providerService;

    @Test
    void page_whenHasRecords_shouldReturnWithModelsAndHealth() {
        // Given
        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        provider.setName("OpenAI");
        provider.setType(ProviderType.OPENAI);
        provider.setStatus(ProviderStatus.ACTIVE);

        Page<ProviderPO> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(provider));
        pageResult.setTotal(1L);

        when(providerMapper.selectPage(any(), any())).thenReturn(pageResult);

        ModelPO model = new ModelPO();
        model.setId(10L);
        model.setProviderId(1L);
        model.setName("gpt-4o");
        when(modelMapper.selectList(any())).thenReturn(List.of(model));

        ProviderHealthPO health = new ProviderHealthPO();
        health.setId(100L);
        health.setProviderId(1L);
        health.setStatus(HealthStatus.UP);
        health.setConsecutiveFailures(0);
        when(healthMapper.selectList(any())).thenReturn(List.of(health));

        // When
        PageResult<ProviderVO> result = providerService.page(1, 20, null, null);

        // Then
        assertThat(result.getList()).hasSize(1);
        ProviderVO vo = result.getList().get(0);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("OpenAI");
        assertThat(vo.getModels()).hasSize(1);
        assertThat(vo.getModels().get(0).getName()).isEqualTo("gpt-4o");
        assertThat(vo.getHealth()).isNotNull();
        assertThat(vo.getHealth().getStatus()).isEqualTo("UP");
        assertThat(vo.getHealth().getConsecutiveFailures()).isZero();
    }

    @Test
    void page_whenNoRecords_shouldReturnEmptyList() {
        Page<ProviderPO> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);

        when(providerMapper.selectPage(any(), any())).thenReturn(emptyPage);

        PageResult<ProviderVO> result = providerService.page(1, 20, null, null);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void create_whenValid_shouldSaveAndReturnVo() {
        when(providerMapper.selectCount(any())).thenReturn(0L);
        when(providerMapper.insert(any(ProviderPO.class))).thenAnswer(invocation -> {
            ProviderPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        });
        when(apiKeyEncryptor.encrypt("sk-test")).thenReturn("encrypted");

        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setName("OpenAI");
        request.setType(ProviderType.OPENAI);
        request.setAuthType(AuthType.BEARER);
        request.setApiBaseUrl("https://api.openai.com/v1");
        request.setApiKey("sk-test");

        ProviderVO vo = providerService.create(request);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("OpenAI");
        assertThat(vo.getType()).isEqualTo("openai");
    }

    @Test
    void create_whenNameDuplicate_shouldThrowBizException() {
        when(providerMapper.selectCount(any())).thenReturn(1L);

        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setName("OpenAI");
        request.setType(ProviderType.OPENAI);
        request.setAuthType(AuthType.BEARER);
        request.setApiBaseUrl("https://api.openai.com/v1");

        assertThatThrownBy(() -> providerService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
                    assertThat(biz.getMessage()).contains("供应商名称已存在");
                });

        verify(providerMapper, never()).insert(any(ProviderPO.class));
    }

    @Test
    void update_whenValid_shouldUpdateAndReturnVo() {
        ProviderPO existing = new ProviderPO();
        existing.setId(1L);
        existing.setName("OpenAI");
        existing.setType(ProviderType.OPENAI);
        existing.setAuthType(AuthType.BEARER);
        existing.setApiBaseUrl("https://api.openai.com/v1");

        when(providerMapper.selectById(1L)).thenReturn(existing);
        when(providerMapper.selectCount(any())).thenReturn(0L);
        when(providerMapper.updateById(any(ProviderPO.class))).thenReturn(1);

        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("OpenAI Updated");
        request.setApiBaseUrl("https://api.openai.com/v2");

        ProviderVO vo = providerService.update(1L, request);

        assertThat(vo.getName()).isEqualTo("OpenAI Updated");
        assertThat(vo.getApiBaseUrl()).isEqualTo("https://api.openai.com/v2");
    }

    @Test
    void update_whenNotFound_shouldThrowBizException() {
        when(providerMapper.selectById(999L)).thenReturn(null);

        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> providerService.update(999L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
                });
    }

    @Test
    void update_whenNameDuplicate_shouldThrowBizException() {
        ProviderPO existing = new ProviderPO();
        existing.setId(1L);
        existing.setName("OpenAI");

        when(providerMapper.selectById(1L)).thenReturn(existing);
        when(providerMapper.selectCount(any())).thenReturn(1L);

        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("Claude");

        assertThatThrownBy(() -> providerService.update(1L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("供应商名称已存在");
    }

    @Test
    void getById_whenExists_shouldReturnVoWithHealth() {
        ProviderVO cached = new ProviderVO();
        cached.setId(1L);
        cached.setName("OpenAI");

        when(detailCacheHelper.getDetail(1L)).thenReturn(cached);
        when(healthMapper.selectOne(any())).thenReturn(null);

        ProviderVO vo = providerService.getById(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("OpenAI");
        assertThat(vo.getHealth()).isNotNull();
        assertThat(vo.getHealth().getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void getById_whenNotFound_shouldThrowBizException() {
        when(detailCacheHelper.getDetail(999L)).thenThrow(
                new BizException(ErrorCode.NOT_FOUND, "供应商不存在"));

        assertThatThrownBy(() -> providerService.getById(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("供应商不存在");
    }

    @Test
    void delete_whenNoModels_shouldDelete() {
        ProviderPO existing = new ProviderPO();
        existing.setId(1L);
        when(providerMapper.selectById(1L)).thenReturn(existing);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(providerMapper.deleteById(1L)).thenReturn(1);

        providerService.delete(1L);

        verify(providerMapper).deleteById(1L);
    }

    @Test
    void delete_whenHasModels_shouldThrowBizException() {
        ProviderPO existing = new ProviderPO();
        existing.setId(1L);
        when(providerMapper.selectById(1L)).thenReturn(existing);
        when(modelMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> providerService.delete(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("存在关联模型，无法删除");

        verify(providerMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void delete_whenNotFound_shouldThrowBizException() {
        when(providerMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> providerService.delete(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("供应商不存在");
    }

}
