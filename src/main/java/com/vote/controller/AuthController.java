package com.vote.controller;

import com.vote.common.Result;
import com.vote.config.JwtUtils;
import com.vote.dto.LoginRequest;
import com.vote.dto.LoginResponse;
import com.vote.dto.RegisterRequest;
import com.vote.entity.User;
import com.vote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest req) {
        if (userService.findByUsername(req.getUsername()) != null) return Result.error("用户名已存在");
        // 角色一律由服务端决定：忽略客户端传入的 roleCode，
        // 否则任何人只要发 roleCode=ADMIN 就能自助注册成管理员（越权）。
        // 管理员账号只能由 DBA / 迁移脚本设置。
        boolean success = userService.register(req.getUsername(), req.getPassword(), req.getEmail(), "USER");
        return success ? Result.success("注册成功") : Result.error("注册失败");
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            User user = userService.findByUsername(req.getUsername());
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getId());
            return Result.success(new LoginResponse(token, user.getUsername(), user.getRole(), user.getId(), user.getAvatar()));
        } catch (Exception e) {
            return Result.error(401, "用户名或密码错误");
        }
    }
}