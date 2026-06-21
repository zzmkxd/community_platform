package com.community.user.controller;

import com.community.user.service.WxMsgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestController
@RequestMapping("/wx/portal/public")
@RequiredArgsConstructor
public class WxPortalController {

    private final WxMpService wxMpService;
    private final WxMpMessageRouter messageRouter;
    private final WxMsgService wxMsgService;

    /**
     * 微信服务器验证
     */
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String authGet(@RequestParam(name = "signature", required = false) String signature,
                          @RequestParam(name = "timestamp", required = false) String timestamp,
                          @RequestParam(name = "nonce", required = false) String nonce,
                          @RequestParam(name = "echostr", required = false) String echostr) {
        log.info("微信服务器验证: signature={}, timestamp={}, nonce={}, echostr={}", signature, timestamp, nonce, echostr);
        if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
            throw new IllegalArgumentException("请求参数非法");
        }
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        return "非法请求";
    }

    /**
     * 微信消息/事件推送
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        log.info("接收微信请求: openid={}, signature={}, encType={}, msgSignature={}", openid, signature, encType, msgSignature);
        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("非法请求");
        }
        String out = null;
        if (encType == null) {
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = route(inMessage);
            if (outMessage != null) {
                out = outMessage.toXml();
            }
        } else if ("aes".equalsIgnoreCase(encType)) {
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody,
                    wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            WxMpXmlOutMessage outMessage = route(inMessage);
            if (outMessage != null) {
                out = outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
            }
        }
        return out;
    }

    private WxMpXmlOutMessage route(WxMpXmlMessage message) {
        try {
            return this.messageRouter.route(message);
        } catch (Exception e) {
            log.error("路由消息异常", e);
        }
        return null;
    }

    /**
     * OAuth2 回调
     */
    @GetMapping("/callBack")
    public RedirectView callBack(@RequestParam String code) {
        try {
            WxOAuth2AccessToken accessToken = wxMpService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxMpService.getOAuth2Service().getUserInfo(accessToken, "zh_CN");
            wxMsgService.authorize(userInfo);
        } catch (Exception e) {
            log.error("微信 OAuth 回调失败", e);
        }
        return new RedirectView("https://www.baidu.com");
    }
}
