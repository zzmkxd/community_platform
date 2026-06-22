package com.community.common.sensitive;

import com.community.common.algorithm.sensitiveWord.IWordFactory;
import com.community.common.sensitive.dao.SensitiveWordDao;
import com.community.common.sensitive.domain.SensitiveWord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MyWordFactory implements IWordFactory {

    private final SensitiveWordDao sensitiveWordDao;

    @Override
    public List<String> getWordList() {
        return sensitiveWordDao.list()
                .stream()
                .map(SensitiveWord::getWord)
                .collect(Collectors.toList());
    }
}
