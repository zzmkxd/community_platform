package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.EmojiVO;
import com.community.server.service.EmojiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/emojis")
@RequiredArgsConstructor
@Tag(name = "表情")
public class EmojiController {

    private final EmojiService emojiService;

    @PostMapping
    public ApiResult<EmojiVO> upload(@PathVariable Long serverId,
                                      @RequestParam String name,
                                      @RequestParam MultipartFile imageFile) throws IOException {
        return ApiResult.success(emojiService.uploadEmoji(serverId, name, imageFile.getBytes()));
    }

    @GetMapping
    public ApiResult<List<EmojiVO>> getEmojis(@PathVariable Long serverId) {
        return ApiResult.success(emojiService.getEmojis(serverId));
    }

    @DeleteMapping("/{emojiId}")
    public ApiResult<Void> delete(@PathVariable Long serverId, @PathVariable Long emojiId) {
        emojiService.deleteEmoji(serverId, emojiId);
        return ApiResult.success();
    }
}
