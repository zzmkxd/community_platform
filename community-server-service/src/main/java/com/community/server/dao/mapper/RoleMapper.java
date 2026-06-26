package com.community.server.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.server.domain.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.* FROM role r INNER JOIN member_role mr ON r.id = mr.role_id WHERE mr.member_id = #{memberId}")
    List<Role> findByMemberId(Long memberId);
}
