package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.message.domain.vo.ReactionVO;
import com.community.message.service.ReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages/{msgId}/reactions")
@RequiredArgsConstructor
@Tag(name = "Reaction")
@Validated
public class ReactionController {

    private final ReactionService reactionService;

    @Operation(summary = "为消息添加 Reaction（emoji），再次请求则移除")
    @PostMapping
    public ApiResult<List<ReactionVO>> add(@PathVariable Long msgId, @NotBlank @RequestParam String emoji) {
        return ApiResult.success(reactionService.addReaction(msgId, emoji));
    }

    @Operation(summary = "移除消息上的 Reaction")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long msgId, @NotBlank @RequestParam String emoji) {
        reactionService.removeReaction(msgId, emoji);
    }

    @Operation(summary = "获取消息的所有 Reaction（按 emoji 聚合，含 reacted 标记）")
    @GetMapping
    public ApiResult<List<ReactionVO>> getReactions(@PathVariable Long msgId) {
        return ApiResult.success(reactionService.getReactions(msgId));
    }
}
