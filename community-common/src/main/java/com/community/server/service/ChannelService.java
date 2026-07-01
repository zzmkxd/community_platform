package com.community.server.service;

import com.community.server.domain.vo.CategoryVO;
import com.community.server.domain.vo.ChannelVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "community-server-service", contextId = "channelService", path = "/internal/channel")
public interface ChannelService {

    // ---- Category ----
    @PostMapping("/category")
    CategoryVO createCategory(@RequestParam("serverId") Long serverId,
                              @RequestParam("name") String name,
                              @RequestParam("sortOrder") Integer sortOrder);

    @PutMapping("/category/{categoryId}")
    CategoryVO updateCategory(@RequestParam("serverId") Long serverId,
                              @PathVariable("categoryId") Long categoryId,
                              @RequestParam("name") String name);

    @DeleteMapping("/category/{categoryId}")
    void deleteCategory(@RequestParam("serverId") Long serverId,
                        @PathVariable("categoryId") Long categoryId);

    // ---- Channel ----
    @PostMapping
    ChannelVO createChannel(@RequestParam("serverId") Long serverId,
                            @RequestParam("categoryId") Long categoryId,
                            @RequestParam("name") String name,
                            @RequestParam("type") Integer type,
                            @RequestParam("topic") String topic);

    @GetMapping("/server/{serverId}")
    List<CategoryVO> getChannels(@PathVariable("serverId") Long serverId);

    @GetMapping("/server/{serverId}/channel/{channelId}")
    ChannelVO getChannel(@PathVariable("serverId") Long serverId,
                         @PathVariable("channelId") Long channelId);

    /** 按频道 ID 查询（不校验 serverId） */
    @GetMapping("/{channelId}")
    ChannelVO getById(@PathVariable("channelId") Long channelId);

    /** 按服务器 ID 查询所有有效频道 */
    @GetMapping("/server/{serverId}/list")
    List<ChannelVO> listByServerId(@PathVariable("serverId") Long serverId);

    @PutMapping("/{channelId}")
    ChannelVO updateChannel(@RequestParam("serverId") Long serverId,
                            @PathVariable("channelId") Long channelId,
                            @RequestParam("name") String name,
                            @RequestParam("topic") String topic);

    @DeleteMapping("/{channelId}")
    void deleteChannel(@RequestParam("serverId") Long serverId,
                       @PathVariable("channelId") Long channelId);
}
