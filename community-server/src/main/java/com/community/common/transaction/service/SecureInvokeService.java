package com.community.common.transaction.service;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.community.common.transaction.dao.SecureInvokeRecordDao;
import com.community.common.transaction.domain.dto.SecureInvokeDTO;
import com.community.common.transaction.domain.entity.SecureInvokeRecord;
import com.community.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class SecureInvokeService {

    public static final double RETRY_INTERVAL_MINUTES = 2D;

    private final SecureInvokeRecordDao secureInvokeRecordDao;

    private final Executor executor;

    @Scheduled(cron = "*/5 * * * * ?")
    public void retry() {
        List<SecureInvokeRecord> records = secureInvokeRecordDao.getWaitRetryRecords();
        for (SecureInvokeRecord record : records) {
            doAsyncInvoke(record);
        }
    }

    public void save(SecureInvokeRecord record) {
        secureInvokeRecordDao.save(record);
    }

    private void retryRecord(SecureInvokeRecord record, String errorMsg) {
        int retryTimes = record.getRetryTimes() + 1;
        SecureInvokeRecord update = new SecureInvokeRecord();
        update.setId(record.getId());
        update.setFailReason(errorMsg);
        update.setNextRetryTime(getNextRetryTime(retryTimes));
        if (retryTimes > record.getMaxRetryTimes()) {
            update.setStatus(SecureInvokeRecord.STATUS_FAIL);
        } else {
            update.setRetryTimes(retryTimes);
        }
        secureInvokeRecordDao.updateById(update);
    }

    private LocalDateTime getNextRetryTime(int retryTimes) {
        // 重试时间指数上升: 2m, 4m, 8m, 16m...
        double waitMinutes = Math.pow(RETRY_INTERVAL_MINUTES, retryTimes);
        return LocalDateTime.now().plusMinutes((long) waitMinutes);
    }

    private void removeRecord(Long id) {
        secureInvokeRecordDao.removeById(id);
    }

    public void invoke(SecureInvokeRecord record, boolean async) {
        boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        if (!inTransaction) {
            return;
        }
        save(record);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                if (async) {
                    doAsyncInvoke(record);
                } else {
                    doInvoke(record);
                }
            }
        });
    }

    public void doAsyncInvoke(SecureInvokeRecord record) {
        executor.execute(() -> doInvoke(record));
    }

    public void doInvoke(SecureInvokeRecord record) {
        SecureInvokeDTO dto = record.getSecureInvokeDTO();
        try {
            SecureInvokeHolder.setInvoking();
            Class<?> beanClass = Class.forName(dto.getClassName());
            Object bean = SpringUtil.getBean(beanClass);
            List<String> parameterStrings = JsonUtils.toList(dto.getParameterTypes(), String.class);
            List<Class<?>> parameterClasses = parameterStrings.stream().map(name -> {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    log.error("SecureInvokeService class not found: {}", name, e);
                    return null;
                }
            }).collect(Collectors.toList());
            Method method = ReflectUtil.getMethod(beanClass, dto.getMethodName(),
                    parameterClasses.toArray(new Class[]{}));
            Object[] args = getArgs(dto, parameterClasses);
            method.invoke(bean, args);
            removeRecord(record.getId());
        } catch (Throwable e) {
            log.error("SecureInvokeService invoke fail", e);
            retryRecord(record, e.getMessage());
        } finally {
            SecureInvokeHolder.invoked();
        }
    }

    private Object[] getArgs(SecureInvokeDTO dto, List<Class<?>> parameterClasses) {
        JsonNode jsonNode = JsonUtils.toJsonNode(dto.getArgs());
        Object[] args = new Object[jsonNode.size()];
        for (int i = 0; i < jsonNode.size(); i++) {
            Class<?> clz = parameterClasses.get(i);
            args[i] = JsonUtils.nodeToValue(jsonNode.get(i), clz);
        }
        return args;
    }
}
