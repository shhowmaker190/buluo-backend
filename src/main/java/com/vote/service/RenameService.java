package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.NameRequest;
import com.vote.entity.User;
import com.vote.mapper.NameRequestMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 改名：用户只能提交申请，真正改 username 发生在管理员审核通过时。
 * 改名后旧 JWT 依然有效——过滤器按 uid 定位用户，不依赖 subject 里的用户名。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenameService {

    /** 用户名：2~20 位中文、字母、数字、下划线 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9_]{2,20}$");

    private final NameRequestMapper nameRequestMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Transactional
    public void submit(Long userId, String newName) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        String name = newName == null ? "" : newName.trim();
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new RuntimeException("用户名需为 2~20 位中文、字母、数字或下划线");
        }
        if (name.equals(user.getUsername())) throw new RuntimeException("新用户名与当前用户名相同");
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, name)) > 0) {
            throw new RuntimeException("该用户名已被占用");
        }
        Long pending = nameRequestMapper.selectCount(new LambdaQueryWrapper<NameRequest>()
                .eq(NameRequest::getUserId, userId)
                .eq(NameRequest::getStatus, 0));
        if (pending != null && pending > 0) throw new RuntimeException("你已有改名申请在审核中，请等待处理");

        NameRequest req = new NameRequest();
        req.setUserId(userId);
        req.setOldName(user.getUsername());
        req.setNewName(name);
        req.setStatus(0);
        req.setCreatedAt(LocalDateTime.now());
        nameRequestMapper.insert(req);
        notificationService.notifyAdmins("有新的改名申请",
                user.getUsername() + " → " + name, user.getUsername(), "ADMIN_RENAME", req.getId());
    }

    /** 用户查看自己最近一次改名申请（含审核结果），没有则返回 null */
    public NameRequest latestOf(Long userId) {
        List<NameRequest> list = nameRequestMapper.selectList(new LambdaQueryWrapper<NameRequest>()
                .eq(NameRequest::getUserId, userId)
                .orderByDesc(NameRequest::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 管理端列表；status 为空返回全部 */
    public List<NameRequest> list(Integer status) {
        LambdaQueryWrapper<NameRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(NameRequest::getStatus, status);
        wrapper.orderByAsc(NameRequest::getStatus).orderByDesc(NameRequest::getCreatedAt);
        List<NameRequest> list = nameRequestMapper.selectList(wrapper);
        List<NameRequest> result = new ArrayList<>();
        for (NameRequest r : list) {
            User u = userMapper.selectById(r.getUserId());
            r.setUsername(u == null ? "已注销用户" : u.getUsername());
            r.setUserNo(u == null ? null : u.getUserNo());
            result.add(r);
        }
        return result;
    }

    @Transactional
    public void handle(Long requestId, Long adminId, boolean accept, String reason) {
        NameRequest req = nameRequestMapper.selectById(requestId);
        if (req == null) throw new RuntimeException("申请不存在");
        if (req.getStatus() != null && req.getStatus() != 0) throw new RuntimeException("该申请已处理过");

        String note = reason == null ? "" : reason.trim();
        if (note.length() > 200) throw new RuntimeException("说明不能超过 200 字");

        User user = userMapper.selectById(req.getUserId());
        if (user == null) throw new RuntimeException("用户不存在");

        if (accept) {
            // 审核期间可能已有人注册了这个名字，通过前必须再校验一次
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, req.getNewName())) > 0) {
                throw new RuntimeException("该用户名已被占用，无法通过");
            }
            User update = new User();
            update.setId(user.getId());
            update.setUsername(req.getNewName());
            userMapper.updateById(update);

            req.setStatus(1);
            notificationService.create(user.getId(), "SYSTEM", "改名申请已通过",
                    "你的用户名已变更为「" + req.getNewName() + "」", "管理员", "NONE", null);
        } else {
            if (note.isEmpty()) throw new RuntimeException("请填写拒绝原因");
            req.setStatus(2);
            req.setRejectReason(note);
            notificationService.create(user.getId(), "SYSTEM", "改名申请未通过",
                    "新用户名「" + req.getNewName() + "」未通过审核：" + note, "管理员", "NONE", null);
        }
        req.setHandlerId(adminId);
        req.setHandledAt(LocalDateTime.now());
        nameRequestMapper.updateById(req);
        log.info("改名审核: requestId={}, accept={}, admin={}", requestId, accept, adminId);
    }
}
