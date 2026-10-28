package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.User;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    public User findById(Long id) {
        return id == null ? null : userMapper.selectById(id);
    }

    @Transactional
    public boolean register(String username, String password, String email, String roleCode) {
        if (findByUsername(username) != null) return false;
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(roleCode);
        user.setStatus(1);
        user.setUserNo(generateUserNo());
        return userMapper.insert(user) > 0;
    }

    public User findByUserNo(String userNo) {
        if (userNo == null || userNo.trim().isEmpty()) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserNo, userNo.trim()));
    }

    /**
     * 生成 6 位用户 ID：优先随机（100000~999999，不暴露注册量），
     * 撞号就重试；极端情况下兜底取当前最大值 +1。
     * admin 的 000001 由迁移脚本固定，随机段不会与之冲突。
     */
    private String generateUserNo() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String no = String.format("%06d", 100000 + RANDOM.nextInt(900000));
            Long used = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserNo, no));
            if (used == null || used == 0) return no;
        }
        int max = userMapper.selectList(new LambdaQueryWrapper<User>().isNotNull(User::getUserNo))
                .stream()
                .map(User::getUserNo)
                .filter(no -> no != null && no.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1);
        return String.format("%06d", Math.min(max + 1, 999999));
    }

    /** 按用户名模糊搜索，或按 6 位 ID 精确匹配 */
    public List<User> searchUsers(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (kw.matches("\\d{6}")) {
            wrapper.eq(User::getUserNo, kw);
        } else {
            wrapper.like(User::getUsername, kw);
        }
        wrapper.last("LIMIT " + limit);
        return userMapper.selectList(wrapper);
    }

    @Transactional
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

    @Transactional
    public boolean updateAvatar(String username, String avatar) {
        User user = findByUsername(username);
        if (user == null) return false;
        user.setAvatar(avatar);
        return userMapper.updateById(user) > 0;
    }

    /** 设置主页帖子列表是否公开，返回设置后的值（1=公开，0=隐藏） */
    @Transactional
    public int setPostsPublic(String username, boolean visible) {
        User user = findByUsername(username);
        if (user == null) return 1;
        user.setPostsPublic(visible ? 1 : 0);
        userMapper.updateById(user);
        return user.getPostsPublic();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("用户不存在");
        return buildUserDetails(user);
    }

    /** 按主键加载（JWT 过滤器用，用户改名后依然能定位到人） */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = findById(userId);
        if (user == null) throw new UsernameNotFoundException("用户不存在");
        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities("ROLE_" + user.getRole())
                .disabled(user.getStatus() != null && user.getStatus() == 0)
                .build();
    }
}