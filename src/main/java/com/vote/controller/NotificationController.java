package com.vote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vote.common.Result;
import com.vote.entity.Notification;
import com.vote.entity.User;
import com.vote.service.NotificationService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserService userService;
    
    @GetMapping("/list")
    public Result<?> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        Page<Notification> pageResult = notificationService.getUserNotifications(user.getId(), page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        
        return Result.success(result);
    }
    
    @GetMapping("/unread")
    public Result<?> getUnreadNotifications(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        List<Notification> notifications = notificationService.getUnreadNotifications(user.getId());
        return Result.success(notifications);
    }
    
    @GetMapping("/unread/count")
    public Result<?> getUnreadCount(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        int count = notificationService.getUnreadCount(user.getId());
        return Result.success(count);
    }
    
    @PutMapping("/{notificationId}/read")
    public Result<?> markAsRead(@PathVariable Long notificationId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = notificationService.markAsRead(notificationId, user.getId());
        return ok ? Result.success("标记已读") : Result.error("操作失败");
    }
    
    @PutMapping("/read-all")
    public Result<?> markAllAsRead(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = notificationService.markAllAsRead(user.getId());
        return ok ? Result.success("全部已读") : Result.error("操作失败");
    }
    
    @DeleteMapping("/{notificationId}")
    public Result<?> deleteNotification(@PathVariable Long notificationId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = notificationService.deleteNotification(notificationId, user.getId());
        return ok ? Result.success("删除成功") : Result.error("操作失败");
    }
    
    @DeleteMapping("/clear-all")
    public Result<?> clearAllNotifications(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        int count = notificationService.deleteAllNotifications(user.getId());
        return Result.success("已清空" + count + "条消息");
    }
}
