package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vote.entity.CheckinRecord;
import com.vote.entity.User;
import com.vote.mapper.CheckinRecordMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 每日签到：一天一次，每次 +5 经验；连续满 7 天的那天额外 +10。
 * 经验写进 user.exp，等级算活跃值时一并计入（满级门槛 300）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    /** 每天签到获得的基础经验 */
    public static final int DAILY_EXP = 5;
    /** 连续签到满 7 天当日的额外奖励 */
    public static final int STREAK_BONUS = 10;
    public static final int STREAK_CYCLE = 7;
    /** 连续天数最多回看这么久，防止无限循环 */
    private static final int STREAK_LOOKBACK_DAYS = 400;

    private final CheckinRecordMapper checkinMapper;
    private final UserMapper userMapper;

    /** 某月签到情况 + 今日状态 + 连续天数 + 累计经验 */
    public Map<String, Object> status(Long userId, Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        YearMonth ym;
        try {
            ym = (year == null || month == null) ? YearMonth.from(today) : YearMonth.of(year, month);
        } catch (Exception e) {
            ym = YearMonth.from(today);
        }

        List<CheckinRecord> monthRecords = checkinMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .between(CheckinRecord::getCheckinDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByAsc(CheckinRecord::getCheckinDate));

        List<String> checkedDates = new ArrayList<>();
        int monthExp = 0;
        for (CheckinRecord r : monthRecords) {
            checkedDates.add(r.getCheckinDate().toString());
            monthExp += r.getExp() == null ? 0 : r.getExp();
        }

        boolean todayChecked = monthRecords.stream().anyMatch(r -> today.equals(r.getCheckinDate()))
                || checkinMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .eq(CheckinRecord::getCheckinDate, today)) > 0;

        User user = userMapper.selectById(userId);
        int totalExp = user == null || user.getExp() == null ? 0 : user.getExp();

        Map<String, Object> data = new HashMap<>();
        data.put("month", ym.toString());
        data.put("today", today.toString());
        data.put("checkedDates", checkedDates);
        data.put("monthCount", monthRecords.size());
        data.put("monthExp", monthExp);
        data.put("todayChecked", todayChecked);
        // 已签到：当前连续天数；未签到：昨天为止的连续天数（打卡后 +1）
        data.put("streak", currentStreak(userId, today));
        data.put("totalExp", totalExp);
        data.put("dailyExp", DAILY_EXP);
        data.put("streakBonus", STREAK_BONUS);
        return data;
    }

    /** 今日签到 */
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        LocalDate today = LocalDate.now();
        Long exist = checkinMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getCheckinDate, today));
        if (exist != null && exist > 0) throw new RuntimeException("今天已经签到过了，明天再来吧");

        // 今天还没签，currentStreak 统计的是到昨天为止的连续天数
        int streak = currentStreak(userId, today) + 1;
        int exp = DAILY_EXP + (streak % STREAK_CYCLE == 0 ? STREAK_BONUS : 0);

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setExp(exp);
        record.setCreatedAt(LocalDateTime.now());
        try {
            checkinMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发连点：唯一索引挡住第二条
            throw new RuntimeException("今天已经签到过了，明天再来吧");
        }
        userMapper.addExp(userId, exp);
        log.info("用户 {} 签到成功：+{} 经验，连续 {} 天", userId, exp, streak);

        Map<String, Object> data = status(userId, today.getYear(), today.getMonthValue());
        data.put("expAdded", exp);
        data.put("streak", streak);
        data.put("bonus", exp > DAILY_EXP);
        return data;
    }

    /** 到 today 为止的连续签到天数（today 没签就从昨天往前数） */
    private int currentStreak(Long userId, LocalDate today) {
        LocalDate from = today.minusDays(STREAK_LOOKBACK_DAYS);
        List<CheckinRecord> recent = checkinMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .between(CheckinRecord::getCheckinDate, from, today));
        Set<LocalDate> days = new HashSet<>();
        for (CheckinRecord r : recent) days.add(r.getCheckinDate());
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
