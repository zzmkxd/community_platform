package com.community.common.sensitive.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.common.sensitive.domain.SensitiveWord;
import com.community.common.sensitive.dao.mapper.SensitiveWordMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver") // ponytail: websocket 等无 DB 服务跳过
public class SensitiveWordDao extends ServiceImpl<SensitiveWordMapper, SensitiveWord> {
}
