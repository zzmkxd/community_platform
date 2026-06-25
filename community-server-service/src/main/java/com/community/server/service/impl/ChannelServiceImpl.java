package com.community.server.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.websocket.service.PushService;
import com.community.server.dao.CategoryDao;
import com.community.server.dao.ChannelDao;
import com.community.server.dao.MemberDao;
import com.community.server.domain.entity.Category;
import com.community.server.domain.entity.Channel;
import com.community.server.domain.entity.ServerMember;
import com.community.server.domain.vo.CategoryVO;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import com.community.server.service.MembershipValidator;
import com.community.websocket.service.adapter.WSAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/channel")
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final CategoryDao categoryDao;
    private final ChannelDao channelDao;
    private final MemberDao memberDao;
    private final PushService pushService;

    // ==================== Category ====================

    @Override
    @Transactional
    @PostMapping("/category")
    public CategoryVO createCategory(@RequestParam("serverId") Long serverId,
                                      @RequestParam("name") String name,
                                      @RequestParam("sortOrder") Integer sortOrder) {
        MembershipValidator.requireMember(memberDao, serverId);

        Category category = new Category();
        category.setServerId(serverId);
        category.setName(name);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
        categoryDao.save(category);

        log.info("Category created: id={}, name={}, serverId={}", category.getId(), name, serverId);
        return toCategoryVO(category, Collections.emptyList());
    }

    @Override
    @Transactional
    @PutMapping("/category/{categoryId}")
    public CategoryVO updateCategory(@RequestParam("serverId") Long serverId,
                                      @PathVariable("categoryId") Long categoryId,
                                      @RequestParam("name") String name) {
        MembershipValidator.requireMember(memberDao, serverId);

        Category category = categoryDao.lambdaQuery()
                .eq(Category::getId, categoryId)
                .eq(Category::getServerId, serverId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CATEGORY_NOT_FOUND));

        if (name != null) {
            category.setName(name);
        }
        categoryDao.updateById(category);

        List<ChannelVO> channels = getChannelsByCategory(categoryId);
        return toCategoryVO(category, channels);
    }

    @Override
    @Transactional
    @DeleteMapping("/category/{categoryId}")
    public void deleteCategory(@RequestParam("serverId") Long serverId,
                               @PathVariable("categoryId") Long categoryId) {
        MembershipValidator.requireMember(memberDao, serverId);

        Category category = categoryDao.lambdaQuery()
                .eq(Category::getId, categoryId)
                .eq(Category::getServerId, serverId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CATEGORY_NOT_FOUND));

        long channelCount = channelDao.lambdaQuery()
                .eq(Channel::getCategoryId, categoryId)
                .eq(Channel::getStatus, 1)
                .count();

        if (channelCount > 0) {
            channelDao.lambdaUpdate()
                    .eq(Channel::getCategoryId, categoryId)
                    .set(Channel::getCategoryId, null)
                    .update();
        }

        categoryDao.removeById(categoryId);
        log.info("Category deleted: id={}, name={}, serverId={}", categoryId, category.getName(), serverId);
    }

    // ==================== Channel ====================

    @Override
    @Transactional
    @PostMapping
    public ChannelVO createChannel(@RequestParam("serverId") Long serverId,
                                    @RequestParam("categoryId") Long categoryId,
                                    @RequestParam("name") String name,
                                    @RequestParam("type") Integer type,
                                    @RequestParam("topic") String topic) {
        MembershipValidator.requireMember(memberDao, serverId);

        Channel channel = new Channel();
        channel.setServerId(serverId);
        channel.setCategoryId(categoryId);
        channel.setName(name);
        channel.setType(type != null ? type : 0);
        channel.setTopic(topic);
        channel.setSortOrder(0);
        channel.setStatus(1);
        channelDao.save(channel);

        log.info("Channel created: id={}, name={}, serverId={}", channel.getId(), name, serverId);
        pushService.pushToServer(serverId, null, WSAdapter.buildChannelCreate(toChannelVO(channel)));
        return toChannelVO(channel);
    }

    @Override
    @GetMapping("/server/{serverId}")
    public List<CategoryVO> getChannels(@PathVariable("serverId") Long serverId) {
        MembershipValidator.requireMember(memberDao, serverId);

        List<Category> categories = categoryDao.lambdaQuery()
                .eq(Category::getServerId, serverId)
                .orderByAsc(Category::getSortOrder)
                .list();

        List<Channel> channels = channelDao.lambdaQuery()
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getSortOrder)
                .list();

        Map<Long, List<ChannelVO>> groupedChannels = new LinkedHashMap<>();
        for (Channel ch : channels) {
            Long catId = ch.getCategoryId() != null ? ch.getCategoryId() : 0L;
            groupedChannels.computeIfAbsent(catId, k -> new ArrayList<>()).add(toChannelVO(ch));
        }

        List<CategoryVO> result = new ArrayList<>();
        for (Category cat : categories) {
            List<ChannelVO> catChannels = groupedChannels.getOrDefault(cat.getId(), Collections.emptyList());
            result.add(toCategoryVO(cat, catChannels));
        }

        List<ChannelVO> uncategorized = groupedChannels.getOrDefault(0L, Collections.emptyList());
        if (!uncategorized.isEmpty()) {
            CategoryVO uncatVO = new CategoryVO();
            uncatVO.setId(null);
            uncatVO.setName("未分类");
            uncatVO.setSortOrder(Integer.MAX_VALUE);
            uncatVO.setChannels(uncategorized);
            result.add(uncatVO);
        }

        return result;
    }

    @Override
    @GetMapping("/server/{serverId}/channel/{channelId}")
    public ChannelVO getChannel(@PathVariable("serverId") Long serverId,
                                 @PathVariable("channelId") Long channelId) {
        MembershipValidator.requireMember(memberDao, serverId);

        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        return toChannelVO(channel);
    }

    @Override
    @GetMapping("/{channelId}")
    public ChannelVO getById(@PathVariable("channelId") Long channelId) {
        Channel channel = channelDao.getById(channelId);
        if (channel == null || channel.getStatus() != 1) {
            return null;
        }
        return toChannelVO(channel);
    }

    @Override
    @GetMapping("/server/{serverId}/list")
    public List<ChannelVO> listByServerId(@PathVariable("serverId") Long serverId) {
        return channelDao.lambdaQuery()
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getSortOrder)
                .list()
                .stream()
                .map(this::toChannelVO)
                .toList();
    }

    @Override
    @Transactional
    @PutMapping("/{channelId}")
    public ChannelVO updateChannel(@RequestParam("serverId") Long serverId,
                                    @PathVariable("channelId") Long channelId,
                                    @RequestParam("name") String name,
                                    @RequestParam("topic") String topic) {
        MembershipValidator.requireMember(memberDao, serverId);

        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        if (name != null) {
            channel.setName(name);
        }
        if (topic != null) {
            channel.setTopic(topic);
        }
        channelDao.updateById(channel);

        pushService.pushToServer(serverId, null, WSAdapter.buildChannelUpdate(toChannelVO(channel)));
        return toChannelVO(channel);
    }

    @Override
    @Transactional
    @DeleteMapping("/{channelId}")
    public void deleteChannel(@RequestParam("serverId") Long serverId,
                              @PathVariable("channelId") Long channelId) {
        MembershipValidator.requireMember(memberDao, serverId);

        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        channel.setStatus(0);
        channelDao.updateById(channel);
        log.info("Channel soft-deleted: id={}, name={}, serverId={}", channelId, channel.getName(), serverId);
        pushService.pushToServer(serverId, null, WSAdapter.buildChannelDelete(channelId));
    }

    // ==================== private helpers ====================

    private List<ChannelVO> getChannelsByCategory(Long categoryId) {
        return channelDao.lambdaQuery()
                .eq(Channel::getCategoryId, categoryId)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getSortOrder)
                .list()
                .stream()
                .map(this::toChannelVO)
                .toList();
    }

    private CategoryVO toCategoryVO(Category category, List<ChannelVO> channels) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSortOrder(category.getSortOrder());
        vo.setChannels(channels);
        return vo;
    }

    private ChannelVO toChannelVO(Channel channel) {
        ChannelVO vo = new ChannelVO();
        vo.setId(channel.getId());
        vo.setServerId(channel.getServerId());
        vo.setName(channel.getName());
        vo.setType(channel.getType());
        vo.setTopic(channel.getTopic());
        vo.setSortOrder(channel.getSortOrder());
        return vo;
    }
}
