package com.community.common.config;

import com.community.user.service.handler.LogHandler;
import com.community.user.service.handler.MsgHandler;
import com.community.user.service.handler.ScanHandler;
import com.community.user.service.handler.SubscribeHandler;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

import static me.chanjar.weixin.common.api.WxConsts.EventType.SCAN;
import static me.chanjar.weixin.common.api.WxConsts.EventType.SUBSCRIBE;
import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType.EVENT;

@Configuration
@EnableConfigurationProperties(WxMpProperties.class)
@RequiredArgsConstructor
public class WxMpConfiguration {

    private final LogHandler logHandler;
    private final SubscribeHandler subscribeHandler;
    private final ScanHandler scanHandler;
    private final MsgHandler msgHandler;
    private final WxMpProperties wxMpProperties;

    @Bean
    public WxMpService wxMpService() {
        if (wxMpProperties.getConfigs() == null) {
            throw new RuntimeException("wx.mp.configs is not configured");
        }
        WxMpService service = new WxMpServiceImpl();
        service.setMultiConfigStorages(wxMpProperties.getConfigs()
                .stream().map(a -> {
                    WxMpDefaultConfigImpl configStorage = new WxMpDefaultConfigImpl();
                    configStorage.setAppId(a.getAppId());
                    configStorage.setSecret(a.getSecret());
                    configStorage.setToken(a.getToken());
                    configStorage.setAesKey(a.getAesKey());
                    return configStorage;
                }).collect(java.util.stream.Collectors.toMap(
                        WxMpDefaultConfigImpl::getAppId, a -> a, (o, n) -> o)));
        return service;
    }

    @Bean
    public WxMpMessageRouter messageRouter(WxMpService wxMpService) {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        router.rule().async(false).handler(logHandler).next();
        router.rule().msgType(EVENT).event(SUBSCRIBE).async(false).handler(subscribeHandler).end();
        router.rule().msgType(EVENT).event(SCAN).async(false).handler(scanHandler).end();
        router.rule().async(false).handler(msgHandler).end();
        return router;
    }
}
