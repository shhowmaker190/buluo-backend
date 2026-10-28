package com.vote.controller;

import com.vote.common.Result;
import com.vote.entity.User;
import com.vote.service.FriendService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    /** 我的好友列表 */
    @GetMapping("/list")
    public Result<?> list(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(friendService.friends(me.getId()));
    }

    /** 我收到的待处理申请 */
    @GetMapping("/requests")
    public Result<?> requests(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(friendService.pendingRequests(me.getId()));
    }

    /** 我发出的待处理申请（前端用于显示"申请中"） */
    @GetMapping("/sent")
    public Result<?> sent(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(friendService.sentRequests(me.getId()));
    }

    /** 与某人的关系：是否好友 */
    @GetMapping("/check/{userId}")
    public Result<?> check(@PathVariable Long userId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("isFriend", friendService.isFriend(me.getId(), userId));
        return Result.success(data);
    }

    @PostMapping("/request/{userId}")
    public Result<?> sendRequest(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body,
                                 Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        try {
            friendService.sendRequest(me.getId(), userId, body == null ? null : body.get("message"));
            return Result.success("申请已发送", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/handle/{requestId}")
    public Result<?> handle(@PathVariable Long requestId, @RequestBody Map<String, Object> body,
                            Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        try {
            friendService.handleRequest(requestId, me.getId(), accept);
            return Result.success(accept ? "已添加好友" : "已拒绝申请", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public Result<?> remove(@PathVariable Long userId, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        friendService.removeFriend(me.getId(), userId);
        return Result.success("已删除好友", null);
    }
}
