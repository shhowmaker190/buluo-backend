package com.vote.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String base64Secret;
    @Value("${jwt.expiration}")
    private Long expiration;
    private SecretKey getKey() { return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret)); }
    public String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    /**
     * 额外写入 uid：用户改名后 username 会变，只靠 subject 会导致旧 token 立刻失效，
     * 过滤器因此优先按 uid 取用户（老 token 没有 uid 时仍回退按 username 查）。
     */
    public String generateToken(String username, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (userId != null) claims.put("uid", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }
    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public Long extractUserId(String token) {
        Object uid = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload().get("uid");
        if (uid == null) return null;
        try {
            return Long.valueOf(uid.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public String extractRole(String token) {
        return (String) Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload().get("role");
    }
    public boolean validateToken(String token) {
        try { Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token); return true; } catch(Exception e) { return false; }
    }
}