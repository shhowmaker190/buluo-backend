package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.Friend;
import com.vote.entity.FriendRequest;
import com.vote.entity.User;
import com.vote.mapper.FriendMapper;
import com.vote.mapper.FriendRequestMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 好友：申请 → 对方同意 → 双向建立关系；删除好友时两条记录一起删 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final FriendRequestMapper requestMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    public boolean isFriend(Long userId, Long otherId) {
        if (userId == null || otherId == null) return false;
        return friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, otherId)) > 0;
    }

    /** 我是否已向对方发出待处理申请（用于搜索结果里显示"已申请"） */
    public boolean hasPendingRequest(Long fromUserId, Long toUserId) {
        return requestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUserId)
                .eq(FriendRequest::getStatus, 0)) > 0;
    }

    /** 发送好友申请；已是好友、重复申请、对方已申请过都会给出明确提示 */
    @Transactional
    public void sendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) throw new RuntimeException("不能添加自己为好友");
        User to = userMapper.selectById(toUserId);
        if (to == null) throw new RuntimeException("用户不存在");
        if (isFriend(fromUserId, toUserId)) throw new RuntimeException("你们已经是好友了");

        Long pending = requestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUserId)
                .eq(FriendRequest::getStatus, 0));
        if (pending > 0) throw new RuntimeException("申请已发送，请等待对方处理");

        Long reversePending = requestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, toUserId)
                .eq(FriendRequest::getToUserId, fromUserId)
                .eq(FriendRequest::getStatus, 0));
        if (reversePending > 0) throw new RuntimeException("对方已向你发送过申请，请去处理");

        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setMessage(message == null || message.trim().isEmpty() ? null : message.trim());
        request.setStatus(0);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        requestMapper.insert(request);

        User from = userMapper.selectById(fromUserId);
        String fromName = from == null ? "有人" : from.getUsername();
        notificationService.create(toUserId, "FRIEND_REQUEST", "新的好友申请",
                fromName + " 想添加你为好友", fromName, "FRIEND", request.getId());
        sseEmitterService.send(toUserId, "friend-request", Map.of(
                "requestId", request.getId(),
                "fromUserId", fromUserId,
                "fromUsername", fromName));
    }

    /** 处理申请：同意则双向建好友关系，并给申请人回一条通知 */
    @Transactional
    public void handleRequest(Long requestId, Long userId, boolean accept) {
        FriendRequest request = requestMapper.selectById(requestId);
        if (request == null) throw new RuntimeException("申请不存在");
        if (!request.getToUserId().equals(userId)) throw new RuntimeException("无权处理该申请");
        if (request.getStatus() != 0) throw new RuntimeException("该申请已处理");

        request.setStatus(accept ? 1 : 2);
        request.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(request);

        User me = userMapper.selectById(userId);
        String myName = me == null ? "对方" : me.getUsername();
        if (accept) {
            addFriendPair(request.getFromUserId(), request.getToUserId());
            notificationService.create(request.getFromUserId(), "FRIEND_ACCEPT", "好友申请已通过",
                    myName + " 同意了你的好友申请", myName, "FRIEND", request.getFromUserId());
            sseEmitterService.send(request.getFromUserId(), "friend-accept", Map.of(
                    "userId", userId, "username", myName));
        } else {
            notificationService.create(request.getFromUserId(), "FRIEND_REJECT", "好友申请被拒绝",
                    myName + " 拒绝了你的好友申请", myName, "FRIEND", request.getFromUserId());
        }
    }

    private void addFriendPair(Long a, Long b) {
        if (isFriend(a, b)) return;
        LocalDateTime now = LocalDateTime.now();
        Friend f1 = new Friend();
        f1.setUserId(a);
        f1.setFriendId(b);
        f1.setCreatedAt(now);
        Friend f2 = new Friend();
        f2.setUserId(b);
        f2.setFriendId(a);
        f2.setCreatedAt(now);
        friendMapper.insert(f1);
        friendMapper.insert(f2);
    }

    /** 好友列表：带用户名、头像，按添加时间倒序 */
    public List<Map<String, Object>> friends(Long userId) {
        List<Friend> list = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .orderByDesc(Friend::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Friend f : list) {
            User u = userMapper.selectById(f.getFriendId());
            if (u == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("userId", u.getId());
            m.put("username", u.getUsername());
            m.put("avatar", u.getAvatar());
            m.put("since", f.getCreatedAt());
            result.add(m);
        }
        return result;
    }

    /** 我收到的待处理申请 */
    public List<Map<String, Object>> pendingRequests(Long userId) {
        List<FriendRequest> list = requestMapper.selectList(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getToUserId, userId)
                .eq(FriendRequest::getStatus, 0)
                .orderByDesc(FriendRequest::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRequest r : list) {
            User u = userMapper.selectById(r.getFromUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getFromUserId());
            m.put("username", u == null ? "未知用户" : u.getUsername());
            m.put("avatar", u == null ? null : u.getAvatar());
            m.put("message", r.getMessage());
            m.put("createdAt", r.getCreatedAt());
            result.add(m);
        }
        return result;
    }

    /** 我发出的待处理申请（用于按钮显示"已申请"） */
    public List<Map<String, Object>> sentRequests(Long userId) {
        List<FriendRequest> list = requestMapper.selectList(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, userId)
                .eq(FriendRequest::getStatus, 0)
                .orderByDesc(FriendRequest::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRequest r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getToUserId());
            result.add(m);
        }
        return result;
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
    }
}
