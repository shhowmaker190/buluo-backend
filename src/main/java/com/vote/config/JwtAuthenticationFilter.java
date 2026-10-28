package com.vote.config;

import com.vote.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserService userService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            chain.doFilter(request, response);
            return;
        }
        // 优先用 uid 定位用户：改名后 username 会变，靠 subject 会查不到人
        UserDetails userDetails = null;
        try {
            Long uid = jwtUtils.extractUserId(token);
            if (uid != null) {
                userDetails = userService.loadUserById(uid);
            } else {
                userDetails = userService.loadUserByUsername(jwtUtils.extractUsername(token));
            }
        } catch (Exception e) {
            // token 有效但用户已不存在（或被删）：按未登录处理，不要抛 500
            log.debug("token 解析用户失败: {}", e.getMessage());
        }
        if (userDetails == null) {
            chain.doFilter(request, response);
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}