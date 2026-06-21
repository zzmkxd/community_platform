package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {

    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "分类下的频道列表")
    private List<ChannelVO> channels;
}
