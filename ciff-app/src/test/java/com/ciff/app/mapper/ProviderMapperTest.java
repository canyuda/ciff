package com.ciff.app.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderType;
import com.ciff.provider.entity.ProviderPO;
import com.ciff.provider.mapper.ProviderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Provider Mapper 集成测试，连接真实数据库。
 * 需要本地 MySQL 环境可用。
 */
@SpringBootTest
@Transactional
class ProviderMapperTest {

    @Autowired
    private ProviderMapper providerMapper;

    @Test
    void insert_and_selectById() {
        ProviderPO po = buildProvider("test-insert", "https://api.test.com");
        providerMapper.insert(po);

        assertNotNull(po.getId());

        ProviderPO found = providerMapper.selectById(po.getId());
        assertNotNull(found);
        assertEquals("test-insert", found.getName());
        assertEquals("https://api.test.com", found.getApiBaseUrl());
    }

    @Test
    void updateById() {
        ProviderPO po = buildProvider("test-update", "https://old.com");
        providerMapper.insert(po);

        po.setName("test-updated");
        po.setApiBaseUrl("https://new.com");
        providerMapper.updateById(po);

        ProviderPO found = providerMapper.selectById(po.getId());
        assertEquals("test-updated", found.getName());
        assertEquals("https://new.com", found.getApiBaseUrl());
    }

    @Test
    void deleteById_logicalDelete() {
        ProviderPO po = buildProvider("test-delete", "https://api.test.com");
        providerMapper.insert(po);
        Long id = po.getId();

        providerMapper.deleteById(id);

        // 逻辑删除后 selectById 查不到
        ProviderPO found = providerMapper.selectById(id);
        assertNull(found);
    }

    @Test
    void selectPage_withStatusFilter() {
        ProviderPO active = buildProvider("test-active", "https://a.com");
        active.setStatus("active");
        providerMapper.insert(active);

        ProviderPO inactive = buildProvider("test-inactive", "https://b.com");
        inactive.setStatus("inactive");
        providerMapper.insert(inactive);

        // Filter by status=active
        Long activeCount = providerMapper.selectCount(
                new LambdaQueryWrapper<ProviderPO>().eq(ProviderPO::getStatus, "active"));

        assertTrue(activeCount >= 1);
    }

    private ProviderPO buildProvider(String name, String url) {
        ProviderPO po = new ProviderPO();
        po.setName(name);
        po.setType(ProviderType.OPENAI);
        po.setAuthType(AuthType.BEARER);
        po.setApiBaseUrl(url);
        po.setStatus("active");
        return po;
    }
}
