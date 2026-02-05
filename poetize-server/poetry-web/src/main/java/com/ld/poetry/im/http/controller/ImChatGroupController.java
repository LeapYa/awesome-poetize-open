package com.ld.poetry.im.http.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.aop.SaveCheck;
import com.ld.poetry.entity.User;
import com.ld.poetry.im.http.entity.ImChatGroup;
import com.ld.poetry.im.http.entity.ImChatGroupUser;
import com.ld.poetry.im.http.entity.ImChatUserGroupMessage;
import com.ld.poetry.im.http.service.ImChatGroupService;
import com.ld.poetry.im.http.service.ImChatGroupUserService;
import com.ld.poetry.im.http.service.ImChatUserGroupMessageService;
import com.ld.poetry.im.http.service.ImChatLastReadService;
import com.ld.poetry.im.http.vo.GroupVO;
import com.ld.poetry.im.websocket.ImConfigConst;
import com.ld.poetry.im.websocket.ImSessionManager;
import com.ld.poetry.enums.CodeMsg;
import com.ld.poetry.enums.PoetryEnum;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 聊天群 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-12-02
 */
@Slf4j
@RestController
@RequestMapping("/imChatGroup")
public class ImChatGroupController {

    @Autowired
    private ImChatGroupService imChatGroupService;

    @Autowired
    private ImChatGroupUserService imChatGroupUserService;

    @Autowired
    private ImChatUserGroupMessageService imChatUserGroupMessageService;

    @Autowired
    private ImChatLastReadService imChatLastReadService;

    @Autowired(required = false)
    private ImSessionManager imSessionManager;

    /**
     * 创建群组
     */
    @PostMapping("/creatGroupCommon")
    @LoginCheck
    @SaveCheck
    public PoetryResult creatGroup(@RequestBody ImChatGroup imChatGroup) {
        if (!StringUtils.hasText(imChatGroup.getGroupName())) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        imChatGroup.setGroupType(ImConfigConst.GROUP_COMMON);
        Integer userId = PoetryUtil.getUserId();
        imChatGroup.setMasterUserId(userId);
        imChatGroupService.save(imChatGroup);

        ImChatGroupUser imChatGroupUser = new ImChatGroupUser();
        imChatGroupUser.setGroupId(imChatGroup.getId());
        imChatGroupUser.setUserId(userId);
        imChatGroupUser.setAdminFlag(ImConfigConst.ADMIN_FLAG_TRUE);
        imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        imChatGroupUserService.save(imChatGroupUser);

        // 注：群组绑定由前端重新建立WebSocket连接时自动处理

        return PoetryResult.success();
    }

    /**
     * 创建话题
     */
    @PostMapping("/creatGroupTopic")
    @LoginCheck(0)
    public PoetryResult creatGroupTopic(@RequestBody ImChatGroup imChatGroup) {
        if (!StringUtils.hasText(imChatGroup.getGroupName())) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        imChatGroup.setGroupType(ImConfigConst.GROUP_TOPIC);
        Integer userId = PoetryUtil.getUserId();
        imChatGroup.setMasterUserId(userId);
        imChatGroup.setInType(ImConfigConst.IN_TYPE_FALSE);
        imChatGroupService.save(imChatGroup);

        return PoetryResult.success();
    }

    /**
     * 更新组
     * <p>
     * 只有群主才能修改组
     */
    @PostMapping("/updateGroup")
    @LoginCheck
    public PoetryResult updateGroup(@RequestBody ImChatGroup imChatGroup) {
        LambdaUpdateChainWrapper<ImChatGroup> lambdaUpdate = imChatGroupService.lambdaUpdate();
        lambdaUpdate.eq(ImChatGroup::getId, imChatGroup.getId());
        lambdaUpdate.eq(ImChatGroup::getMasterUserId, PoetryUtil.getUserId());
        if (StringUtils.hasText(imChatGroup.getGroupName())) {
            lambdaUpdate.set(ImChatGroup::getGroupName, imChatGroup.getGroupName());
        }
        if (StringUtils.hasText(imChatGroup.getAvatar())) {
            lambdaUpdate.set(ImChatGroup::getAvatar, imChatGroup.getAvatar());
        }
        if (StringUtils.hasText(imChatGroup.getIntroduction())) {
            lambdaUpdate.set(ImChatGroup::getIntroduction, imChatGroup.getIntroduction());
        }
        // 群通知
        if (StringUtils.hasText(imChatGroup.getNotice())) {
            lambdaUpdate.set(ImChatGroup::getNotice, imChatGroup.getNotice());
        }
        // 修改进入方式
        if (imChatGroup.getInType() != null) {
            lambdaUpdate.set(ImChatGroup::getInType, imChatGroup.getInType());
        }
        // 转让群
        if (imChatGroup.getMasterUserId() != null) {
            lambdaUpdate.set(ImChatGroup::getMasterUserId, imChatGroup.getMasterUserId());
        }
        boolean isSuccess = lambdaUpdate.update();
        if (isSuccess && StringUtils.hasText(imChatGroup.getNotice())) {
            // todo 发送群公告
        }
        return PoetryResult.success();
    }

    /**
     * 解散群
     */
    @GetMapping("/deleteGroup")
    @LoginCheck
    public PoetryResult deleteGroup(@RequestParam("id") Integer id) {
        User currentUser = PoetryUtil.getCurrentUser();
        boolean isSuccess;
        if (currentUser.getUserType().intValue() == PoetryEnum.USER_TYPE_ADMIN.getCode()) {
            isSuccess = imChatGroupService.removeById(id);
        } else {
            LambdaUpdateChainWrapper<ImChatGroup> lambdaUpdate = imChatGroupService.lambdaUpdate();
            lambdaUpdate.eq(ImChatGroup::getId, id);
            lambdaUpdate.eq(ImChatGroup::getMasterUserId, PoetryUtil.getUserId());
            isSuccess = lambdaUpdate.remove();
        }
        if (isSuccess) {
            // 删除用户
            LambdaUpdateChainWrapper<ImChatGroupUser> lambdaUpdate = imChatGroupUserService.lambdaUpdate();
            lambdaUpdate.eq(ImChatGroupUser::getGroupId, id).remove();
            // 删除聊天记录
            LambdaUpdateChainWrapper<ImChatUserGroupMessage> messageLambdaUpdateChainWrapper = imChatUserGroupMessageService
                    .lambdaUpdate();
            messageLambdaUpdateChainWrapper.eq(ImChatUserGroupMessage::getGroupId, id).remove();

            // 注：群组解绑由前端重新建立WebSocket连接时自动处理
        }
        return PoetryResult.success();
    }

    /**
     * 管理员查询所有群
     */
    @PostMapping("/listGroupForAdmin")
    @LoginCheck(0)
    public PoetryResult<Page> listGroupForAdmin(@RequestBody BaseRequestVO baseRequestVO) {
        Page<ImChatGroup> page = new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize());
        LambdaQueryChainWrapper<ImChatGroup> lambdaQuery = imChatGroupService.lambdaQuery();
        Page<ImChatGroup> result = lambdaQuery.orderByDesc(ImChatGroup::getCreateTime).page(page);
        return PoetryResult.success(result);
    }

    /**
     * 加入话题
     */
    @GetMapping("/addGroupTopic")
    @LoginCheck
    public PoetryResult addGroupTopic(@RequestParam("id") Integer id) {
        LambdaQueryChainWrapper<ImChatGroup> lambdaQuery = imChatGroupService.lambdaQuery();
        Long count = lambdaQuery.eq(ImChatGroup::getId, id)
                .eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_TOPIC).count();
        if (count.intValue() == 1) {
            // 注：话题绑定由前端在WebSocket连接后处理
            // 前端会自动将话题加入到当前会话的群组中
        }
        return PoetryResult.success();
    }

    /**
     * 用户查询所有群
     * <p>
     * 只查询审核通过和禁言的群
     */
    @GetMapping("/listGroup")
    @LoginCheck
    public PoetryResult<List<GroupVO>> listGroup() {
        Integer userId = PoetryUtil.getUserId();
        LambdaQueryChainWrapper<ImChatGroupUser> lambdaQuery = imChatGroupUserService.lambdaQuery();
        lambdaQuery.eq(ImChatGroupUser::getUserId, userId);
        lambdaQuery.in(ImChatGroupUser::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS,
                ImConfigConst.GROUP_USER_STATUS_SILENCE);
        List<ImChatGroupUser> groupUsers = lambdaQuery.list();

        Map<Integer, ImChatGroupUser> groupUserMap = groupUsers.stream()
                .collect(Collectors.toMap(ImChatGroupUser::getGroupId, Function.identity()));
        LambdaQueryChainWrapper<ImChatGroup> wrapper = imChatGroupService.lambdaQuery();
        wrapper.eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_TOPIC);
        if (!CollectionUtils.isEmpty(groupUserMap.keySet())) {
            wrapper.or(w -> w.in(ImChatGroup::getId, groupUserMap.keySet())
                    .eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_COMMON));
        }
        List<ImChatGroup> imChatGroups = wrapper.list();
        List<GroupVO> groupVOS = imChatGroups.stream().map(imChatGroup -> {
            ImChatGroupUser imChatGroupUser = groupUserMap.get(imChatGroup.getId());
            if (imChatGroupUser == null) {
                imChatGroupUser = new ImChatGroupUser();
                imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
                imChatGroupUser.setCreateTime(LocalDateTime.now());
                imChatGroupUser.setUserId(userId);
                imChatGroupUser.setAdminFlag(userId.intValue() == PoetryUtil.getAdminUser().getId().intValue());
            }
            return getGroupVO(imChatGroup, imChatGroupUser);
        }).collect(Collectors.toList());
        return PoetryResult.success(groupVOS);
    }

    /**
     * 获取群组在线用户数
     */
    @GetMapping("/getOnlineCount")
    @LoginCheck
    public PoetryResult<Integer> getOnlineCount(@RequestParam("groupId") Integer groupId) {
        // 参数验证 (允许-1作为公共聊天室ID)
        if (groupId == null) {
            log.warn("获取在线用户数请求参数无效: groupId={}", groupId);
            return PoetryResult.fail("无效的群组ID");
        }

        try {
            if (imSessionManager == null) {
                log.warn("WebSocket服务未启动，groupId: {}", groupId);
                return PoetryResult.success(0);
            }

            // 获取群组在线用户数
            int onlineCount = imSessionManager.getGroupOnlineCount(String.valueOf(groupId));

            return PoetryResult.success(Math.max(0, onlineCount));
        } catch (Exception e) {
            log.error("获取群组在线用户数失败，groupId: {}", groupId, e);
            return PoetryResult.fail("服务器内部错误，请稍后重试");
        }
    }

    /**
     * 获取所有群的未读消息数
     */
    @GetMapping("/getGroupUnreadCounts")
    @LoginCheck
    public PoetryResult<Map<Integer, Integer>> getGroupUnreadCounts() {
        try {
            Integer userId = PoetryUtil.getUserId();
            Map<Integer, Integer> unreadCounts = imChatLastReadService.getGroupUnreadCounts(userId);
            return PoetryResult.success(unreadCounts);
        } catch (Exception e) {
            log.error("获取群聊未读数失败", e);
            return PoetryResult.fail("获取未读消息数失败");
        }
    }

    /**
     * 标记群消息为已读
     */
    @PostMapping("/markGroupAsRead")
    @LoginCheck
    public PoetryResult markGroupAsRead(@RequestParam("groupId") Integer groupId) {
        try {
            Integer userId = PoetryUtil.getUserId();
            imChatLastReadService.markGroupAsRead(userId, groupId);
            return PoetryResult.success();
        } catch (Exception e) {
            log.error("标记群消息为已读失败 - userId: {}, groupId: {}", PoetryUtil.getUserId(), groupId, e);
            return PoetryResult.fail("标记已读失败");
        }
    }

    /**
     * 获取所有好友的未读消息数
     */
    @GetMapping("/getFriendUnreadCounts")
    @LoginCheck
    public PoetryResult<Map<Integer, Integer>> getFriendUnreadCounts() {
        try {
            Integer userId = PoetryUtil.getUserId();
            Map<Integer, Integer> unreadCounts = imChatLastReadService.getFriendUnreadCounts(userId);
            return PoetryResult.success(unreadCounts);
        } catch (Exception e) {
            log.error("获取好友未读数失败", e);
            return PoetryResult.fail("获取未读消息数失败");
        }
    }

    /**
     * 标记好友消息为已读
     */
    @PostMapping("/markFriendAsRead")
    @LoginCheck
    public PoetryResult markFriendAsRead(@RequestParam("friendId") Integer friendId) {
        try {
            Integer userId = PoetryUtil.getUserId();
            imChatLastReadService.markFriendAsRead(userId, friendId);
            return PoetryResult.success();
        } catch (Exception e) {
            log.error("标记好友消息为已读失败 - userId: {}, friendId: {}", PoetryUtil.getUserId(), friendId, e);
            return PoetryResult.fail("标记已读失败");
        }
    }

    /**
     * 隐藏好友聊天（从列表中删除，但不删除好友）
     */
    @PostMapping("/hideFriendChat")
    @LoginCheck
    public PoetryResult hideFriendChat(@RequestParam("friendId") Integer friendId) {
        try {
            Integer userId = PoetryUtil.getUserId();
            imChatLastReadService.hideFriendChat(userId, friendId);
            return PoetryResult.success();
        } catch (Exception e) {
            log.error("隐藏好友聊天失败 - userId: {}, friendId: {}", PoetryUtil.getUserId(), friendId, e);
            return PoetryResult.fail("隐藏聊天失败");
        }
    }

    /**
     * 隐藏群聊（从列表中删除，但不退群）
     */
    @PostMapping("/hideGroupChat")
    @LoginCheck
    public PoetryResult hideGroupChat(@RequestParam("groupId") Integer groupId) {
        try {
            Integer userId = PoetryUtil.getUserId();
            imChatLastReadService.hideGroupChat(userId, groupId);
            return PoetryResult.success();
        } catch (Exception e) {
            log.error("隐藏群聊失败 - userId: {}, groupId: {}", PoetryUtil.getUserId(), groupId, e);
            return PoetryResult.fail("隐藏聊天失败");
        }
    }

    private GroupVO getGroupVO(ImChatGroup imChatGroup, ImChatGroupUser imChatGroupUser) {
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupName(imChatGroup.getGroupName());
        groupVO.setAvatar(imChatGroup.getAvatar());
        groupVO.setIntroduction(imChatGroup.getIntroduction());
        groupVO.setNotice(imChatGroup.getNotice());
        groupVO.setInType(imChatGroup.getInType());
        groupVO.setGroupType(imChatGroup.getGroupType());
        groupVO.setId(imChatGroup.getId());
        groupVO.setCreateTime(imChatGroupUser.getCreateTime());
        groupVO.setUserStatus(imChatGroupUser.getUserStatus());
        groupVO.setAdminFlag(imChatGroupUser.getAdminFlag());
        groupVO.setMasterFlag(imChatGroup.getMasterUserId().intValue() == imChatGroupUser.getUserId().intValue());
        return groupVO;
    }
}
