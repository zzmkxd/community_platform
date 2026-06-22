package com.community.message.service.adapter;

import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.MessageVO;
import com.community.user.domain.entity.User;

import java.util.List;

public class MessageAdapter {

    public static MessageVO buildMessageVO(Message message, User fromUser, List<Reaction> reactions, Thread thread) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setChannelId(message.getChannelId());
        vo.setThreadId(message.getThreadId());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setStatus(message.getStatus());
        vo.setReplyMsgId(message.getReplyMsgId());
        vo.setCreateTime(message.getCreateTime());
        vo.setUpdateTime(message.getUpdateTime());

        if (fromUser != null) {
            MessageVO.MessageUserVO userVO = new MessageVO.MessageUserVO();
            userVO.setId(fromUser.getId());
            userVO.setNickname(fromUser.getNickname());
            userVO.setAvatar(fromUser.getAvatar());
            vo.setFromUser(userVO);
        }

        if (reactions != null && !reactions.isEmpty()) {
            vo.setReactions(reactions.stream().map(r -> {
                com.community.message.domain.vo.ReactionVO rvo = new com.community.message.domain.vo.ReactionVO();
                rvo.setEmoji(r.getEmoji());
                rvo.setCount(1);
                rvo.setUserIds(List.of(r.getUserId()));
                return rvo;
            }).toList());
        }

        if (thread != null) {
            MessageVO.ThreadSummaryVO ts = new MessageVO.ThreadSummaryVO();
            ts.setId(thread.getId());
            ts.setName(thread.getName());
            ts.setMessageCount(thread.getMessageCount());
            vo.setThread(ts);
        }

        return vo;
    }
}
