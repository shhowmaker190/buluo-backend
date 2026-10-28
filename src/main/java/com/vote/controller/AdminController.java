package com.vote.controller;

import com.vote.common.Result;
import com.vote.dto.PostDetailDTO;
import com.vote.dto.RestrictDTO;
import com.vote.entity.Post;
import com.vote.entity.User;
import com.vote.service.AdminService;
import com.vote.service.PostService;
import com.vote.service.RenameService;
import com.vote.service.ReportService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final UserService userService;
    private final PostService postService;
    private final ReportService reportService;
    private final RenameService renameService;

    @DeleteMapping("/post/{postId}")
    public Result<?> deletePost(@PathVariable Long postId) {
        adminService.deletePost(postId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/restrict-comment")
    public Result<?> restrictComment(@RequestBody RestrictDTO dto, Authentication auth) {
        User admin = userService.findByUsername(auth.getName());
        adminService.restrictUserComment(dto.getUserId(), dto.getPostId(), admin.getId());
        return Result.success("已限制该用户评论此帖子", null);
    }

    @DeleteMapping("/unrestrict-comment")
    public Result<?> unrestrictComment(@RequestBody RestrictDTO dto) {
        adminService.unrestrictUserComment(dto.getUserId(), dto.getPostId());
        return Result.success("已解除限制", null);
    }

    @DeleteMapping("/comment/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId) {
        adminService.deleteComment(commentId);
        return Result.success("删除成功", null);
    }

    @GetMapping("/users")
    public Result<?> getAllUsers() {
        return Result.success(adminService.getAllUsers());
    }

    @GetMapping("/tribes/pending")
    public Result<?> getPendingTribes() {
        return Result.success(adminService.getPendingTribes());
    }

    @GetMapping("/tribes")
    public Result<?> getAllTribes() {
        return Result.success(adminService.getAllTribes());
    }

    @PostMapping("/tribe/approve/{tribeId}")
    public Result<?> approveTribe(@PathVariable Long tribeId) {
        return adminService.approveTribe(tribeId)
                ? Result.success("部落已通过审核", null)
                : Result.error("操作失败，部落不存在或已审核");
    }

    @GetMapping("/tribes/pending-edits")
    public Result<?> getPendingTribeEdits() {
        return Result.success(adminService.getPendingTribeEdits());
    }

    @PostMapping("/tribe/approve-edit/{tribeId}")
    public Result<?> approveTribeEdit(@PathVariable Long tribeId) {
        return adminService.approveTribeEdit(tribeId)
                ? Result.success("修改已通过并生效", null)
                : Result.error("操作失败，没有待审修改");
    }

    @PostMapping("/tribe/reject-edit/{tribeId}")
    public Result<?> rejectTribeEdit(@PathVariable Long tribeId,
                                     @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return adminService.rejectTribeEdit(tribeId, reason)
                ? Result.success("已拒绝该修改", null)
                : Result.error("操作失败，没有待审修改");
    }

    @PostMapping("/tribe/reject/{tribeId}")
    public Result<?> rejectTribe(@PathVariable Long tribeId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return adminService.rejectTribe(tribeId, reason)
                ? Result.success("已拒绝该部落", null)
                : Result.error("操作失败，部落不存在或已审核");
    }

    // 获取待审核的帖子列表
    @GetMapping("/posts/pending")
    public Result<?> getPendingPosts() {
        List<Post> pendingPosts = postService.getPendingPosts();
        return Result.success(pendingPosts);
    }

    // 全部帖子（含待审 / 已拒绝），管理端按 ID 管理
    @GetMapping("/posts")
    public Result<?> getAllPosts(@RequestParam(defaultValue = "300") int limit) {
        return Result.success(postService.getAllPostsForAdmin(limit));
    }

    // 审核通过
    @PostMapping("/post/approve/{postId}")
    public Result<?> approvePost(@PathVariable Long postId) {
        boolean ok = postService.approvePost(postId);
        return ok ? Result.success("审核通过", null) : Result.error("操作失败");
    }

    // 审核拒绝
    @PostMapping("/post/reject/{postId}")
    public Result<?> rejectPost(@PathVariable Long postId, @RequestParam String reason) {
        boolean ok = postService.rejectPost(postId, reason);
        return ok ? Result.success("已拒绝", null) : Result.error("操作失败");
    }

    // 管理员查看帖子详情（不受审核状态限制）
    @GetMapping("/post/{postId}")
    public Result<?> getPostDetailAdmin(@PathVariable Long postId) {
        PostDetailDTO detail = postService.getPostDetailAdmin(postId);
        if (detail == null) return Result.error("帖子不存在");
        return Result.success(detail);
    }

    // ---------- 举报审核 ----------

    @GetMapping("/reports")
    public Result<?> getReports(@RequestParam(required = false) Integer status) {
        return Result.success(reportService.list(status));
    }

    /** takeDown=true 下架被举报内容；false 驳回举报 */
    @PostMapping("/report/handle/{reportId}")
    public Result<?> handleReport(@PathVariable Long reportId,
                                  @RequestBody(required = false) Map<String, Object> body,
                                  Authentication auth) {
        User admin = userService.findByUsername(auth.getName());
        boolean takeDown = body != null && body.get("takeDown") != null
                && Boolean.parseBoolean(body.get("takeDown").toString());
        String note = body == null || body.get("note") == null ? null : body.get("note").toString();
        try {
            reportService.handle(reportId, admin.getId(), takeDown, note);
            return Result.success(takeDown ? "内容已下架" : "举报已驳回", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ---------- 改名审核 ----------

    @GetMapping("/rename-requests")
    public Result<?> getRenameRequests(@RequestParam(required = false) Integer status) {
        return Result.success(renameService.list(status));
    }

    @PostMapping("/rename/handle/{requestId}")
    public Result<?> handleRename(@PathVariable Long requestId,
                                  @RequestBody(required = false) Map<String, Object> body,
                                  Authentication auth) {
        User admin = userService.findByUsername(auth.getName());
        boolean accept = body != null && body.get("accept") != null
                && Boolean.parseBoolean(body.get("accept").toString());
        String reason = body == null || body.get("reason") == null ? null : body.get("reason").toString();
        try {
            renameService.handle(requestId, admin.getId(), accept, reason);
            return Result.success(accept ? "已通过，用户名已更新" : "已拒绝该申请", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

}
