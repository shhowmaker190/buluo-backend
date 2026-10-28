package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.Post;
import com.vote.entity.Report;
import com.vote.entity.Tribe;
import com.vote.entity.User;
import com.vote.mapper.PostMapper;
import com.vote.mapper.ReportMapper;
import com.vote.mapper.TribeMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 举报：帖子 / 部落。用户在详情页发起，管理员在后台处理——
 * 处理动作只有两种：下架内容（status 置 2）或驳回（内容保留）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    public static final String TYPE_POST = "POST";
    public static final String TYPE_TRIBE = "TRIBE";

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final TribeMapper tribeMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Transactional
    public void submit(Long reporterId, String targetType, Long targetId, String reason, String detail) {
        String type = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!TYPE_POST.equals(type) && !TYPE_TRIBE.equals(type)) {
            throw new RuntimeException("举报对象类型不合法");
        }
        if (targetId == null) throw new RuntimeException("缺少举报对象");
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) throw new RuntimeException("请选择举报原因");
        if (text.length() > 32) throw new RuntimeException("举报原因过长");
        String extra = detail == null ? "" : detail.trim();
        if (extra.length() > 500) throw new RuntimeException("补充说明不能超过 500 字");

        Long ownerId = checkTarget(type, targetId);
        if (reporterId.equals(ownerId)) throw new RuntimeException("不能举报自己发布的内容");

        Long dup = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, type)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, 0));
        if (dup != null && dup > 0) throw new RuntimeException("你已经举报过该内容，请等待管理员处理");

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setReason(text);
        report.setDetail(extra.isEmpty() ? null : extra);
        report.setStatus(0);
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);

        User reporter = userMapper.selectById(reporterId);
        String reporterName = reporter == null ? "用户" : reporter.getUsername();
        String targetLabel;
        if (TYPE_POST.equals(type)) {
            Post post = postMapper.selectById(targetId);
            targetLabel = post == null ? "帖子" : "帖子《" + post.getTitle() + "》";
        } else {
            Tribe tribe = tribeMapper.selectById(targetId);
            targetLabel = tribe == null ? "部落" : "部落「" + tribe.getName() + "」";
        }
        notificationService.notifyAdmins("有新的举报待处理",
                targetLabel + " · " + text + " · 举报人 " + reporterName,
                reporterName, "ADMIN_REPORT", report.getId());
    }

    /** 校验对象存在且处于可举报状态，返回内容作者 id */
    private Long checkTarget(String type, Long targetId) {
        if (TYPE_POST.equals(type)) {
            Post post = postMapper.selectById(targetId);
            if (post == null) throw new RuntimeException("帖子不存在");
            if (post.getStatus() == null || post.getStatus() != 1) {
                throw new RuntimeException("该帖子当前不可举报");
            }
            return post.getUserId();
        }
        Tribe tribe = tribeMapper.selectById(targetId);
        if (tribe == null) throw new RuntimeException("部落不存在");
        if (tribe.getStatus() == null || tribe.getStatus() != 1) {
            throw new RuntimeException("该部落当前不可举报");
        }
        return tribe.getCreatorId();
    }

    /** 管理端列表：status 为空时返回全部，并填充举报人 / 目标标题 */
    public List<Report> list(Integer status) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Report::getStatus, status);
        wrapper.orderByAsc(Report::getStatus).orderByDesc(Report::getCreatedAt);
        List<Report> list = reportMapper.selectList(wrapper);

        List<Report> result = new ArrayList<>();
        for (Report r : list) {
            User reporter = userMapper.selectById(r.getReporterId());
            r.setReporterName(reporter == null ? "已注销用户" : reporter.getUsername());
            if (TYPE_POST.equals(r.getTargetType())) {
                Post post = postMapper.selectById(r.getTargetId());
                r.setTargetTitle(post == null ? "（帖子已删除）" : post.getTitle());
                if (post != null) {
                    User author = userMapper.selectById(post.getUserId());
                    r.setTargetAuthor(author == null ? "未知" : author.getUsername());
                }
            } else {
                Tribe tribe = tribeMapper.selectById(r.getTargetId());
                r.setTargetTitle(tribe == null ? "（部落已删除）" : tribe.getName());
                if (tribe != null) {
                    User creator = userMapper.selectById(tribe.getCreatorId());
                    r.setTargetAuthor(creator == null ? "未知" : creator.getUsername());
                }
            }
            result.add(r);
        }
        return result;
    }

    /**
     * 管理员处理举报。
     *
     * @param takeDown true=下架被举报内容，false=驳回举报（内容保留）
     */
    @Transactional
    public void handle(Long reportId, Long adminId, boolean takeDown, String note) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new RuntimeException("举报记录不存在");
        if (report.getStatus() != null && report.getStatus() != 0) {
            throw new RuntimeException("该举报已处理过");
        }
        String reason = note == null ? "" : note.trim();
        if (reason.length() > 200) throw new RuntimeException("处理说明不能超过 200 字");

        Long ownerId = null;
        if (takeDown) {
            String takeDownNote = reason.isEmpty() ? "举报核实：内容违规，已下架" : reason;
            if (TYPE_POST.equals(report.getTargetType())) {
                Post post = postMapper.selectById(report.getTargetId());
                if (post != null) {
                    Post update = new Post();
                    update.setId(post.getId());
                    update.setStatus(2);
                    update.setRejectReason(takeDownNote);
                    postMapper.updateById(update);
                    ownerId = post.getUserId();
                }
            } else {
                Tribe tribe = tribeMapper.selectById(report.getTargetId());
                if (tribe != null) {
                    Tribe update = new Tribe();
                    update.setId(tribe.getId());
                    update.setStatus(2);
                    update.setRejectReason(takeDownNote);
                    tribeMapper.updateById(update);
                    ownerId = tribe.getCreatorId();
                }
            }
        }

        report.setStatus(takeDown ? 1 : 2);
        report.setHandlerId(adminId);
        report.setHandleNote(reason.isEmpty() ? null : reason);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        // 通知内容作者：内容被下架
        if (takeDown && ownerId != null) {
            String what = TYPE_POST.equals(report.getTargetType()) ? "帖子" : "部落";
            notificationService.create(ownerId, "SYSTEM", "你的" + what + "已被下架",
                    "经核实存在违规内容，处理说明：" + (reason.isEmpty() ? "举报属实" : reason),
                    "管理员", "NONE", null);
        }
        // 通知举报人：处理结果
        notificationService.create(report.getReporterId(), "SYSTEM", "举报已处理",
                takeDown ? "你举报的内容已被下架，感谢你的反馈" : "你举报的内容经核实未违规，本次举报已驳回",
                "管理员", "NONE", null);

        log.info("举报处理完成: reportId={}, takeDown={}, admin={}", reportId, takeDown, adminId);
    }

    /** 当前用户对某个目标的待处理举报（前端用来显示「已举报」） */
    public boolean pending(Long reporterId, String targetType, Long targetId) {
        Long c = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType == null ? "" : targetType.trim().toUpperCase())
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, 0));
        return c != null && c > 0;
    }
}
