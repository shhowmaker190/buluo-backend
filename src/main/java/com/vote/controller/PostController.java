package com.vote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vote.common.Result;
import com.vote.dto.*;
import com.vote.entity.*;
import com.vote.service.PostService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserService userService;

    @PostMapping("/create")
    public Result<?> createPost(@RequestBody CreatePostDTO dto, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        try {
            postService.createPost(dto, user.getId());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
        return Result.success("帖子已提交，等待管理员审核", null);
    }

    @DeleteMapping("/{postId}")
    public Result<?> deletePost(@PathVariable Long postId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = postService.deletePost(postId, user.getId());
        return ok ? Result.success("删除成功", null) : Result.error("无权限");
    }

    @GetMapping("/list")
    public Result<?> getPostList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) Long tribeId,
                                 @RequestParam(defaultValue = "latest") String sort,
                                 @RequestParam(required = false) String keyword,
                                 Authentication auth) {
        // 浏览列表对游客开放，但搜索需要登录
        if (keyword != null && !keyword.trim().isEmpty() && auth == null) {
            return Result.error("请先登录后再搜索");
        }
        Page<Post> pageResult = postService.getPostList(page, size, tribeId, sort, keyword);
        return Result.success(pageResult);
    }

    @GetMapping("/my")
    public Result<?> getMyPosts(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return Result.success(postService.getMyPosts(user.getId()));
    }

    @GetMapping("/{postId}")
    public Result<?> getPostDetail(@PathVariable Long postId, Authentication auth) {
        User user = auth == null ? null : userService.findByUsername(auth.getName());
        // 管理员从后台「查看」进来的可能是待审核 / 已拒绝的帖子，允许直接预览
        PostDetailDTO detail = (user != null && "ADMIN".equals(user.getRole()))
                ? postService.getPostDetailAdmin(postId)
                : postService.getPostDetail(postId, user == null ? null : user.getId());
        if (detail == null) return Result.error("帖子不存在或未通过审核");
        return Result.success(detail);
    }

    @PostMapping("/comment")
    public Result<?> addComment(@RequestBody CommentDTO dto, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        if (postService.isUserRestricted(user.getId(), dto.getPostId())) {
            return Result.error("您已被管理员禁止评论该帖子");
        }
        try {
            Comment comment = postService.addComment(dto.getPostId(), user.getId(), dto.getContent().trim(), dto.getParentId());
            if (comment == null) return Result.error("帖子不存在或回复的评论不存在");
            return Result.success("评论成功", comment);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/comment/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        Comment comment = postService.getCommentById(commentId);
        if (comment == null) return Result.error("评论不存在");
        Post post = postService.getPostById(comment.getPostId());
        if (post == null) return Result.error("帖子不存在");
        boolean ok = postService.deleteComment(commentId, user.getId(), post.getUserId());
        return ok ? Result.success("删除成功", null) : Result.error("无权限");
    }

    @PostMapping("/comment/like/{commentId}")
    public Result<?> likeComment(@PathVariable Long commentId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = postService.likeComment(commentId, user.getId());
        return ok ? Result.success("点赞成功", null) : Result.error("您已经点过赞");
    }

    @DeleteMapping("/comment/like/{commentId}")
    public Result<?> unlikeComment(@PathVariable Long commentId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        boolean ok = postService.unlikeComment(commentId, user.getId());
        return ok ? Result.success("取消点赞成功", null) : Result.error("操作失败");
    }
}
