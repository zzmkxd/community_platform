package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/search")
@RequiredArgsConstructor
@Tag(name = "搜索")
@Validated
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "搜索服务器内消息（支持按频道、时间范围、分页过滤）")
    @GetMapping
    public ApiResult<CursorPageBaseResp<MessageVO>> search(
            @PathVariable Long serverId,
            @NotBlank @RequestParam String q,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page) {
        return ApiResult.success(searchService.search(serverId, q, channelId, from, to, page));
    }
}
