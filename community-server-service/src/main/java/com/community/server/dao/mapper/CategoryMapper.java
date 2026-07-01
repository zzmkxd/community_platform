package com.community.server.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.server.domain.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
