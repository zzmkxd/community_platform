package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ThreadVO;
import com.community.message.service.ThreadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "话题")
public class ThreadController {

    private final ThreadService threadService;

    @PostMapping("/channels/{channelId}/threads")
    public ApiResult<ThreadVO> create(@PathVariable Long channelId, @RequestBody Map<String, Object> body) {
        Long rootMsgId = ((Number) body.get("rootMsgId")).longValue();
        String name = (String) body.get("name");
        return ApiResult.success(threadService.createThread(channelId, rootMsgId, name));
    }

    @GetMapping("/channels/{channelId}/threads")
    public ApiResult<CursorPageBaseResp<ThreadVO>> getThreads(
            @PathVariable Long channelId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "30") Integer pageSize) {
        return ApiResult.success(threadService.getThreads(channelId, cursor, pageSize));
    }

    @GetMapping("/threads/{threadId}")
    public ApiResult<ThreadVO> getThread(@PathVariable Long threadId) {
        return ApiResult.success(threadService.getThread(threadId));
    }

    @GetMapping("/threads/{threadId}/messages")
    public ApiResult<CursorPageBaseResp<MessageVO>> getThreadMessages(
            @PathVariable Long threadId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return ApiResult.success(threadService.getThreadMessages(threadId, cursor, pageSize));
    }

    @PutMapping("/threads/{threadId}")
    public ApiResult<ThreadVO> update(@PathVariable Long threadId, @RequestBody Map<String, String> body) {
        return ApiResult.success(threadService.updateThread(threadId, body.get("name"), body.get("status")));
    }
}
