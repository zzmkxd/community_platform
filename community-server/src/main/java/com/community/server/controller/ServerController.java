package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.message.service.ChannelReadStateService;
import com.community.server.domain.vo.ServerDetailVO;
import com.community.server.domain.vo.ServerVO;
import com.community.server.service.ServerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
@Tag(name = "服务器")
public class ServerController {

    private final ServerService serverService;
    private final ChannelReadStateService channelReadStateService;

    @PostMapping
    public ApiResult<ServerVO> create(@RequestBody Map<String, String> body) {
        return ApiResult.success(serverService.createServer(
                body.get("name"), body.get("description"), body.get("icon")));
    }

    @GetMapping
    public ApiResult<List<ServerVO>> getMyServers() {
        return ApiResult.success(serverService.getMyServers());
    }

    @GetMapping("/discover")
    public ApiResult<List<ServerVO>> getDiscoverableServers() {
        return ApiResult.success(serverService.getDiscoverableServers());
    }

    @GetMapping("/{serverId}")
    public ApiResult<ServerDetailVO> getDetail(@PathVariable Long serverId) {
        return ApiResult.success(serverService.getServerDetail(serverId));
    }

    @GetMapping("/{serverId}/unread")
    public ApiResult<Map<Long, Long>> getUnread(@PathVariable Long serverId) {
        return ApiResult.success(channelReadStateService.getUnreadCounts(serverId));
    }

    @PutMapping("/{serverId}")
    public ApiResult<ServerVO> update(@PathVariable Long serverId, @RequestBody Map<String, String> body) {
        return ApiResult.success(serverService.updateServer(serverId,
                body.get("name"), body.get("description"), body.get("icon")));
    }

    @DeleteMapping("/{serverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serverId) {
        serverService.deleteServer(serverId);
    }
}
