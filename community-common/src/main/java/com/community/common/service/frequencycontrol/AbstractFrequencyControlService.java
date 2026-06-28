package com.community.common.service.frequencycontrol;

import com.community.common.domain.dto.FrequencyControlDTO;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractFrequencyControlService<K extends FrequencyControlDTO> {

    @PostConstruct
    protected void registerMyselfToFactory() {
        FrequencyControlStrategyFactory.register(getStrategyName(), this);
    }

    @SuppressWarnings("unchecked")
    public <T> T executeWithFrequencyControlList(List<K> frequencyControlList, SupplierThrowWithoutParam<T> supplier) throws Throwable {
        Map<String, K> frequencyControlMap = frequencyControlList.stream()
                .collect(Collectors.toMap(FrequencyControlDTO::getKey, f -> f, (a, b) -> a));
        if (reachRateLimit(frequencyControlMap)) {
            throw new BusinessException(BusinessErrorEnum.FREQUENCY_LIMIT);
        }
        try {
            return supplier.get();
        } finally {
            addFrequencyControlStatisticsCount(frequencyControlMap);
        }
    }

    public <T> T executeWithFrequencyControl(K frequencyControl, SupplierThrowWithoutParam<T> supplier) throws Throwable {
        return executeWithFrequencyControlList(Collections.singletonList(frequencyControl), supplier);
    }

    @FunctionalInterface
    public interface SupplierThrowWithoutParam<T> {
        T get() throws Throwable;
    }

    @FunctionalInterface
    public interface Executor {
        void execute() throws Throwable;
    }

    protected abstract boolean reachRateLimit(Map<String, K> frequencyControlMap);

    protected abstract void addFrequencyControlStatisticsCount(Map<String, K> frequencyControlMap);

    protected abstract String getStrategyName();
}
