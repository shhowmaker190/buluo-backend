package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.*;
import com.vote.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final CommentRestrictionMapper restrictionMapper;
    private final UserMapper userMapper;
    private final TribeService tribeService;

    @Transactional
    public void deletePost(Long postId) {
        int result = postMapper.deleteById(postId);
        if (result == 0) throw new RuntimeException("帖子不存在");
    }

    @Transactional
    public void restrictUserComment(Long userId, Long postId, Long adminId) {
        LambdaQueryWrapper<CommentRestriction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentRestriction::getUserId, userId).eq(CommentRestriction::getPostId, postId);
        if (restrictionMapper.selectCount(wrapper) == 0) {
            CommentRestriction restriction = new CommentRestriction();
            restriction.setUserId(userId);
            restriction.setPostId(postId);
            restriction.setAdminId(adminId);
            restriction.setCreatedAt(LocalDateTime.now());
            restrictionMapper.insert(restriction);
        }
    }

    @Transactional
    public void unrestrictUserComment(Long userId, Long postId) {
        LambdaQueryWrapper<CommentRestriction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentRestriction::getUserId, userId).eq(CommentRestriction::getPostId, postId);
        restrictionMapper.delete(wrapper);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, commentId));
        int result = commentMapper.deleteById(commentId);
        if (result == 0) throw new RuntimeException("评论不存在");
    }

    public List<User> getAllUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
        // 密码哈希绝不返回给前端（此前该接口把整个实体直接序列化了）
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    // ---------- 部落审核 ----------

    public List<Tribe> getPendingTribes() {
        return tribeService.pendingTribes();
    }

    public List<Tribe> getAllTribes() {
        return tribeService.listApproved(null, null);
    }

    @Transactional
    public boolean approveTribe(Long tribeId) {
        return tribeService.approve(tribeId);
    }

    @Transactional
    public boolean rejectTribe(Long tribeId, String reason) {
        return tribeService.reject(tribeId, reason);
    }

    public List<Tribe> getPendingTribeEdits() {
        return tribeService.pendingEdits();
    }

    @Transactional
    public boolean approveTribeEdit(Long tribeId) {
        return tribeService.approveEdit(tribeId);
    }

    @Transactional
    public boolean rejectTribeEdit(Long tribeId, String reason) {
        return tribeService.rejectEdit(tribeId, reason);
    }
}
