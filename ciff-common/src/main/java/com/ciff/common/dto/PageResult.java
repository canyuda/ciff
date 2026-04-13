package com.ciff.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        return new PageResult<>(list == null ? Collections.emptyList() : list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0, page, pageSize);
    }
}
