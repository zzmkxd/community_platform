package com.community.common.transaction.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.common.transaction.domain.entity.SecureInvokeRecord;
import com.community.common.transaction.mapper.SecureInvokeRecordMapper;
import com.community.common.transaction.service.SecureInvokeService;

import java.time.LocalDateTime;
import java.util.List;

// ponytail: 由 TransactionAutoConfiguration @Import 按需加载，不加 @Component
public class SecureInvokeRecordDao extends ServiceImpl<SecureInvokeRecordMapper, SecureInvokeRecord> {

    public List<SecureInvokeRecord> getWaitRetryRecords() {
        LocalDateTime now = LocalDateTime.now();
        // 查 2 分钟前的失败数据，避免刚入库的数据被扫到
        LocalDateTime afterTime = now.minusMinutes((long) SecureInvokeService.RETRY_INTERVAL_MINUTES);
        return lambdaQuery()
                .eq(SecureInvokeRecord::getStatus, SecureInvokeRecord.STATUS_WAIT)
                .lt(SecureInvokeRecord::getNextRetryTime, now)
                .lt(SecureInvokeRecord::getCreateTime, afterTime)
                .list();
    }
}
