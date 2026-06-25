package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.CategoryVO;
import com.community.server.service.ChannelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/categories")
@RequiredArgsConstructor
@Tag(name = "分类")
public class CategoryController {

    private final ChannelService channelService;

    @PostMapping
    public ApiResult<CategoryVO> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        return ApiResult.success(channelService.createCategory(serverId, name, sortOrder));
    }

    @PutMapping("/{id}")
    public ApiResult<CategoryVO> update(@PathVariable Long serverId, @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        return ApiResult.success(channelService.updateCategory(serverId, id, body.get("name")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serverId, @PathVariable Long id) {
        channelService.deleteCategory(serverId, id);
    }
}
