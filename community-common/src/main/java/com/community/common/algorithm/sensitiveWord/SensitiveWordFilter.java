package com.community.common.algorithm.sensitiveWord;

import java.util.List;

public interface SensitiveWordFilter {

    boolean hasSensitiveWord(String text);

    String filter(String text);

    void loadWord(List<String> words);
}
