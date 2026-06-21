package com.community.common.cache;

import cn.hutool.core.collection.CollectionUtil;
import com.community.common.utils.RedisUtils;
import org.springframework.data.util.Pair;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRedisStringCache<IN, OUT> implements BatchCache<IN, OUT> {

    private Class<OUT> outClass;

    @SuppressWarnings("unchecked")
    protected AbstractRedisStringCache() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        this.outClass = (Class<OUT>) genericSuperclass.getActualTypeArguments()[1];
    }

    protected abstract String getKey(IN req);

    protected abstract Long getExpireSeconds();

    protected abstract Map<IN, OUT> load(List<IN> req);

    @Override
    public OUT get(IN req) {
        return getBatch(Collections.singletonList(req)).get(req);
    }

    @Override
    public Map<IN, OUT> getBatch(List<IN> req) {
        if (CollectionUtil.isEmpty(req)) {
            return new HashMap<>();
        }
        req = req.stream().distinct().collect(Collectors.toList());
        List<String> keys = req.stream().map(this::getKey).collect(Collectors.toList());
        List<OUT> valueList = RedisUtils.mget(keys, outClass);
        List<IN> loadReqs = new ArrayList<>();
        for (int i = 0; i < valueList.size(); i++) {
            if (Objects.isNull(valueList.get(i))) {
                loadReqs.add(req.get(i));
            }
        }
        Map<IN, OUT> load = new HashMap<>();
        if (CollectionUtil.isNotEmpty(loadReqs)) {
            load = load(loadReqs);
            Map<String, OUT> loadMap = load.entrySet().stream()
                    .map(a -> Pair.of(getKey(a.getKey()), a.getValue()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            RedisUtils.mset(loadMap, getExpireSeconds());
        }
        Map<IN, OUT> resultMap = new HashMap<>();
        for (int i = 0; i < req.size(); i++) {
            IN in = req.get(i);
            OUT out = Optional.ofNullable(valueList.get(i)).orElse(load.get(in));
            resultMap.put(in, out);
        }
        return resultMap;
    }

    @Override
    public void delete(IN req) {
        deleteBatch(Collections.singletonList(req));
    }

    @Override
    public void deleteBatch(List<IN> req) {
        List<String> keys = req.stream().map(this::getKey).collect(Collectors.toList());
        RedisUtils.del(keys);
    }
}
