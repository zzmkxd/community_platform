package com.community.message.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.message.domain.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("<script>"
            + "SELECT m.channel_id, COUNT(*) cnt FROM message m "
            + "LEFT JOIN channel_read_state s ON m.channel_id = s.channel_id AND s.user_id = #{uid} "
            + "WHERE m.channel_id IN "
            + "<foreach collection='channelIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> "
            + "AND m.status != 1 AND m.id &gt; COALESCE(s.last_read_msg_id, 0) "
            + "GROUP BY m.channel_id"
            + "</script>")
    List<Map<String, Object>> countUnreadByChannels(@Param("channelIds") List<Long> channelIds,
                                                     @Param("uid") Long uid);
}
