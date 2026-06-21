package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.message.domain.vo.ReactionVO;
import com.community.message.service.ReactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages/{msgId}/reactions")
@RequiredArgsConstructor
@Tag(name = "Reaction")
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    public ApiResult<List<ReactionVO>> toggle(@PathVariable Long msgId, @RequestParam String emoji) {
        return ApiResult.success(reactionService.toggleReaction(msgId, emoji));
    }

    @DeleteMapping
    public ApiResult<List<ReactionVO>> remove(@PathVariable Long msgId, @RequestParam String emoji) {
        return ApiResult.success(reactionService.toggleReaction(msgId, emoji));
    }

    @GetMapping
    public ApiResult<List<ReactionVO>> getReactions(@PathVariable Long msgId) {
        return ApiResult.success(reactionService.getReactions(msgId));
    }
}
