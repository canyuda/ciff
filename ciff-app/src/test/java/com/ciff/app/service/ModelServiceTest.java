package com.ciff.app.service;

import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.provider.dto.ModelCreateRequest;
import com.ciff.provider.dto.ModelUpdateRequest;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import com.ciff.provider.service.impl.ModelServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ProviderMapper providerMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ModelServiceImpl modelService;

    private void setupCacheMock() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("provider-cache")).thenReturn(cache);
    }

    @Test
    void create_whenProviderExists_shouldSave() {
        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        provider.setName("OpenAI");
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(modelMapper.insert(any(ModelPO.class))).thenReturn(1);
        setupCacheMock();

        ModelCreateRequest request = new ModelCreateRequest();
        request.setProviderId(1L);
        request.setName("gpt-4o");

        // When
        ModelVO vo = modelService.create(request);

        // Then
        assertNotNull(vo);
        assertEquals("gpt-4o", vo.getName());
        assertEquals("OpenAI", vo.getProviderName());
        verify(modelMapper).insert(any(ModelPO.class));
    }

    @Test
    void create_whenProviderNotExists_shouldThrow() {
        when(providerMapper.selectById(999L)).thenReturn(null);

        ModelCreateRequest request = new ModelCreateRequest();
        request.setProviderId(999L);
        request.setName("test");

        assertThrows(BizException.class, () -> modelService.create(request));
        verify(modelMapper, never()).insert(any(ModelPO.class));
    }

    @Test
    void create_withValidDefaultParams_shouldSave() throws Exception {
        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(modelMapper.insert(any(ModelPO.class))).thenReturn(1);
        setupCacheMock();

        ModelCreateRequest request = new ModelCreateRequest();
        request.setProviderId(1L);
        request.setName("gpt-4o");
        request.setDefaultParams("{\"temperature\":0.7}");

        modelService.create(request);

        verify(modelMapper).insert(any(ModelPO.class));
    }

    @Test
    void create_withInvalidDefaultParams_shouldThrow() throws Exception {
        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(objectMapper.readTree(any(String.class))).thenThrow(
                new com.fasterxml.jackson.core.JsonParseException("Invalid JSON"));

        ModelCreateRequest request = new ModelCreateRequest();
        request.setProviderId(1L);
        request.setName("gpt-4o");
        request.setDefaultParams("not-json");

        assertThatThrownBy(() -> modelService.create(request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
                });

        verify(modelMapper, never()).insert(any(ModelPO.class));
    }

    @Test
    void update_whenProviderIdChanged_shouldValidateNewProvider() {
        // Given
        ModelPO existing = new ModelPO();
        existing.setId(1L);
        existing.setProviderId(1L);
        existing.setName("gpt-4o");
        when(modelMapper.selectById(1L)).thenReturn(existing);

        ProviderPO newProvider = new ProviderPO();
        newProvider.setId(2L);
        newProvider.setName("Claude");
        when(providerMapper.selectById(2L)).thenReturn(newProvider);
        when(modelMapper.updateById(any(ModelPO.class))).thenReturn(1);
        setupCacheMock();

        ModelUpdateRequest request = new ModelUpdateRequest();
        request.setProviderId(2L);

        // When
        ModelVO vo = modelService.update(1L, request);

        // Then
        assertEquals("Claude", vo.getProviderName());
        verify(modelMapper).updateById(any(ModelPO.class));
    }

    @Test
    void update_withInvalidDefaultParams_shouldThrow() throws Exception {
        ModelPO existing = new ModelPO();
        existing.setId(1L);
        existing.setProviderId(1L);
        when(modelMapper.selectById(1L)).thenReturn(existing);
        when(objectMapper.readTree(any(String.class))).thenThrow(
                new com.fasterxml.jackson.core.JsonParseException("Invalid JSON"));

        ModelUpdateRequest request = new ModelUpdateRequest();
        request.setDefaultParams("bad-json");

        assertThatThrownBy(() -> modelService.update(1L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("defaultParams 不是有效的 JSON");

        verify(modelMapper, never()).updateById(any(ModelPO.class));
    }

    @Test
    void delete_shouldCallDeleteById() {
        ModelPO existing = new ModelPO();
        existing.setId(1L);
        existing.setProviderId(1L);
        when(modelMapper.selectById(1L)).thenReturn(existing);
        when(modelMapper.deleteById(1L)).thenReturn(1);
        setupCacheMock();

        modelService.delete(1L);

        verify(modelMapper).deleteById(1L);
    }

    @Test
    void getById_whenExists_shouldReturnWithProviderName() {
        ModelPO po = new ModelPO();
        po.setId(1L);
        po.setProviderId(1L);
        po.setName("gpt-4o");
        when(modelMapper.selectById(1L)).thenReturn(po);

        ProviderPO provider = new ProviderPO();
        provider.setId(1L);
        provider.setName("OpenAI");
        when(providerMapper.selectById(1L)).thenReturn(provider);

        ModelVO vo = modelService.getById(1L);

        assertEquals("gpt-4o", vo.getName());
        assertEquals("OpenAI", vo.getProviderName());
    }

    @Test
    void getById_whenNotExists_shouldThrow() {
        when(modelMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> modelService.getById(999L));
    }
}
