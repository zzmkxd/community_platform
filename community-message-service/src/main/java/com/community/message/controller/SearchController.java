package com.community.message.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/search")
@RequiredArgsConstructor
@Tag(name = "搜索")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResult<CursorPageBaseResp<MessageVO>> search(
            @PathVariable Long serverId,
            @RequestParam String q,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page) {
        return ApiResult.success(searchService.search(serverId, q, channelId, from, to, page));
    }
}
