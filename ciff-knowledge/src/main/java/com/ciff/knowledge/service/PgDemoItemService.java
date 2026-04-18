package com.ciff.knowledge.service;

import com.ciff.knowledge.entity.PgDemoItem;

import java.util.List;

public interface PgDemoItemService {

    PgDemoItem create(String name);

    PgDemoItem getById(Long id);

    List<PgDemoItem> list(int page, int pageSize);

    PgDemoItem update(Long id, String name);

    void delete(Long id);
}