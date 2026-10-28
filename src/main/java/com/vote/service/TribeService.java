package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vote.entity.Tribe;
import com.vote.entity.TribeMember;
import com.vote.entity.User;
import com.vote.mapper.TribeMapper;
import com.vote.mapper.TribeMemberMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TribeService {

    /** 副首领名额上限 */
    public static final int MAX_DEPUTY = 3;

    private final TribeMapper tribeMapper;
    private final TribeMemberMapper tribeMemberMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    public List<Tribe> listApproved(String keyword, Long userId) {
        List<Tribe> list = tribeMapper.selectApproved(keyword, userId);
        fillHeat(list);
        return list;
    }

    public List<Tribe> myTribes(Long userId) {
        List<Tribe> list = tribeMapper.selectMyTribes(userId);
        fillHeat(list);
        return list;
    }

    public List<Tribe> createdTribes(Long userId) {
        List<Tribe> list = tribeMapper.selectCreatedTribes(userId);
        fillHeat(list);
        return list;
    }

    /**
     * 热度口径：成员数 × 3 + 已发布帖子数 × 2。
     * 成员比帖子更能代表活跃度，所以权重给得高一点；部落列表、详情、热门榜共用这一处。
     */
    private void fillHeat(List<Tribe> list) {
        if (list == null) return;
        for (Tribe t : list) fillHeat(t);
    }

    private void fillHeat(Tribe t) {
        if (t == null) return;
        int members = t.getMemberCount() == null ? 0 : t.getMemberCount();
        int posts = t.getPostCount() == null ? 0 : t.getPostCount();
        t.setHeat(members * 3 + posts * 2);
    }

    /**
     * 热门部落榜：热度 = 成员数 × 3 + 已发布帖子数 × 2。
     * 成员比帖子更能代表活跃度，所以权重给得高一点；同热度按成员数、再按名称排。
     */
    /** 热门榜缓存 key 前缀，成员/帖子数变化时按前缀批量失效 */
    private static final String HOT_KEY_PREFIX = "tribe:hot:";
    /** 榜单允许的陈旧时间：60 秒。热度变化对用户不敏感，换来的是首页一次查询都不用 */
    private static final Duration HOT_TTL = Duration.ofSeconds(60);

    public List<Tribe> hotTribes(int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        return cacheService.getOrLoad(HOT_KEY_PREFIX + size, new TypeReference<List<Tribe>>() {
        }, HOT_TTL, () -> computeHotTribes(size));
    }

    private List<Tribe> computeHotTribes(int size) {
        List<Tribe> all = tribeMapper.selectWithStats();
        fillHeat(all);
        all.sort((a, b) -> {
            int byHeat = Integer.compare(b.getHeat(), a.getHeat());
            if (byHeat != 0) return byHeat;
            int byMember = Integer.compare(
                    b.getMemberCount() == null ? 0 : b.getMemberCount(),
                    a.getMemberCount() == null ? 0 : a.getMemberCount());
            if (byMember != 0) return byMember;
            return a.getId().compareTo(b.getId());
        });
        return all.size() > size ? new ArrayList<>(all.subList(0, size)) : all;
    }

    public Tribe detail(Long tribeId, Long userId) {
        Tribe tribe = tribeMapper.selectDetail(tribeId, userId);
        fillHeat(tribe);
        return tribe;
    }

    public List<TribeMember> members(Long tribeId) {
        return tribeMemberMapper.selectMembers(tribeId);
    }

    public TribeMember getMember(Long tribeId, Long userId) {
        return tribeMemberMapper.selectOne(new LambdaQueryWrapper<TribeMember>()
                .eq(TribeMember::getTribeId, tribeId).eq(TribeMember::getUserId, userId));
    }

    public String roleOf(Long tribeId, Long userId) {
        if (tribeId == null || userId == null) return null;
        TribeMember member = getMember(tribeId, userId);
        return member == null ? null : member.getRole();
    }

    /** 批量查询一批用户在该部落的职位 */
    public Map<Long, String> rolesOf(Long tribeId, Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (tribeId == null || userIds == null || userIds.isEmpty()) return map;
        QueryWrapper<TribeMember> query = new QueryWrapper<TribeMember>()
                .select("user_id", "role")
                .eq("tribe_id", tribeId)
                .in("user_id", userIds);
        for (TribeMember member : tribeMemberMapper.selectList(query)) {
            map.put(member.getUserId(), member.getRole());
        }
        return map;
    }

    public boolean isMuted(Long tribeId, Long userId) {
        TribeMember member = getMember(tribeId, userId);
        return member != null && member.getMuted() != null && member.getMuted() == 1;
    }

    @Transactional
    public Long create(Long userId, String name, String description, String avatar) {
        if (name == null || name.trim().isEmpty()) throw new RuntimeException("部落名称不能为空");
        String trimmed = name.trim();
        if (trimmed.length() > 20) throw new RuntimeException("部落名称不能超过 20 个字");
        Long dup = tribeMapper.selectCount(new LambdaQueryWrapper<Tribe>().eq(Tribe::getName, trimmed));
        if (dup != null && dup > 0) throw new RuntimeException("该部落名称已存在");

        Tribe tribe = new Tribe();
        tribe.setName(trimmed);
        tribe.setDescription(description);
        tribe.setAvatar(avatar);
        tribe.setCreatorId(userId);
        tribe.setStatus(0);
        tribe.setCreatedAt(LocalDateTime.now());
        tribeMapper.insert(tribe);
        cacheService.evictByPrefix(HOT_KEY_PREFIX);
        log.info("创建部落待审核: {} (创建者 {})", trimmed, userId);
        User creator = userMapper.selectById(userId);
        String creatorName = creator == null ? "用户" : creator.getUsername();
        notificationService.notifyAdmins("有新的部落待审核",
                "「" + trimmed + "」· 创建者 " + creatorName,
                creatorName, "ADMIN_TRIBE", tribe.getId());
        return tribe.getId();
    }

    /**
     * 首领提交部落信息修改（名称/简介/头像），写入待审字段，管理员审核通过后生效。
     * 未提交修改的字段保持原值。
     */
    @Transactional
    public void submitEdit(Long tribeId, Long userId, String name, String description, String avatar) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null) throw new RuntimeException("部落不存在");
        TribeMember member = getMember(tribeId, userId);
        if (member == null || !TribeMember.ROLE_LEADER.equals(member.getRole())) {
            throw new RuntimeException("只有首领可以修改部落信息");
        }
        if (tribe.getStatus() != null && tribe.getStatus() != 1) {
            throw new RuntimeException("部落未通过审核，不能修改");
        }

        String newName = (name == null || name.trim().isEmpty()) ? tribe.getName() : name.trim();
        if (newName.length() > 20) throw new RuntimeException("部落名称不能超过 20 个字");
        if (!newName.equals(tribe.getName())) {
            Long dup = tribeMapper.selectCount(new LambdaQueryWrapper<Tribe>()
                    .eq(Tribe::getName, newName).ne(Tribe::getId, tribeId));
            if (dup != null && dup > 0) throw new RuntimeException("该部落名称已存在");
        }

        tribe.setPendingName(newName);
        tribe.setPendingDescription(description == null ? tribe.getDescription() : description);
        tribe.setPendingAvatar(avatar == null ? tribe.getAvatar() : avatar);
        tribe.setPendingStatus(1);
        tribeMapper.updateById(tribe);
        log.info("部落修改待审核: {} (首领 {})", tribeId, userId);
        User leader = userMapper.selectById(userId);
        notificationService.notifyAdmins("有部落修改待审核",
                "「" + tribe.getName() + "」提交了资料修改 · 首领 " + (leader == null ? "未知" : leader.getUsername()),
                leader == null ? "未知" : leader.getUsername(), "ADMIN_EDIT", tribeId);
    }

    /** 管理员：待审核的部落信息修改 */
    public List<Tribe> pendingEdits() {
        return tribeMapper.selectList(new LambdaQueryWrapper<Tribe>()
                .eq(Tribe::getPendingStatus, 1).orderByAsc(Tribe::getCreatedAt));
    }

    /** 管理员：通过修改，把待审内容覆盖到正式字段 */
    @Transactional
    public boolean approveEdit(Long tribeId) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null || tribe.getPendingStatus() == null || tribe.getPendingStatus() != 1) return false;
        tribe.setName(tribe.getPendingName());
        tribe.setDescription(tribe.getPendingDescription());
        tribe.setAvatar(tribe.getPendingAvatar());
        tribe.setPendingName(null);
        tribe.setPendingDescription(null);
        tribe.setPendingAvatar(null);
        tribe.setPendingStatus(0);
        boolean ok = tribeMapper.updateById(tribe) > 0;
        if (ok) {
            notificationService.create(tribe.getCreatorId(), "REVIEW", "部落修改已通过",
                    "部落「" + tribe.getName() + "」的资料修改已生效", "管理员", "TRIBE", tribeId);
        }
        return ok;
    }

    /** 管理员：拒绝修改，清空待审内容 */
    @Transactional
    public boolean rejectEdit(Long tribeId, String reason) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null || tribe.getPendingStatus() == null || tribe.getPendingStatus() != 1) return false;
        tribe.setPendingName(null);
        tribe.setPendingDescription(null);
        tribe.setPendingAvatar(null);
        tribe.setPendingStatus(0);
        boolean ok = tribeMapper.updateById(tribe) > 0;
        if (ok) {
            String note = reason == null ? "" : reason.trim();
            notificationService.create(tribe.getCreatorId(), "REVIEW", "部落修改未通过",
                    "部落「" + tribe.getName() + "」的资料修改未通过审核" +
                            (note.isEmpty() ? "，可修改后重新提交" : "，原因：" + note),
                    "管理员", "TRIBE", tribeId);
        }
        return ok;
    }

    @Transactional
    public void join(Long tribeId, Long userId) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null) throw new RuntimeException("部落不存在");
        if (tribe.getStatus() != 1) throw new RuntimeException("该部落未通过审核");
        if (getMember(tribeId, userId) != null) throw new RuntimeException("你已经是该部落成员");

        TribeMember member = new TribeMember();
        member.setTribeId(tribeId);
        member.setUserId(userId);
        member.setRole(TribeMember.ROLE_MEMBER);
        member.setMuted(0);
        member.setJoinedAt(LocalDateTime.now());
        tribeMemberMapper.insert(member);
        // 成员数变了，热门榜缓存立刻失效（否则要等 60 秒 TTL）
        cacheService.evictByPrefix(HOT_KEY_PREFIX);
    }

    @Transactional
    public void leave(Long tribeId, Long userId) {
        TribeMember member = getMember(tribeId, userId);
        if (member == null) throw new RuntimeException("你还不是该部落成员");
        if (TribeMember.ROLE_LEADER.equals(member.getRole())) {
            throw new RuntimeException("首领不能退出自己的部落");
        }
        tribeMemberMapper.deleteById(member.getId());
        cacheService.evictByPrefix(HOT_KEY_PREFIX);
    }

    /** 首领任命/撤销副首领 */
    @Transactional
    public void setRole(Long tribeId, Long operatorId, Long targetUserId, String role) {
        String target = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!TribeMember.ROLE_DEPUTY.equals(target) && !TribeMember.ROLE_MEMBER.equals(target)) {
            throw new RuntimeException("只能任命副首领或恢复为普通成员");
        }
        TribeMember operator = requireLeader(tribeId, operatorId);
        if (operator.getUserId().equals(targetUserId)) throw new RuntimeException("不能调整首领自己的职位");

        TribeMember member = getMember(tribeId, targetUserId);
        if (member == null) throw new RuntimeException("该用户不是部落成员");
        if (TribeMember.ROLE_LEADER.equals(member.getRole())) throw new RuntimeException("不能调整首领的职位");

        if (TribeMember.ROLE_DEPUTY.equals(target) && !TribeMember.ROLE_DEPUTY.equals(member.getRole())) {
            long deputies = tribeMemberMapper.selectCount(new LambdaQueryWrapper<TribeMember>()
                    .eq(TribeMember::getTribeId, tribeId).eq(TribeMember::getRole, TribeMember.ROLE_DEPUTY));
            if (deputies >= MAX_DEPUTY) throw new RuntimeException("副首领最多 " + MAX_DEPUTY + " 名");
        }
        member.setRole(target);
        tribeMemberMapper.updateById(member);
    }

    /** 首领禁言/解除禁言（仅在该部落内生效） */
    @Transactional
    public void setMuted(Long tribeId, Long operatorId, Long targetUserId, boolean muted) {
        TribeMember operator = requireLeader(tribeId, operatorId);
        if (operator.getUserId().equals(targetUserId)) throw new RuntimeException("不能对自己禁言");
        TribeMember member = getMember(tribeId, targetUserId);
        if (member == null) throw new RuntimeException("该用户不是部落成员");
        if (TribeMember.ROLE_LEADER.equals(member.getRole())) throw new RuntimeException("不能禁言首领");
        member.setMuted(muted ? 1 : 0);
        tribeMemberMapper.updateById(member);
    }

    /** 首领踢出成员 */
    @Transactional
    public void kick(Long tribeId, Long operatorId, Long targetUserId) {
        TribeMember operator = requireLeader(tribeId, operatorId);
        if (operator.getUserId().equals(targetUserId)) throw new RuntimeException("不能踢出自己");
        TribeMember member = getMember(tribeId, targetUserId);
        if (member == null) throw new RuntimeException("该用户不是部落成员");
        if (TribeMember.ROLE_LEADER.equals(member.getRole())) throw new RuntimeException("不能踢出首领");
        tribeMemberMapper.deleteById(member.getId());
    }

    private TribeMember requireLeader(Long tribeId, Long operatorId) {
        TribeMember operator = getMember(tribeId, operatorId);
        if (operator == null || !TribeMember.ROLE_LEADER.equals(operator.getRole())) {
            throw new RuntimeException("只有首领可以执行该操作");
        }
        return operator;
    }

    // ---------- 管理员审核 ----------

    public List<Tribe> pendingTribes() {
        return tribeMapper.selectList(new LambdaQueryWrapper<Tribe>()
                .eq(Tribe::getStatus, 0).orderByAsc(Tribe::getCreatedAt));
    }

    @Transactional
    public boolean approve(Long tribeId) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null || !Integer.valueOf(0).equals(tribe.getStatus())) return false;
        tribe.setStatus(1);
        tribe.setRejectReason(null);
        tribeMapper.updateById(tribe);
        // 通过时把创建者登记为首领
        if (getMember(tribeId, tribe.getCreatorId()) == null) {
            insertLeader(tribeId, tribe.getCreatorId());
        }
        notificationService.create(tribe.getCreatorId(), "REVIEW", "部落已通过审核",
                "你创建的部落「" + tribe.getName() + "」已上线", "管理员", "TRIBE", tribeId);
        cacheService.evictByPrefix(HOT_KEY_PREFIX);
        return true;
    }

    private void insertLeader(Long tribeId, Long userId) {
        TribeMember leader = new TribeMember();
        leader.setTribeId(tribeId);
        leader.setUserId(userId);
        leader.setRole(TribeMember.ROLE_LEADER);
        leader.setMuted(0);
        leader.setJoinedAt(LocalDateTime.now());
        tribeMemberMapper.insert(leader);
    }

    @Transactional
    public boolean reject(Long tribeId, String reason) {
        Tribe tribe = tribeMapper.selectById(tribeId);
        if (tribe == null || !Integer.valueOf(0).equals(tribe.getStatus())) return false;
        tribe.setStatus(2);
        tribe.setRejectReason(reason);
        boolean ok = tribeMapper.updateById(tribe) > 0;
        if (ok) {
            notificationService.create(tribe.getCreatorId(), "REVIEW", "部落未通过审核",
                    "你创建的部落「" + tribe.getName() + "」被拒绝" +
                            (reason == null || reason.isBlank() ? "" : "，原因：" + reason),
                    "管理员", "TRIBE", tribeId);
        }
        if (ok) cacheService.evictByPrefix(HOT_KEY_PREFIX);
        return ok;
    }

    public List<Tribe> tribesOfUser(Long userId) {
        return tribeMapper.selectMyTribes(userId);
    }

    public User userOf(Long userId) {
        return userMapper.selectById(userId);
    }
}
