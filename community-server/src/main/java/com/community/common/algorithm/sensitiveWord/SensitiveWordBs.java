package com.community.common.algorithm.sensitiveWord;

import java.util.List;

public class SensitiveWordBs {

    private SensitiveWordBs() {
    }

    private SensitiveWordFilter sensitiveWordFilter = DFAFilter.getInstance();

    private IWordFactory wordDeny;

    public static SensitiveWordBs newInstance() {
        return new SensitiveWordBs();
    }

    public SensitiveWordBs init() {
        List<String> words = wordDeny.getWordList();
        loadWord(words);
        return this;
    }

    public SensitiveWordBs filterStrategy(SensitiveWordFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter can not be null");
        }
        this.sensitiveWordFilter = filter;
        return this;
    }

    public SensitiveWordBs sensitiveWord(IWordFactory wordFactory) {
        if (wordFactory == null) {
            throw new IllegalArgumentException("wordFactory can not be null");
        }
        this.wordDeny = wordFactory;
        return this;
    }

    public boolean hasSensitiveWord(String text) {
        return sensitiveWordFilter.hasSensitiveWord(text);
    }

    public String filter(String text) {
        return sensitiveWordFilter.filter(text);
    }

    private void loadWord(List<String> words) {
        sensitiveWordFilter.loadWord(words);
    }
}
