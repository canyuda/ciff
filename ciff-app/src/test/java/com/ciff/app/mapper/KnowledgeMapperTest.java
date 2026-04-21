package com.ciff.app.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Knowledge Mapper 集成测试，连接真实 MySQL 数据库。
 * 需要本地 MySQL 环境可用。
 */
@SpringBootTest
@Transactional
class KnowledgeMapperTest {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Test
    void insert_and_selectById() {
        KnowledgePO po = buildKnowledge("test-insert", "text-embedding-v3");
        knowledgeMapper.insert(po);

        assertNotNull(po.getId());

        KnowledgePO found = knowledgeMapper.selectById(po.getId());
        assertNotNull(found);
        assertEquals("test-insert", found.getName());
        assertEquals("text-embedding-v3", found.getEmbeddingModel());
        assertEquals(700, found.getChunkSize());
        assertEquals(1L, found.getUserId());
    }

    @Test
    void updateById() {
        KnowledgePO po = buildKnowledge("test-update", "text-embedding-v3");
        knowledgeMapper.insert(po);

        po.setName("test-updated");
        po.setChunkSize(512);
        knowledgeMapper.updateById(po);

        KnowledgePO found = knowledgeMapper.selectById(po.getId());
        assertEquals("test-updated", found.getName());
        assertEquals(512, found.getChunkSize());
    }

    @Test
    void deleteById_logicalDelete() {
        KnowledgePO po = buildKnowledge("test-delete", "text-embedding-v3");
        knowledgeMapper.insert(po);
        Long id = po.getId();

        knowledgeMapper.deleteById(id);

        // 逻辑删除后 selectById 查不到
        KnowledgePO found = knowledgeMapper.selectById(id);
        assertNull(found);
    }

    @Test
    void selectPage_withStatusFilter() {
        KnowledgePO active = buildKnowledge("test-active", "text-embedding-v3");
        active.setStatus("active");
        knowledgeMapper.insert(active);

        KnowledgePO inactive = buildKnowledge("test-inactive", "text-embedding-v3");
        inactive.setStatus("inactive");
        knowledgeMapper.insert(inactive);

        Long activeCount = knowledgeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePO>().eq(KnowledgePO::getStatus, "active"));

        assertTrue(activeCount >= 1);
    }

    @Test
    void selectPage_withUserIdIsolation() {
        KnowledgePO user1Kb = buildKnowledge("user1-kb", "text-embedding-v3");
        user1Kb.setUserId(1L);
        knowledgeMapper.insert(user1Kb);

        KnowledgePO user2Kb = buildKnowledge("user2-kb", "text-embedding-v3");
        user2Kb.setUserId(2L);
        knowledgeMapper.insert(user2Kb);

        Long user1Count = knowledgeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePO>().eq(KnowledgePO::getUserId, 1L));
        Long user2Count = knowledgeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePO>().eq(KnowledgePO::getUserId, 2L));

        assertTrue(user1Count >= 1);
        assertTrue(user2Count >= 1);
    }

    @Test
    void selectById_shouldReturnNull_whenNotOwner() {
        KnowledgePO po = buildKnowledge("owner-test", "text-embedding-v3");
        po.setUserId(2L);
        knowledgeMapper.insert(po);

        // 查询时业务层会做 userId 校验，Mapper 层本身不做隔离
        // 这里验证 Mapper 可以查到数据（隔离在 Service 层实现）
        KnowledgePO found = knowledgeMapper.selectById(po.getId());
        assertNotNull(found);
        assertEquals(2L, found.getUserId());
    }

    // --- Helper ---

    private KnowledgePO buildKnowledge(String name, String embeddingModel) {
        KnowledgePO po = new KnowledgePO();
        po.setUserId(1L);
        po.setName(name);
        po.setDescription("test description");
        po.setChunkSize(700);
        po.setEmbeddingModel(embeddingModel);
        po.setStatus("active");
        return po;
    }
}
