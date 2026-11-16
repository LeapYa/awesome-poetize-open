package com.ld.poetry.utils.mail;

import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Comment;
import com.ld.poetry.entity.ResourcePath;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.enums.CommentTypeEnum;
import com.ld.poetry.im.http.entity.ImChatUserMessage;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.CommentService;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.cache.UserCacheManager;
import com.ld.poetry.utils.RetryUtil;
import com.ld.poetry.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class MailSendUtil {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailUtil mailUtil;

    @Autowired
    private UserCacheManager userCacheManager;

    @Autowired
    private CacheService cacheService;

    public void sendCommentMail(CommentVO commentVO, Article one, CommentService commentService) {
        RetryUtil.executeWithRetryVoid(() -> {
            try {
                Integer currentUserId = PoetryUtil.getUserId();
                User adminUser = PoetryUtil.getAdminUser();
                sendCommentMailAsync(commentVO, one, commentService, currentUserId, adminUser);
            } catch (Exception e) {
                log.warn("获取用户信息失败，使用异步安全模式发送邮件: {}", e.getMessage());
                sendCommentMailAsync(commentVO, one, commentService, null, null);
            }
        });
    }

    /**
     * 异步安全的评论邮件发送方法
     * @param commentVO 评论信息
     * @param one 文章信息
     * @param commentService 评论服务
     * @param currentUserId 当前用户ID（可为null）
     * @param adminUser 管理员用户（可为null）
     */
    public void sendCommentMailAsync(CommentVO commentVO, Article one, CommentService commentService, Integer currentUserId, User adminUser) {
        List<String> mail = new ArrayList<>();
        String toName = "";
        if (commentVO.getParentUserId() != null) {
            User user = commonQuery.getUser(commentVO.getParentUserId());
            if (user != null && (currentUserId == null || !user.getId().equals(currentUserId)) && StringUtils.hasText(user.getEmail())) {
                toName = user.getUsername();
                mail.add(user.getEmail());
            }
        } else {
            if (CommentTypeEnum.COMMENT_TYPE_MESSAGE.getCode().equals(commentVO.getType()) ||
                    CommentTypeEnum.COMMENT_TYPE_LOVE.getCode().equals(commentVO.getType())) {
                if (adminUser != null && StringUtils.hasText(adminUser.getEmail()) && 
                    (currentUserId == null || !Objects.equals(currentUserId, adminUser.getId()))) {
                    mail.add(adminUser.getEmail());
                }
            } else if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentVO.getType())) {
                User user = commonQuery.getUser(one.getUserId());
                if (user != null && StringUtils.hasText(user.getEmail()) && 
                    (currentUserId == null || !user.getId().equals(currentUserId))) {
                    mail.add(user.getEmail());
                }
            }
        }

        if (!CollectionUtils.isEmpty(mail)) {
            String sourceName = "";
            if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentVO.getType())) {
                sourceName = one.getArticleTitle();
            }
            
            // 安全获取用户名
            String currentUsername = "匿名用户";
            try {
                currentUsername = PoetryUtil.getUsername();
            } catch (Exception e) {
            }
            
            String commentMail = getCommentMail(commentVO.getType(), sourceName,
                    currentUsername,
                    commentVO.getCommentContent(),
                    toName,
                    commentVO.getParentCommentId(), commentService);

            // 使用Redis缓存管理邮件发送频率控制
            String mailCountKey = CacheConstants.CACHE_PREFIX + "comment:mail:" + mail.get(0);
            try {
                Object cached = cacheService.get(mailCountKey);
                AtomicInteger count = null;
                if (cached instanceof AtomicInteger) {
                    count = (AtomicInteger) cached;
                } else if (cached instanceof Integer) {
                    count = new AtomicInteger((Integer) cached);
                }

                if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                    // 获取网站信息
                    WebInfo webInfo = cacheService.getCachedWebInfo();
                    String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());

                    mailUtil.sendMailMessage(mail, "您有一封来自" + webName + "的回执！", commentMail);

                    if (count == null) {
                        cacheService.set(mailCountKey, new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                    } else {
                        count.incrementAndGet();
                        cacheService.set(mailCountKey, count, CommonConst.CODE_EXPIRE);
                    }
                } else {
                }
            } catch (Exception e) {
                log.error("处理评论邮件发送频率控制时发生错误: email={}", mail.get(0), e);
            }
        }
    }

    /**
     * source：0留言 其他是文章标题
     * fromName：评论人
     * toName：被评论人
     */
    private String getCommentMail(String commentType, String source, String fromName, String fromContent, String toName, Integer toCommentId, CommentService commentService) {
        // 使用CacheService获取网站信息
        WebInfo webInfo = cacheService.getCachedWebInfo();
        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());

        String mailType = "";
        String toMail = "";
        if (StringUtils.hasText(toName)) {
            mailType = String.format(MailUtil.replyMail, fromName);
            Comment toComment = commentService.lambdaQuery().select(Comment::getCommentContent).eq(Comment::getId, toCommentId).one();
            if (toComment != null) {
                toMail = String.format(MailUtil.originalText, toName, toComment.getCommentContent());
            }
        } else {
            if (CommentTypeEnum.COMMENT_TYPE_MESSAGE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.messageMail, fromName);
            } else if (CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.commentMail, source, fromName);
            } else if (CommentTypeEnum.COMMENT_TYPE_LOVE.getCode().equals(commentType)) {
                mailType = String.format(MailUtil.loveMail, fromName);
            }
        }

        return String.format(mailUtil.getMailText(),
                webName,
                mailType,
                fromName,
                fromContent,
                toMail,
                webName);
    }

    public void sendImMail(ImChatUserMessage message) {
        // 使用重试机制发送IM邮件
        RetryUtil.executeWithRetryVoid(() -> {
            sendImMailAsync(message);
        });
    }

    /**
     * 异步安全的IM邮件发送方法
     * @param message IM消息
     */
    public void sendImMailAsync(ImChatUserMessage message) {
        if (!message.getMessageStatus()) {
            List<String> mail = new ArrayList<>();
            String username = "";
            User toUser = commonQuery.getUser(message.getToId());
            if (toUser != null && StringUtils.hasText(toUser.getEmail())) {
                mail.add(toUser.getEmail());
            }
            User fromUser = commonQuery.getUser(message.getFromId());
            if (fromUser != null) {
                username = fromUser.getUsername();
            }

            if (!CollectionUtils.isEmpty(mail)) {
                String commentMail = getImMail(username, message.getContent());

                // 使用Redis缓存管理IM邮件发送频率控制
                String mailCountKey = CacheConstants.CACHE_PREFIX + "im:mail:" + mail.get(0);
                try {
                    Object cached = cacheService.get(mailCountKey);
                    AtomicInteger count = null;
                    if (cached instanceof AtomicInteger) {
                        count = (AtomicInteger) cached;
                    } else if (cached instanceof Integer) {
                        count = new AtomicInteger((Integer) cached);
                    }

                    if (count == null || count.get() < CommonConst.COMMENT_IM_MAIL_COUNT) {
                        // 获取网站信息
                        WebInfo webInfo = cacheService.getCachedWebInfo();
                        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());

                        mailUtil.sendMailMessage(mail, "您有一封来自" + webName + "的回执！", commentMail);

                        if (count == null) {
                            cacheService.set(mailCountKey, new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                        } else {
                            count.incrementAndGet();
                            cacheService.set(mailCountKey, count, CommonConst.CODE_EXPIRE);
                        }
                    } else {
                    }
                } catch (Exception e) {
                    log.error("处理IM邮件发送频率控制时发生错误: email={}", mail.get(0), e);
                }
            }
        }
    }

    private String getImMail(String fromName, String fromContent) {
        // 使用CacheService获取网站信息
        WebInfo webInfo = cacheService.getCachedWebInfo();
        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());

        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, fromName),
                fromName,
                fromContent,
                "",
                webName);
    }

    public void sendFriendMail(ResourcePath friend) {
        User adminUser = PoetryUtil.getAdminUser();
        if (adminUser == null || !StringUtils.hasText(adminUser.getEmail())) {
            return;
        }
        if (Objects.equals(PoetryUtil.getUserId(), adminUser.getId())) {
            return;
        }

        String mailCountKey = CacheConstants.CACHE_PREFIX + "friend:mail:" + adminUser.getEmail();
        Object cached = cacheService.get(mailCountKey);
        int sentCount = 0;
        if (cached instanceof AtomicInteger) {
            sentCount = ((AtomicInteger) cached).get();
        } else if (cached instanceof Integer) {
            sentCount = (Integer) cached;
        }
        if (sentCount >= CommonConst.COMMENT_IM_MAIL_COUNT) {
            return;
        }

        WebInfo webInfo = cacheService.getCachedWebInfo();
        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());

        mailUtil.sendMailMessage(List.of(adminUser.getEmail()),
                "您有一封来自" + webName + "的回执！",
                getFriendMail(PoetryUtil.getUsername(), friend, webName));

        cacheService.set(mailCountKey, new AtomicInteger(sentCount + 1), CommonConst.CODE_EXPIRE);
    }

    private String getFriendMail(String fromName, ResourcePath friend, String webName) {
        String content = "网站名称：" + friend.getTitle() + "<br>"
                + "网站简介：" + friend.getIntroduction() + "<br>"
                + "网站地址：<a href=\"" + friend.getUrl() + "\" target=\"_blank\" rel=\"nofollow noopener\">"
                + friend.getUrl() + "</a><br>"
                + "请前往后台「资源聚合」审核。";

        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.friendMail, fromName),
                fromName,
                content,
                "",
                webName);
    }
}
