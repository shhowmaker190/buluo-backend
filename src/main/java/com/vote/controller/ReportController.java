package com.vote.controller;

import com.vote.common.Result;
import com.vote.entity.User;
import com.vote.service.ReportService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 举报入口：登录用户对帖子 / 部落发起举报 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    public Result<?> submit(@RequestBody Map<String, Object> body, Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        if (me == null) return Result.error(401, "请先登录");
        Object targetId = body.get("targetId");
        if (targetId == null) return Result.error("缺少举报对象");
        String targetType = body.get("targetType") == null ? null : body.get("targetType").toString();
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        String detail = body.get("detail") == null ? null : body.get("detail").toString();
        try {
            reportService.submit(me.getId(), targetType, Long.valueOf(targetId.toString()), reason, detail);
            return Result.success("举报已提交，管理员会尽快处理", null);
        } catch (NumberFormatException e) {
            return Result.error("举报对象不合法");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 是否已举报待处理，用于按钮状态 */
    @GetMapping("/status")
    public Result<?> status(@RequestParam String targetType,
                            @RequestParam Long targetId,
                            Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("pending", me != null && reportService.pending(me.getId(), targetType, targetId));
        return Result.success(data);
    }
}
