package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.CategoryVO;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/channels")
@RequiredArgsConstructor
@Tag(name = "频道")
public class ChannelController {

    private final ChannelService channelService;

    @Operation(summary = "创建频道（TEXT 或 VOICE 类型）")
    @PostMapping
    public ApiResult<ChannelVO> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        Long categoryId = body.get("categoryId") != null ? ((Number) body.get("categoryId")).longValue() : null;
        String name = (String) body.get("name");
        Integer type = body.get("type") != null ? ((Number) body.get("type")).intValue() : 0;
        String topic = (String) body.get("topic");
        return ApiResult.success(channelService.createChannel(serverId, categoryId, name, type, topic));
    }

    @Operation(summary = "获取服务器全部频道（按分类分组）")
    @GetMapping
    public ApiResult<List<CategoryVO>> getChannels(@PathVariable Long serverId) {
        return ApiResult.success(channelService.getChannels(serverId));
    }

    @Operation(summary = "获取频道详情")
    @GetMapping("/{chanId}")
    public ApiResult<ChannelVO> getChannel(@PathVariable Long serverId, @PathVariable Long chanId) {
        return ApiResult.success(channelService.getChannel(serverId, chanId));
    }

    @Operation(summary = "修改频道名称和简介")
    @PutMapping("/{chanId}")
    public ApiResult<ChannelVO> update(@PathVariable Long serverId, @PathVariable Long chanId,
                                        @RequestBody Map<String, String> body) {
        return ApiResult.success(channelService.updateChannel(serverId, chanId,
                body.get("name"), body.get("topic")));
    }

    @Operation(summary = "删除频道")
    @DeleteMapping("/{chanId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serverId, @PathVariable Long chanId) {
        channelService.deleteChannel(serverId, chanId);
    }
}
