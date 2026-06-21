package com.community.common.cache;

import java.util.List;
import java.util.Map;

public interface BatchCache<IN, OUT> {

    OUT get(IN req);

    Map<IN, OUT> getBatch(List<IN> req);

    void delete(IN req);

    void deleteBatch(List<IN> req);
}
