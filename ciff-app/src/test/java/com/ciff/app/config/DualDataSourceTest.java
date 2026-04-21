package com.ciff.app.config;

import com.ciff.knowledge.mapper.KnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dual datasource integration test — verifies MySQL and PGVector are both reachable.
 * Requires local MySQL and PostgreSQL services.
 */
@SpringBootTest
class DualDataSourceTest {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    @Qualifier("pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Test
    void mysqlDataSource_shouldBeReachable() {
        // MyBatis-Plus mapper relies on the auto-configured MySQL DataSource
        Long count = knowledgeMapper.selectCount(null);
        assertThat(count).isNotNull();
    }

    @Test
    void pgVectorJdbcTemplate_shouldBeReachable() {
        Integer result = pgVectorJdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void pgVectorExtension_shouldBeInstalled() {
        // PGVector extension must be available for vector operations
        Integer result = pgVectorJdbcTemplate.queryForObject(
                "SELECT 1 FROM pg_extension WHERE extname = 'vector'", Integer.class);
        assertThat(result).isEqualTo(1);
    }
}
