package com.community.common.sensitive.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.common.sensitive.domain.SensitiveWord;
import com.community.common.sensitive.dao.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SensitiveWordDao extends ServiceImpl<SensitiveWordMapper, SensitiveWord> {
}
