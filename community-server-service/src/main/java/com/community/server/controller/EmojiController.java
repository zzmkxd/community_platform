package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.EmojiVO;
import com.community.server.service.EmojiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/emojis")
@RequiredArgsConstructor
@Tag(name = "表情")
@Validated
public class EmojiController {

    private final EmojiService emojiService;

    @Operation(summary = "上传自定义表情（multipart/form-data）")
    @PostMapping
    public ApiResult<EmojiVO> upload(@PathVariable Long serverId,
                                      @NotBlank @RequestParam String name,
                                      @RequestParam MultipartFile imageFile) throws IOException {
        return ApiResult.success(emojiService.uploadEmoji(serverId, name, imageFile.getBytes()));
    }

    @Operation(summary = "获取服务器全部自定义表情")
    @GetMapping
    public ApiResult<List<EmojiVO>> getEmojis(@PathVariable Long serverId) {
        return ApiResult.success(emojiService.getEmojis(serverId));
    }

    @Operation(summary = "删除自定义表情")
    @DeleteMapping("/{emojiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serverId, @PathVariable Long emojiId) {
        emojiService.deleteEmoji(serverId, emojiId);
    }
}
