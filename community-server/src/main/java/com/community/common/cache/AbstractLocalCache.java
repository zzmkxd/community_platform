package com.community.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractLocalCache<IN, OUT> implements BatchCache<IN, OUT> {

    private LoadingCache<IN, OUT> cache;

    protected AbstractLocalCache() {
        init(60, 10 * 60, 1024);
    }

    protected AbstractLocalCache(long refreshSeconds, long expireSeconds, int maxSize) {
        init(refreshSeconds, expireSeconds, maxSize);
    }

    private void init(long refreshSeconds, long expireSeconds, int maxSize) {
        cache = Caffeine.newBuilder()
                .refreshAfterWrite(refreshSeconds, TimeUnit.SECONDS)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .build(key -> load(Collections.singletonList(key)).get(key));
    }

    protected abstract Map<IN, OUT> load(List<IN> req);

    @Override
    public OUT get(IN req) {
        return cache.get(req);
    }

    @Override
    public Map<IN, OUT> getBatch(List<IN> req) {
        return cache.getAll(req);
    }

    @Override
    public void delete(IN req) {
        cache.invalidate(req);
    }

    @Override
    public void deleteBatch(List<IN> req) {
        cache.invalidateAll(req);
    }
}
