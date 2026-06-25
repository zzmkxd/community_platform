package com.community.common.sensitive;

import com.community.common.algorithm.sensitiveWord.IWordFactory;
import com.community.common.sensitive.dao.SensitiveWordDao;
import com.community.common.sensitive.domain.SensitiveWord;

import java.util.List;
import java.util.stream.Collectors;

// ponytail: 取消 @Component，由 SensitiveWordConfig 按需创建
// 避免没有扫描 SensitiveWordMapper 的服务启动失败
public class MyWordFactory implements IWordFactory {

    private final SensitiveWordDao sensitiveWordDao;

    public MyWordFactory(SensitiveWordDao sensitiveWordDao) {
        this.sensitiveWordDao = sensitiveWordDao;
    }

    @Override
    public List<String> getWordList() {
        return sensitiveWordDao.list()
                .stream()
                .map(SensitiveWord::getWord)
                .collect(Collectors.toList());
    }
}
