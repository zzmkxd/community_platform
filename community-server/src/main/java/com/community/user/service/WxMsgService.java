package com.community.user.service;

import cn.hutool.core.util.RandomUtil;
import com.community.common.constant.MQConstant;
import com.community.common.constant.RedisKey;
import com.community.common.domain.dto.LoginMessageDTO;
import com.community.common.domain.dto.ScanSuccessMessageDTO;
import com.community.common.utils.RedisUtils;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import com.community.user.service.adapter.TextBuilder;
import com.community.user.service.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxMsgService {

    private static final String URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";

    @Value("${wx.mp.callback}")
    private String callback;

    private final UserDao userDao;
    private final RocketMQTemplate rocketMQTemplate;

    public WxMpXmlOutMessage scan(WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage) {
        String openid = wxMpXmlMessage.getFromUser();
        String eventKey = getEventKey(wxMpXmlMessage);
        if (!eventKey.matches("\\d+")) {
            log.warn("scan() called with non-numeric eventKey: {}", eventKey);
            return null;
        }
        Integer loginCode = Integer.parseInt(eventKey);
        User user = userDao.getByOpenId(openid);
        // 已注册且有头像，直接登录成功
        if (Objects.nonNull(user) && StringUtils.isNotEmpty(user.getAvatar())) {
            sendMsg(MQConstant.LOGIN_MSG_TOPIC, new LoginMessageDTO(user.getId(), loginCode));
            return null;
        }
        // 用户不存在则先创建
        if (Objects.isNull(user)) {
            user = new User();
            user.setOpenId(openid);
            user.setNickname("微信用户" + RandomUtil.randomInt(100000));
            userDao.save(user);
        }
        // Redis 存 openId → loginCode 映射（60 分钟）
        RedisUtils.set(RedisKey.OPEN_ID_STRING + openid, String.valueOf(loginCode), TimeUnit.MINUTES.toSeconds(60));
        // 通知前端扫码成功
        sendMsg(MQConstant.SCAN_MSG_TOPIC, new ScanSuccessMessageDTO(loginCode));
        // 返回授权链接
        String skipUrl = String.format(URL,
                wxMpService.getWxMpConfigStorage().getAppId(),
                URLEncoder.encode(callback + "/wx/portal/public/callBack"));
        return new TextBuilder().build("请点击链接授权：<a href=\"" + skipUrl + "\">登录</a>", wxMpXmlMessage, wxMpService);
    }

    private String getEventKey(WxMpXmlMessage wxMpXmlMessage) {
        return wxMpXmlMessage.getEventKey().replace("qrscene_", "");
    }

    public void authorize(WxOAuth2UserInfo userInfo) {
        User user = userDao.getByOpenId(userInfo.getOpenid());
        if (user == null) {
            user = new User();
            user.setOpenId(userInfo.getOpenid());
            user.setNickname("微信用户" + RandomUtil.randomInt(100000));
            userDao.save(user);
        }
        // 更新用户信息（首次授权或无头像时填入微信信息）
        if (StringUtils.isEmpty(user.getAvatar()) || StringUtils.isEmpty(user.getNickname())
                || user.getNickname().startsWith("微信用户")) {
            fillUserInfo(user.getId(), userInfo);
        }
        // 找到对应的 code
        String codeStr = RedisUtils.get(RedisKey.OPEN_ID_STRING + userInfo.getOpenid());
        Integer code = codeStr != null ? Integer.parseInt(codeStr) : null;
        // 发送登录成功事件
        sendMsg(MQConstant.LOGIN_MSG_TOPIC, new LoginMessageDTO(user.getId(), code));
    }

    private void fillUserInfo(Long uid, WxOAuth2UserInfo userInfo) {
        User update = UserAdapter.buildAuthorizeUser(uid, userInfo);
        for (int i = 0; i < 5; i++) {
            try {
                userDao.updateById(update);
                return;
            } catch (Exception e) {
                log.info("fill userInfo duplicate uid:{},info:{}", uid, userInfo);
            }
            update.setNickname("名字重置" + RandomUtil.randomInt(100000));
        }
    }

    private void sendMsg(String topic, Object body) {
        Message<Object> build = MessageBuilder.withPayload(body).build();
        rocketMQTemplate.send(topic, build);
    }
}
