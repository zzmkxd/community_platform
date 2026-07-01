package com.community.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.community.common.annotation.FrequencyControl;
import com.community.common.domain.dto.FrequencyControlDTO;
import com.community.common.service.frequencycontrol.FrequencyControlUtil;
import com.community.common.utils.RequestHolder;
import com.community.common.utils.SpElUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.community.common.service.frequencycontrol.FrequencyControlStrategyFactory.TOTAL_COUNT_WITH_IN_FIX_TIME;

@Slf4j
@Aspect
@Component
public class FrequencyControlAspect {

    @Around("@annotation(com.community.common.annotation.FrequencyControl)||@annotation(com.community.common.annotation.FrequencyControlContainer)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        FrequencyControl[] annotations = method.getAnnotationsByType(FrequencyControl.class);
        Map<String, FrequencyControl> keyMap = new HashMap<>();
        for (int i = 0; i < annotations.length; i++) {
            FrequencyControl fc = annotations[i];
            String prefix = StrUtil.isBlank(fc.prefixKey()) ? SpElUtils.getMethodKey(method) + ":index:" + i : fc.prefixKey();
            String key = switch (fc.target()) {
                case EL -> SpElUtils.parseSpEl(method, joinPoint.getArgs(), fc.spEl());
                case IP -> RequestHolder.get().getIp();
                case UID -> RequestHolder.get().getUid().toString();
            };
            keyMap.put(prefix + ":" + key, fc);
        }
        List<FrequencyControlDTO> dtos = keyMap.entrySet().stream()
                .map(e -> FrequencyControlDTO.builder()
                        .key(e.getKey())
                        .count(e.getValue().count())
                        .time(e.getValue().time())
                        .unit(e.getValue().unit())
                        .build())
                .collect(Collectors.toList());
        return FrequencyControlUtil.executeWithFrequencyControlList(TOTAL_COUNT_WITH_IN_FIX_TIME, dtos, joinPoint::proceed);
    }
}
