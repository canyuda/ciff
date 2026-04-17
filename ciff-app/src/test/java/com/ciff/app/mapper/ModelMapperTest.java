package com.ciff.app.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import com.ciff.provider.entity.ModelPO;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ModelMapper;
import com.ciff.provider.mapper.ProviderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model Mapper 集成测试，连接真实数据库。
 * 需要本地 MySQL 环境可用。
 */
@SpringBootTest
@Transactional
class ModelMapperTest {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProviderMapper providerMapper;

    @Test
    void insert_and_selectById() {
        Long providerId = insertProvider("test-model-provider");

        ModelPO po = buildModel(providerId, "gpt-4o", "GPT-4o");
        modelMapper.insert(po);

        assertNotNull(po.getId());

        ModelPO found = modelMapper.selectById(po.getId());
        assertNotNull(found);
        assertEquals("gpt-4o", found.getName());
        assertEquals(providerId, found.getProviderId());
    }

    @Test
    void updateById() {
        Long providerId = insertProvider("test-model-update");
        ModelPO po = buildModel(providerId, "gpt-3.5", "GPT-3.5");
        modelMapper.insert(po);

        po.setDisplayName("GPT-3.5 Turbo");
        po.setMaxTokens(16384);
        modelMapper.updateById(po);

        ModelPO found = modelMapper.selectById(po.getId());
        assertEquals("GPT-3.5 Turbo", found.getDisplayName());
        assertEquals(16384, found.getMaxTokens());
    }

    @Test
    void deleteById_logicalDelete() {
        Long providerId = insertProvider("test-model-delete");
        ModelPO po = buildModel(providerId, "claude-3", "Claude 3");
        modelMapper.insert(po);
        Long id = po.getId();

        modelMapper.deleteById(id);

        ModelPO found = modelMapper.selectById(id);
        assertNull(found);
    }

    @Test
    void selectByProviderId() {
        Long providerId = insertProvider("test-model-filter");
        modelMapper.insert(buildModel(providerId, "model-a", "Model A"));
        modelMapper.insert(buildModel(providerId, "model-b", "Model B"));

        Long count = modelMapper.selectCount(
                new LambdaQueryWrapper<ModelPO>().eq(ModelPO::getProviderId, providerId));

        assertEquals(2, count);
    }

    private Long insertProvider(String name) {
        ProviderPO provider = new ProviderPO();
        provider.setName(name);
        provider.setType(ProviderType.OPENAI);
        provider.setAuthType(AuthType.BEARER);
        provider.setApiBaseUrl("https://api.test.com");
        provider.setStatus(ProviderStatus.ACTIVE);
        providerMapper.insert(provider);
        return provider.getId();
    }

    private ModelPO buildModel(Long providerId, String name, String displayName) {
        ModelPO po = new ModelPO();
        po.setProviderId(providerId);
        po.setName(name);
        po.setDisplayName(displayName);
        po.setMaxTokens(4096);
        po.setStatus(ProviderStatus.ACTIVE);
        return po;
    }
}
