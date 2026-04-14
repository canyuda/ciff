package com.ciff.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.dto.PageResult;

import java.util.List;
import java.util.function.Function;

public final class PageHelper {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private PageHelper() {
    }

    public static <T> Page<T> toPage(Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(p, size);
    }

    public static <T> PageResult<T> toPageResult(IPage<T> iPage) {
        return PageResult.of(iPage.getRecords(), iPage.getTotal(),
                (int) iPage.getCurrent(), (int) iPage.getSize());
    }

    public static <T, R> PageResult<R> toPageResult(IPage<T> iPage, Function<T, R> converter) {
        List<R> list = iPage.getRecords().stream().map(converter).toList();
        return PageResult.of(list, iPage.getTotal(),
                (int) iPage.getCurrent(), (int) iPage.getSize());
    }
}
