package com.vote.controller;

import com.vote.common.Result;
import com.vote.entity.User;
import com.vote.service.CheckinService;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final UserService userService;

    /** 某月签到日历 + 今日状态 + 连续天数 + 累计经验 */
    @GetMapping("/status")
    public Result<?> status(@RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Integer month,
                            Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        return Result.success(checkinService.status(me.getId(), year, month));
    }

    /** 今日签到 */
    @PostMapping
    public Result<?> checkin(Authentication auth) {
        User me = userService.findByUsername(auth.getName());
        try {
            Map<String, Object> data = checkinService.checkin(me.getId());
            return Result.success("签到成功，经验 +" + data.get("expAdded"), data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
