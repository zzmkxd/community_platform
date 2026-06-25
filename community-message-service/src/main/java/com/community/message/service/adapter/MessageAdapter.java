package com.community.message.service.adapter;

import com.community.common.utils.RequestHolder;
import com.community.message.dao.ReactionDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.entity.Thread;
import com.community.file.domain.vo.FileVO;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ReactionVO;
import com.community.common.domain.vo.UserVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageAdapter {

    public static MessageVO buildMessageVO(Message message, UserVO fromUser, List<ReactionVO> reactions, Thread thread) {
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
            vo.setReactions(reactions);
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

    /**
     * 为一批消息批量构建 Reaction 映射（msgId → List<ReactionVO>）
     */
    public static Map<Long, List<ReactionVO>> buildReactionMap(List<Long> msgIds, ReactionDao reactionDao) {
        if (msgIds.isEmpty()) return Map.of();
        List<Reaction> reactions = reactionDao.lambdaQuery()
                .in(Reaction::getMessageId, msgIds)
                .list();

        Map<Long, Map<String, ReactionVO>> grouped = new HashMap<>();
        for (Reaction r : reactions) {
            Map<String, ReactionVO> emojiMap = grouped.computeIfAbsent(r.getMessageId(),
                    k -> new LinkedHashMap<>());
            ReactionVO vo = emojiMap.computeIfAbsent(r.getEmoji(), emoji -> {
                ReactionVO rvo = new ReactionVO();
                rvo.setEmoji(emoji);
                rvo.setCount(0);
                rvo.setUserIds(new ArrayList<>());
                return rvo;
            });
            vo.setCount(vo.getCount() + 1);
            vo.getUserIds().add(r.getUserId());
        }

        Long currentUid = RequestHolder.get() != null ? RequestHolder.get().getUid() : null;
        Map<Long, List<ReactionVO>> result = new HashMap<>();
        for (var entry : grouped.entrySet()) {
            List<ReactionVO> list = new ArrayList<>(entry.getValue().values());
            if (currentUid != null) {
                for (ReactionVO vo : list) {
                    vo.setReacted(vo.getUserIds().contains(currentUid));
                }
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    /**
     * 为一批消息批量构建附件映射（msgId → List<FileVO>）
     * @param messages 消息列表
     * @param fileMap  文件 ID → FileVO 映射
     */
    public static Map<Long, List<FileVO>> buildAttachmentMap(List<Message> messages, Map<Long, FileVO> fileMap) {
        if (fileMap == null || fileMap.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<FileVO>> result = new HashMap<>();
        for (Message msg : messages) {
            MessageExtra extra = msg.getExtra();
            if (extra == null || extra.getFileIds() == null || extra.getFileIds().isEmpty()) continue;
            List<FileVO> files = extra.getFileIds().stream()
                    .map(fileMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (!files.isEmpty()) {
                result.put(msg.getId(), files);
            }
        }
        return result;
    }

    /**
     * 为单条消息构建附件列表
     * @param message 消息
     * @param files   文件 ID → FileVO 映射
     */
    public static List<FileVO> buildAttachments(Message message, Map<Long, FileVO> files) {
        MessageExtra extra = message.getExtra();
        if (extra == null || extra.getFileIds() == null || extra.getFileIds().isEmpty() || files == null || files.isEmpty()) {
            return null;
        }
        return extra.getFileIds().stream()
                .map(files::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
