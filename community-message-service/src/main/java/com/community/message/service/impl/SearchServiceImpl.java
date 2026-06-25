package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ReactionDao;
import com.community.message.domain.document.MessageDocument;
import com.community.message.domain.entity.Message;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MessageDao messageDao;
    private final ReactionDao reactionDao;
    private final UserService userService;
    private final ElasticsearchOperations esOps;

    @Override
    public CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                                 String from, String to, Integer page) {
        // ES 查询：content 全文匹配 + serverId 精确过滤 + status != 1
        var criteria = new Criteria("content").matches(q)
                .and(new Criteria("serverId").is(serverId));
        if (channelId != null) {
            criteria = criteria.and(new Criteria("channelId").is(channelId));
        }
        criteria = criteria.and(new Criteria("status").not().is(1));
        if (from != null) {
            criteria = criteria.and(new Criteria("createTime")
                    .greaterThanEqual(parseDateTime(from)));
        }
        if (to != null) {
            criteria = criteria.and(new Criteria("createTime")
                    .lessThanEqual(parseDateTime(to)));
        }

        var query = new CriteriaQuery(criteria);
        query.setMaxResults(50);
        query.addSort(Sort.by(Sort.Direction.DESC, "createTime"));

        SearchHits<MessageDocument> hits = esOps.search(query, MessageDocument.class);

        List<Long> msgIds = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(MessageDocument::getId)
                .toList();

        if (msgIds.isEmpty()) {
            CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
            resp.setIsLast(true);
            resp.setList(List.of());
            return resp;
        }

        // 按 ES 返回顺序从 MySQL 批量取
        Map<Long, Message> msgMap = messageDao.listByIds(msgIds).stream()
                .collect(Collectors.toMap(Message::getId, m -> m));
        List<Message> ordered = msgIds.stream()
                .map(msgMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<Long> userIds = ordered.stream().map(Message::getFromUid).distinct().toList();
        Map<Long, UserVO> userMap = userService.getBatchUsers(userIds).stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

        Map<Long, List<com.community.message.domain.vo.ReactionVO>> reactionMap =
                MessageAdapter.buildReactionMap(msgIds, reactionDao);

        List<MessageVO> vos = ordered.stream()
                .map(msg -> MessageAdapter.buildMessageVO(
                        msg, userMap.get(msg.getFromUid()),
                        reactionMap.getOrDefault(msg.getId(), List.of()), null))
                .toList();

        CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
        resp.setIsLast(true);
        resp.setList(vos);
        return resp;
    }

    private LocalDateTime parseDateTime(String s) {
        try {
            return LocalDateTime.parse(s.contains("T") ? s : s.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.parse(s.replace(" ", "T") + ":00");
        }
    }
}
