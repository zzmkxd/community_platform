package com.community.common.algorithm.sensitiveWord.ac;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Aho-Corasick automaton for multi-pattern matching.
 */
public class ACTrie {

    private ACTrieNode root;

    public ACTrie(List<String> words) {
        words = words.stream().distinct().collect(Collectors.toList());
        root = new ACTrieNode();
        for (String word : words) {
            addWord(word);
        }
        initFailover();
    }

    public void addWord(String word) {
        ACTrieNode walkNode = root;
        char[] chars = word.toCharArray();
        for (int i = 0; i < word.length(); i++) {
            walkNode.addChildrenIfAbsent(chars[i]);
            walkNode = walkNode.childOf(chars[i]);
            walkNode.setDepth(i + 1);
        }
        walkNode.setLeaf(true);
    }

    private void initFailover() {
        Queue<ACTrieNode> queue = new LinkedList<>();
        Map<Character, ACTrieNode> children = root.getChildren();
        for (ACTrieNode node : children.values()) {
            node.setFailover(root);
            queue.offer(node);
        }
        while (!queue.isEmpty()) {
            ACTrieNode parentNode = queue.poll();
            for (Map.Entry<Character, ACTrieNode> entry : parentNode.getChildren().entrySet()) {
                ACTrieNode childNode = entry.getValue();
                ACTrieNode failover = parentNode.getFailover();
                while (failover != null && (!failover.hasChild(entry.getKey()))) {
                    failover = failover.getFailover();
                }
                if (failover == null) {
                    childNode.setFailover(root);
                } else {
                    childNode.setFailover(failover.childOf(entry.getKey()));
                }
                queue.offer(childNode);
            }
        }
    }

    public List<MatchResult> matches(String text) {
        List<MatchResult> result = new ArrayList<>();
        ACTrieNode walkNode = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (!walkNode.hasChild(c) && walkNode.getFailover() != null) {
                walkNode = walkNode.getFailover();
            }
            if (walkNode.hasChild(c)) {
                walkNode = walkNode.childOf(c);
                if (walkNode.isLeaf()) {
                    result.add(new MatchResult(i - walkNode.getDepth() + 1, i + 1));
                    walkNode = walkNode.getFailover();
                }
            }
        }
        return result;
    }
}
