package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/messages")
@RequiredArgsConstructor
@Tag(name = "消息")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ApiResult<MessageVO> send(@PathVariable Long channelId, @RequestBody Map<String, Object> body) {
        Long threadId = body.get("threadId") != null ? ((Number) body.get("threadId")).longValue() : null;
        String content = (String) body.get("content");
        Integer msgType = body.get("msgType") != null ? ((Number) body.get("msgType")).intValue() : 1;
        Long replyMsgId = body.get("replyMsgId") != null ? ((Number) body.get("replyMsgId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Number> fileIdsRaw = (List<Number>) body.get("fileIds");
        List<Long> fileIds = fileIdsRaw != null ? fileIdsRaw.stream().map(Number::longValue).toList() : null;
        return ApiResult.success(messageService.sendMessage(channelId, threadId, content, msgType, replyMsgId, fileIds));
    }

    @GetMapping
    public ApiResult<CursorPageBaseResp<MessageVO>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(required = false) Long threadId) {
        return ApiResult.success(messageService.getMessages(channelId, threadId, cursor, pageSize));
    }

    @GetMapping("/{msgId}")
    public ApiResult<MessageVO> getMessage(@PathVariable Long channelId, @PathVariable Long msgId) {
        return ApiResult.success(messageService.getMessage(channelId, msgId));
    }

    @PutMapping("/{msgId}")
    public ApiResult<MessageVO> edit(@PathVariable Long channelId, @PathVariable Long msgId,
                                      @RequestBody Map<String, String> body) {
        return ApiResult.success(messageService.editMessage(channelId, msgId, body.get("content")));
    }

    @DeleteMapping("/{msgId}")
    public ApiResult<Void> delete(@PathVariable Long channelId, @PathVariable Long msgId) {
        messageService.deleteMessage(channelId, msgId);
        return ApiResult.success();
    }
}
