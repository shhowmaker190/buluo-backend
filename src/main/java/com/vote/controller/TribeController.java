package com.vote.controller;

import com.vote.common.Result;
import com.vote.dto.CreateTribeDTO;
import com.vote.entity.Tribe;
import com.vote.entity.TribeMember;
import com.vote.entity.User;
import com.vote.service.TribeService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tribe")
@RequiredArgsConstructor
public class TribeController {

    private final TribeService tribeService;
    private final UserService userService;

    /** 全部已通过审核的部落；列出列表游客也能看，但搜索需要登录 */
    @GetMapping({"/list", "/search"})
    public Result<?> list(@RequestParam(required = false) String keyword, Authentication auth) {
        if (keyword != null && !keyword.trim().isEmpty() && auth == null) {
            return Result.error("请先登录后再搜索");
        }
        Long userId = auth == null ? null : currentUserId(auth);
        return Result.success(tribeService.listApproved(keyword, userId));
    }

    /** 热门部落榜：按热度（成员数×3 + 已发布帖子×2）降序，游客也能看 */
    @GetMapping("/hot")
    public Result<?> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(tribeService.hotTribes(limit));
    }

    /** 我加入的部落（含职位） */
    @GetMapping("/my/list")
    public Result<?> myTribes(Authentication auth) {
        return Result.success(tribeService.myTribes(currentUserId(auth)));
    }

    /** 我创建的部落（含审核状态） */
    @GetMapping("/my/created")
    public Result<?> createdTribes(Authentication auth) {
        return Result.success(tribeService.createdTribes(currentUserId(auth)));
    }

    @GetMapping("/{tribeId}")
    public Result<?> detail(@PathVariable Long tribeId, Authentication auth) {
        Long userId = auth == null ? null : currentUserId(auth);
        Tribe tribe = tribeService.detail(tribeId, userId);
        if (tribe == null) return Result.error("部落不存在");
        return Result.success(tribe);
    }

    /** 部落成员详情：用户名、头像、职位、禁言状态、等级数据 */
    @GetMapping("/{tribeId}/members")
    public Result<?> members(@PathVariable Long tribeId) {
        List<TribeMember> members = tribeService.members(tribeId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", members);
        data.put("total", members.size());
        data.put("maxDeputy", TribeService.MAX_DEPUTY);
        return Result.success(data);
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody CreateTribeDTO dto, Authentication auth) {
        try {
            Long id = tribeService.create(currentUserId(auth), dto.getName(), dto.getDescription(), dto.getAvatar());
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            return Result.success("部落已提交，等待管理员审核", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 首领修改部落头像/名称/简介，提交后待管理员审核 */
    @PutMapping("/{tribeId}")
    public Result<?> submitEdit(@PathVariable Long tribeId, @RequestBody CreateTribeDTO dto, Authentication auth) {
        try {
            tribeService.submitEdit(tribeId, currentUserId(auth), dto.getName(), dto.getDescription(), dto.getAvatar());
            return Result.success("修改已提交，等待管理员审核", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{tribeId}/join")
    public Result<?> join(@PathVariable Long tribeId, Authentication auth) {
        try {
            tribeService.join(tribeId, currentUserId(auth));
            return Result.success("已加入部落", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{tribeId}/leave")
    public Result<?> leave(@PathVariable Long tribeId, Authentication auth) {
        try {
            tribeService.leave(tribeId, currentUserId(auth));
            return Result.success("已退出部落", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 首领任命副首领 / 恢复普通成员 */
    @PutMapping("/{tribeId}/role")
    public Result<?> setRole(@PathVariable Long tribeId, @RequestBody Map<String, Object> body, Authentication auth) {
        Long targetUserId = longValue(body.get("userId"));
        String role = body.get("role") == null ? null : String.valueOf(body.get("role"));
        if (targetUserId == null) return Result.error(400, "缺少 userId");
        try {
            tribeService.setRole(tribeId, currentUserId(auth), targetUserId, role);
            return Result.success("已更新职位", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 首领禁言 / 解除禁言（仅本部落内生效） */
    @PutMapping("/{tribeId}/mute")
    public Result<?> setMuted(@PathVariable Long tribeId, @RequestBody Map<String, Object> body, Authentication auth) {
        Long targetUserId = longValue(body.get("userId"));
        boolean muted = Boolean.parseBoolean(String.valueOf(body.get("muted")));
        if (targetUserId == null) return Result.error(400, "缺少 userId");
        try {
            tribeService.setMuted(tribeId, currentUserId(auth), targetUserId, muted);
            return Result.success(muted ? "已禁言该成员" : "已解除禁言", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 首领踢出成员 */
    @DeleteMapping("/{tribeId}/member/{userId}")
    public Result<?> kick(@PathVariable Long tribeId, @PathVariable Long userId, Authentication auth) {
        try {
            tribeService.kick(tribeId, currentUserId(auth), userId);
            return Result.success("已踢出该成员", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    private Long currentUserId(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return user == null ? null : user.getId();
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
