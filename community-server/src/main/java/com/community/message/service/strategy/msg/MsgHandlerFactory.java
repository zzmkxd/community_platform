package com.community.message.service.strategy.msg;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsgHandlerFactory {

    private static final Map<Integer, AbstractMsgHandler> STRATEGY_MAP = new ConcurrentHashMap<>();

    public static void register(Integer msgType, AbstractMsgHandler handler) {
        STRATEGY_MAP.put(msgType, handler);
    }

    public static AbstractMsgHandler getStrategyNoNull(Integer msgType) {
        AbstractMsgHandler handler = STRATEGY_MAP.get(msgType);
        if (handler == null) {
            throw new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND);
        }
        return handler;
    }
}
