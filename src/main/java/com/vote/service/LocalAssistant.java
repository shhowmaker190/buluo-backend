package com.vote.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地知识库助手：不调用任何外部 API，零成本、零延迟。
 * 没有配置 DeepSeek Key 时用它兜底，保证 AI 小助手始终能用。
 */
@Component
public class LocalAssistant {

    private record Entry(String[] keywords, String answer) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public LocalAssistant() {
        entries.add(new Entry(new String[]{"等级", "经验", "升级", "活跃值", "lv6", "元老", "level"},
                "等级看「活跃值」：已发布帖子 × 3 + 评论 × 1 + 签到经验。\n" +
                        "Lv1 萌新 0 · Lv2 见习 10 · Lv3 常客 30 · Lv4 活跃 70 · Lv5 核心 150 · Lv6 元老 300。\n" +
                        "所以想升级最快：多发审核通过的帖子、多回复，再坚持每天签到。"));

        entries.add(new Entry(new String[]{"签到", "打卡", "日历", "连续"},
                "首页最左侧有每日打卡日历：\n" +
                        "· 每天签到 +5 经验，一天只能签一次；\n" +
                        "· 连续签到满 7 天的那天额外 +10（当天共 15）；\n" +
                        "· 断签后连续天数从 1 重新算。\n" +
                        "签到经验会直接加进等级活跃值。"));

        entries.add(new Entry(new String[]{"审核", "没显示", "看不到", "发帖", "帖子不见", "待审"},
                "发帖后是「待审核」状态，管理员通过后才会出现在帖子列表里。\n" +
                        "· 想确认进度：进「我的帖子」看状态；\n" +
                        "· 被拒绝会在通知铃铛里收到原因，可以修改后重新发。\n" +
                        "注意：等级只统计「已发布」的帖子，待审核的不算。"));

        entries.add(new Entry(new String[]{"创建部落", "建部落", "新建部落", "开部落"},
                "创建部落有两个入口：首页左侧底部的「+ 建设部落」，或导航栏「我的部落 ▾ → 创建部落」。\n" +
                        "填名称和简介提交后需要管理员审核，通过后你自动成为首领。首领可以改资料、任命副首领、禁言和踢人。"));

        entries.add(new Entry(new String[]{"加入部落", "退出部落", "成员", "首领", "禁言"},
                "在首页左侧点任意部落即可进入详情，点「加入部落」；已加入的可以「退出部落」。\n" +
                        "首领在成员列表里可以任命副首领（最多 3 位）、禁言或踢出成员。"));

        entries.add(new Entry(new String[]{"好友", "私聊", "聊天", "id", "加好友", "搜索用户"},
                "每个用户都有一个 6 位唯一 ID（例如管理员是 000001），在个人主页能看到。\n" +
                        "· 加好友：进「好友」页，在搜索框输入 6 位 ID 或用户名，点「加好友」；\n" +
                        "· 通过后会收到通知，同意后即可私聊；\n" +
                        "· 对方还不是你好友时，你只能先发 1 条消息。"));

        entries.add(new Entry(new String[]{"举报", "违规", "广告", "骂人"},
                "帖子详情页和部落详情里都有「举报」按钮，可选违法违规、色情低俗、广告引流、人身攻击、虚假信息等理由。\n" +
                        "提交后进入管理员审核队列，处理结果（下架或驳回）会通过站内通知告知你。"));

        entries.add(new Entry(new String[]{"改名", "用户名", "昵称", "名字"},
                "在个人主页用户名旁边点「修改用户名」即可提交申请。\n" +
                        "要求 2~20 位中文、字母、数字或下划线，不能与现有用户重名；同时只能有一条待审申请，审核通过后才会真正改名。"));

        entries.add(new Entry(new String[]{"点赞", "取消赞", "赞"},
                "点一下「赞」就是点赞，已赞状态下再点一下即可取消，数字会实时变化。\n" +
                        "主楼回复和楼中楼回复都支持。"));

        entries.add(new Entry(new String[]{"热度", "热门", "榜单", "排行"},
                "首页最右侧是热门部落榜，热度 = 成员数 × 3 + 已发布帖子数 × 2。\n" +
                        "热度分五档：Lv1 新芽（0）、Lv2 微热（5）、Lv3 升温（12）、Lv4 火爆（25）、Lv5 顶流（50）。"));

        entries.add(new Entry(new String[]{"密码", "登录不了", "忘记密码", "改密码"},
                "登录状态下，首页导航栏有「修改密码」，需要输入原密码。\n" +
                        "如果忘记原密码，目前只能联系管理员（ID 000001）在后台协助处理。"));

        entries.add(new Entry(new String[]{"管理员", "客服", "联系你们", "反馈"},
                "管理员账号是 admin，用户 ID 为 000001 —— 在「好友」页搜索 000001 就能找到并发起私聊。\n" +
                        "功能异常、内容申诉、忘记密码都可以找他。"));

        entries.add(new Entry(new String[]{"你好", "hi", "hello", "你是谁", "在吗", "能做什么"},
                "你好，我是部落闲聊的 AI 小助手。\n" +
                        "我可以帮你：了解怎么升级/签到、帖子为什么没显示、怎么创建或加入部落、怎么加好友私聊、怎么举报或改名。\n" +
                        "直接问我就行，比如「等级怎么升得最快？」"));

        entries.add(new Entry(new String[]{"签到经验不给", "没加经验", "经验不变"},
                "签到经验会加在等级活跃值里，个人主页的「活跃值」数字就是总数。\n" +
                        "如果签到成功但数字没变，刷新一下个人主页；还是不对的话把用户 ID 发给管理员排查。"));
    }

    /**
     * 只返回「命中」的答案，没命中返回 null。
     * 供 AiService 判断：常见问题直接本地秒回，不必去等免费 AI。
     */
    public String tryReply(String question) {
        String q = question == null ? "" : question.toLowerCase().trim();
        if (q.isEmpty()) return null;

        Entry best = null;
        int bestScore = 0;
        for (Entry entry : entries) {
            int score = 0;
            for (String kw : entry.keywords()) {
                if (q.contains(kw.toLowerCase())) {
                    // 命中词越长权重越高，避免「赞」这种单字乱匹配
                    score += kw.length() >= 2 ? kw.length() : 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        return best != null ? best.answer() : null;
    }

    /** 兜底回答：没命中时给引导 */
    public String reply(String question) {
        String hit = tryReply(question);
        return hit != null ? hit : fallback();
    }

    private String fallback() {
        return "这个问题我暂时没把握答准。你可以试试问：\n" +
                "· 等级怎么升得最快？\n" +
                "· 帖子为什么没显示？\n" +
                "· 怎么创建或加入部落？\n" +
                "· 怎么加好友私聊？\n" +
                "也可以直接在「好友」页搜索 000001 找管理员。";
    }
}
