package com.community.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @mention parser — extracts mentioned usernames from message text.
 * Pattern matches {@code @username} where username is 1-32 word chars.
 */
public class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w]{1,32})");

    /**
     * Extract all distinct mentioned usernames from text.
     */
    public static List<String> extractMentions(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!mentions.contains(name)) {
                mentions.add(name);
            }
        }
        return mentions;
    }

    /**
     * Check if text contains @all or @everyone (broadcast mention).
     */
    public static boolean hasAtAll(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.matches("(?s).*@(all|everyone|所有人).*");
    }

    /**
     * Returns deduplicated mention names, excluding special broadcast keywords.
     */
    public static List<String> extractUserMentions(String text) {
        List<String> all = extractMentions(text);
        Set<String> specials = Set.of("all", "everyone", "所有人");
        return all.stream().filter(m -> !specials.contains(m)).toList();
    }
}
