package com.ciff.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Test entity for PGVector datasource verification.
 * Stored in PostgreSQL, mapped via JdbcTemplate RowMapper.
 */
@Data
public class PgDemoItem {

    private Long id;

    private String name;

    private String status;

    private LocalDateTime createTime;
}