package com.community.common.algorithm.sensitiveWord;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * DFA-based sensitive word filter with skip-character handling.
 */
public final class DFAFilter implements SensitiveWordFilter {

    private DFAFilter() {
    }

    private static Word root = new Word(' ');
    private static final char replace = '*';
    private static final String skipChars = " !*-+_=,，.@;:；：。、？?（）()【】[]《》<>\"\"''";
    private static final Set<Character> skipSet = new HashSet<>();

    static {
        for (char c : skipChars.toCharArray()) {
            skipSet.add(c);
        }
    }

    public static DFAFilter getInstance() {
        return new DFAFilter();
    }

    @Override
    public boolean hasSensitiveWord(String text) {
        if (StringUtils.isBlank(text)) return false;
        return !Objects.equals(filter(text), text);
    }

    @Override
    public String filter(String text) {
        StringBuilder result = new StringBuilder(text);
        int index = 0;
        while (index < result.length()) {
            char c = result.charAt(index);
            if (skip(c)) {
                index++;
                continue;
            }
            Word word = root;
            int start = index;
            boolean found = false;
            for (int i = index; i < result.length(); i++) {
                c = result.charAt(i);
                if (skip(c)) {
                    continue;
                }
                if (c >= 'A' && c <= 'Z') {
                    c += 32;
                }
                word = word.next.get(c);
                if (word == null) {
                    break;
                }
                if (word.end) {
                    found = true;
                    for (int j = start; j <= i; j++) {
                        result.setCharAt(j, replace);
                    }
                    index = i;
                }
            }
            if (!found) {
                index++;
            }
        }
        return result.toString();
    }

    @Override
    public void loadWord(List<String> words) {
        if (!CollectionUtils.isEmpty(words)) {
            Word newRoot = new Word(' ');
            words.forEach(word -> loadWord(word, newRoot));
            root = newRoot;
        }
    }

    public void loadWord(String word, Word root) {
        if (StringUtils.isBlank(word)) {
            return;
        }
        Word current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c += 32;
            }
            if (skip(c)) {
                continue;
            }
            Word next = current.next.get(c);
            if (next == null) {
                next = new Word(c);
                current.next.put(c, next);
            }
            current = next;
        }
        current.end = true;
    }

    private boolean skip(char c) {
        return skipSet.contains(c);
    }

    private static class Word {
        private final char c;
        private boolean end;
        private Map<Character, Word> next;

        public Word(char c) {
            this.c = c;
            this.next = new HashMap<>();
        }
    }
}
