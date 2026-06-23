package com.community.common.service.frequencycontrol;

import com.community.common.domain.dto.FrequencyControlDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrequencyControlStrategyFactory {

    public static final String TOTAL_COUNT_WITH_IN_FIX_TIME = "TotalCountWithInFixTime";

    static Map<String, AbstractFrequencyControlService<?>> strategyMap = new ConcurrentHashMap<>(8);

    public static <K extends FrequencyControlDTO> void register(String strategyName, AbstractFrequencyControlService<K> service) {
        strategyMap.put(strategyName, service);
    }

    @SuppressWarnings("unchecked")
    public static <K extends FrequencyControlDTO> AbstractFrequencyControlService<K> getByName(String strategyName) {
        return (AbstractFrequencyControlService<K>) strategyMap.get(strategyName);
    }

    private FrequencyControlStrategyFactory() {}
}
