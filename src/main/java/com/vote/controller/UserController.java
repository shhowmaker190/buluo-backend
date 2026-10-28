package com.vote.controller;

import com.vote.common.Result;
import com.vote.entity.Comment;
import com.vote.entity.User;
import com.vote.mapper.CommentMapper;
import com.vote.service.FriendService;
import com.vote.service.PostService;
import com.vote.service.TribeService;
import com.vote.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PostService postService;
    private final TribeService tribeService;
    private final FriendService friendService;
    private final CommentMapper commentMapper;

    @PostMapping("/changePassword")
    public Result<?> changePassword(@RequestBody Map<String, String> params, Authentication auth) {
        String username = auth.getName();
        String old = params.get("oldPassword");
        String newPwd = params.get("newPassword");
        if (old == null || old.isEmpty()) return Result.error(400, "原密码不能为空");
        if (newPwd == null || newPwd.length() < 6) return Result.error(400, "新密码至少6位");
        boolean ok = userService.changePassword(username, old, newPwd);
        return ok ? Result.success("密码修改成功，请重新登录") : Result.error("原密码错误");
    }

    @PostMapping("/avatar")
    public Result<?> updateAvatar(@RequestBody Map<String, String> params, Authentication auth) {
        String avatar = params.get("avatar");
        if (avatar == null || avatar.isEmpty()) return Result.error("头像地址不能为空");
        userService.updateAvatar(auth.getName(), avatar);
        return Result.success(avatar);
    }

    @GetMapping("/{userId}/profile")
    public Result<?> publicProfile(@PathVariable Long userId, Authentication auth) {
        User user = userService.findById(userId);
        if (user == null) return Result.error("用户不存在");
        boolean isOwner = auth != null && user.getUsername().equals(auth.getName());
        boolean postsPublic = user.getPostsPublic() == null || user.getPostsPublic() == 1;
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("userNo", user.getUserNo());
        data.put("username", user.getUsername());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        data.put("createdAt", user.getCreatedAt());
        data.put("postCount", postService.countPublishedByUserId(userId));
        data.put("commentCount", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId)));
        // 签到等额外经验，前端算等级时一并计入
        data.put("exp", user.getExp() == null ? 0 : user.getExp());
        data.put("tribes", tribeService.myTribes(userId));
        data.put("postsPublic", postsPublic);
        // 本人永远能看自己的帖子；他人访问时按隐私设置决定是否返回列表
        data.put("postsHidden", !postsPublic && !isOwner);
        data.put("posts", (postsPublic || isOwner) ? postService.getPublishedPosts(userId, 20) : new ArrayList<>());
        return Result.success(data);
    }

    /**
     * 搜索用户：输入 6 位数字按用户 ID 精确匹配，否则按用户名模糊匹配。
     * 结果带上好友关系，前端可直接决定显示「添加好友」还是「已申请」。
     */
    @GetMapping("/search")
    public Result<?> search(@RequestParam(required = false) String keyword, Authentication auth) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return Result.error("请输入用户名或 6 位用户 ID");
        User me = auth == null ? null : userService.findByUsername(auth.getName());
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userService.searchUsers(kw, 20)) {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", u.getId());
            m.put("userNo", u.getUserNo());
            m.put("username", u.getUsername());
            m.put("avatar", u.getAvatar());
            m.put("isSelf", me != null && me.getId().equals(u.getId()));
            m.put("isFriend", me != null && friendService.isFriend(me.getId(), u.getId()));
            m.put("pending", me != null && friendService.hasPendingRequest(me.getId(), u.getId()));
            list.add(m);
        }
        return Result.success(list);
    }

    @PostMapping("/posts-visibility")
    public Result<?> setPostsVisibility(@RequestBody Map<String, Object> params, Authentication auth) {
        Object visible = params.get("visible");
        if (visible == null) return Result.error(400, "缺少 visible 参数");
        int value = userService.setPostsPublic(auth.getName(), Boolean.parseBoolean(String.valueOf(visible)));
        Map<String, Object> data = new HashMap<>();
        data.put("postsPublic", value);
        data.put("visible", value == 1);
        return Result.success("已保存", data);
    }

    @GetMapping("/profile")
    public Result<?> profile(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (user == null) return Result.error("用户不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("userNo", user.getUserNo());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        data.put("createdAt", user.getCreatedAt());
        data.put("postsPublic", user.getPostsPublic() == null ? 1 : user.getPostsPublic());
        // 等级口径统一用「已发布」帖子数，待审核单列，避免首页/个人主页等级不一致
        data.put("postCount", postService.countPublishedByUserId(user.getId()));
        data.put("pendingCount", postService.countPendingByUserId(user.getId()));
        data.put("commentCount", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, user.getId())));
        // 签到等额外经验，前端算等级时一并计入
        data.put("exp", user.getExp() == null ? 0 : user.getExp());
        data.put("tribes", tribeService.myTribes(user.getId()));
        return Result.success(data);
    }
}
