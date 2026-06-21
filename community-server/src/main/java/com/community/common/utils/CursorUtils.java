package com.community.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.alanpoi.analysis.lambda.LambdaUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;

import java.util.Optional;
import java.util.function.Consumer;

public class CursorUtils {

    public static <T> CursorPageBaseResp<T> getCursorPageByMysql(IService<T> mapper, CursorPageBaseReq request,
                                                                   Consumer<LambdaQueryWrapper<T>> initWrapper,
                                                                   SFunction<T, ?> cursorColumn) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        initWrapper.accept(wrapper);
        if (cn.hutool.core.util.StrUtil.isNotBlank(request.getCursor())) {
            wrapper.lt(cursorColumn, request.getCursor());
        }
        wrapper.orderByDesc(cursorColumn);

        Page<T> page = mapper.page(request.plusPage(), wrapper);
        String cursor = Optional.ofNullable(CollectionUtil.getLast(page.getRecords()))
                .map(cursorColumn)
                .map(Object::toString)
                .orElse(null);
        Boolean isLast = page.getRecords().size() != request.getPageSize();
        return new CursorPageBaseResp<>(cursor, isLast, page.getRecords());
    }
}
