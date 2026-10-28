package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vote.entity.Notification;
import com.vote.entity.User;
import com.vote.mapper.NotificationMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    
    /**
     * 通用通知创建：帖子之外的业务（私聊、好友申请）走这里，
     * 用 bizType + bizId 告诉前端该往哪跳：POST→帖子详情，CHAT→聊天会话，FRIEND→好友请求
     */
    @Transactional
    public void create(Long targetUserId, String type, String title, String content,
                       String fromUsername, String bizType, Long bizId) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            log.warn("通知接收者不存在，跳过通知: {}", targetUserId);
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setFromUsername(fromUsername);
        notification.setIsRead(false);
        notification.setEmailSent(false);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 给所有启用中的管理员推送审核提醒。
     * bizType 决定前端点击后跳到后台哪个 tab：
     * ADMIN_POST / ADMIN_TRIBE / ADMIN_EDIT / ADMIN_RENAME / ADMIN_REPORT
     */
    @Transactional
    public void notifyAdmins(String title, String content, String fromUsername, String bizType, Long bizId) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN")
                .eq(User::getStatus, 1));
        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setUserId(admin.getId());
            notification.setType("ADMIN_REVIEW");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setFromUsername(fromUsername);
            notification.setIsRead(false);
            notification.setEmailSent(false);
            notification.setBizType(bizType);
            notification.setBizId(bizId);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);
        }
        log.info("审核提醒已推送给 {} 位管理员: {}", admins.size(), title);
    }

    @Transactional
    public void createCommentNotification(Long targetUserId, Long postId, Long commentId, String postTitle,
                                          String commentContent, String fromUsername, boolean isReply) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            log.warn("通知接收者不存在，跳过通知");
            return;
        }
        
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setPostId(postId);
        notification.setCommentId(commentId);
        notification.setType("COMMENT");
        notification.setTitle(isReply ? "有人回复了您" : "有人评论了您的帖子");
        notification.setContent(isReply
                ? fromUsername + " 回复了您在《" + postTitle + "》中的评论"
                : fromUsername + " 评论了您的帖子《" + postTitle + "》");
        notification.setFromUsername(fromUsername);
        notification.setIsRead(false);
        notification.setEmailSent(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        log.info("创建评论通知成功，帖子ID: {}, 接收者: {}", postId, targetUserId);
    }
    
    @Transactional
    public void createLikeNotification(Long commentOwnerId, Long postId, String postTitle, 
                                       String commentContent, String fromUsername) {
        User commentOwner = userMapper.selectById(commentOwnerId);
        if (commentOwner == null) {
            log.warn("评论作者不存在，跳过通知");
            return;
        }
        
        Notification notification = new Notification();
        notification.setUserId(commentOwnerId);
        notification.setPostId(postId);
        notification.setType("LIKE");
        notification.setTitle("有人赞了您的评论");
        notification.setContent(fromUsername + " 赞了您在《" + postTitle + "》中的评论");
        notification.setFromUsername(fromUsername);
        notification.setIsRead(false);
        notification.setEmailSent(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        log.info("创建点赞通知成功，帖子ID: {}, 接收者: {}", postId, commentOwnerId);
    }
    
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Page<Notification> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(pageObj, wrapper);
    }
    
    public List<Notification> getUnreadNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false)
               .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectList(wrapper);
    }
    
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false);
        return Math.toIntExact(notificationMapper.selectCount(wrapper));
    }
    
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return false;
        }
        notification.setIsRead(true);
        return notificationMapper.updateById(notification) > 0;
    }
    
    @Transactional
    public boolean markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        if (unreadNotifications.isEmpty()) {
            return true;
        }
        unreadNotifications.forEach(n -> n.setIsRead(true));
        unreadNotifications.forEach(n -> notificationMapper.updateById(n));
        return true;
    }
    
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return false;
        }
        return notificationMapper.deleteById(notificationId) > 0;
    }
    
    @Transactional
    public int deleteAllNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        return notificationMapper.delete(wrapper);
    }
}
