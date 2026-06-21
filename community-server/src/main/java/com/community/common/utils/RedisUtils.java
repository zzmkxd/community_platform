package com.community.common.utils;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RedisUtils {

    private static StringRedisTemplate redisTemplate;

    public RedisUtils(StringRedisTemplate redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
    }

    // ---- String 操作 ----

    public static String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public static void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    public static Boolean expire(String key, long seconds) {
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    // ---- 批量操作 ----

    public static <T> List<T> mget(List<String> keys, Class<T> clazz) {
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(v -> v != null ? JSONUtil.toBean(v, clazz) : null)
                .collect(Collectors.toList());
    }

    public static <T> void mset(Map<String, T> map, long seconds) {
        Map<String, String> strMap = map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> JSONUtil.toJsonStr(e.getValue())));
        redisTemplate.opsForValue().multiSet(strMap);
        for (String key : strMap.keySet()) {
            redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        }
    }

    // ---- 删除 ----

    public static void del(String key) {
        redisTemplate.delete(key);
    }

    public static void del(List<String> keys) {
        redisTemplate.delete(keys);
    }

    // ---- Set 操作 ----

    public static void sAdd(String key, String... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public static Boolean sIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public static void sRemove(String key, String... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    // ---- 自增 ----

    public static Long inc(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ---- TTL ----

    public static Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }
}
