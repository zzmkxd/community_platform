package com.community.common.service.frequencycontrol;

import com.community.common.domain.dto.FrequencyControlDTO;

import java.util.List;

public class FrequencyControlUtil {

    public static <T, K extends FrequencyControlDTO> T executeWithFrequencyControl(String strategyName, K frequencyControl,
            AbstractFrequencyControlService.SupplierThrowWithoutParam<T> supplier) throws Throwable {
        AbstractFrequencyControlService<K> controller = FrequencyControlStrategyFactory.getByName(strategyName);
        return controller.executeWithFrequencyControl(frequencyControl, supplier);
    }

    public static <T, K extends FrequencyControlDTO> T executeWithFrequencyControlList(String strategyName,
            List<K> frequencyControlList, AbstractFrequencyControlService.SupplierThrowWithoutParam<T> supplier) throws Throwable {
        AbstractFrequencyControlService<K> controller = FrequencyControlStrategyFactory.getByName(strategyName);
        return controller.executeWithFrequencyControlList(frequencyControlList, supplier);
    }

    private FrequencyControlUtil() {}
}
