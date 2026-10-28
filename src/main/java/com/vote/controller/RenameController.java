package com.vote.controller;

import com.vote.common.Result;
import com.vote.entity.NameRequest;
import com.vote.entity.User;
import com.vote.service.RenameService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 改名：用户提交申请、查询审核进度 */
@RestController
@RequestMapping("/api/rename")
@RequiredArgsConstructor
public class RenameController {

    private final RenameService renameService;
    private final UserService userService;

    @PostMapping
    public Result<?> submit(@RequestBody Map<String, Object> body, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        if (me == null) return Result.error(401, "请先登录");
        String newName = body.get("newName") == null ? null : body.get("newName").toString();
        try {
            renameService.submit(me.getId(), newName);
            return Result.success("申请已提交，等待管理员审核", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/latest")
    public Result<?> latest(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        NameRequest req = me == null ? null : renameService.latestOf(me.getId());
        Map<String, Object> data = new HashMap<>();
        if (req != null) {
            data.put("id", req.getId());
            data.put("oldName", req.getOldName());
            data.put("newName", req.getNewName());
            data.put("status", req.getStatus());
            data.put("rejectReason", req.getRejectReason());
            data.put("createdAt", req.getCreatedAt());
            data.put("handledAt", req.getHandledAt());
        }
        return Result.success(data);
    }
}
