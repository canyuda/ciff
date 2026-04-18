package com.ciff.knowledge.service.impl;

import com.ciff.knowledge.entity.PgDemoItem;
import com.ciff.knowledge.service.PgDemoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PgDemoItemServiceImpl implements PgDemoItemService {

    @Qualifier("pgVectorJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PgDemoItem> ROW_MAPPER = (rs, rowNum) -> {
        PgDemoItem item = new PgDemoItem();
        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("create_time");
        item.setCreateTime(ts != null ? ts.toLocalDateTime() : null);
        return item;
    };

    @Override
    public PgDemoItem create(String name) {
        String sql = "INSERT INTO pg_demo_item (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, name);
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        return getById(id);
    }

    @Override
    public PgDemoItem getById(Long id) {
        List<PgDemoItem> list = jdbcTemplate.query(
                "SELECT id, name, status, create_time FROM pg_demo_item WHERE id = ?",
                ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<PgDemoItem> list(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return jdbcTemplate.query(
                "SELECT id, name, status, create_time FROM pg_demo_item ORDER BY id DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, pageSize, offset);
    }

    @Override
    public PgDemoItem update(Long id, String name) {
        jdbcTemplate.update(
                "UPDATE pg_demo_item SET name = ? WHERE id = ?",
                name, id);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM pg_demo_item WHERE id = ?", id);
    }
}