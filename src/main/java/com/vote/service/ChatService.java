package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vote.entity.ChatMessage;
import com.vote.entity.Post;
import com.vote.entity.Tribe;
import com.vote.entity.User;
import com.vote.mapper.ChatMessageMapper;
import com.vote.mapper.PostMapper;
import com.vote.mapper.TribeMapper;
import com.vote.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 私聊：好友之间自由聊天；非好友时**双方各只能发 1 条**，
 * 即 A 发一句、B 回一句，之后必须成为好友才能继续。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    /** 非好友时，单向最多可发送的消息条数 */
    private static final int STRANGER_QUOTA = 1;
    /** 会话列表最多扫描的消息条数，避免全表扫描 */
    private static final int CONVERSATION_SCAN = 1000;

    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final TribeMapper tribeMapper;
    private final FriendService friendService;
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public ChatMessage send(Long fromUserId, Long toUserId, String content) {
        return doSend(fromUserId, toUserId, content, "text", null, null, null, null);
    }

    /**
     * 分享帖子 / 部落给好友：卡片标题、副标题、头像在发送时就固化到消息里，
     * 这样即使原帖后来被删，历史消息也能正常显示（点击后由目标页面提示不存在）。
     */
    @Transactional
    public ChatMessage sendShare(Long fromUserId, Long toUserId, String msgType, Long refId) {
        if (refId == null) throw new RuntimeException("缺少分享对象");
        String type = msgType == null ? "" : msgType.trim();
        String content, title, sub, avatar;
        if ("post".equals(type)) {
            Post post = postMapper.selectById(refId);
            if (post == null || post.getStatus() == null || post.getStatus() != 1) {
                throw new RuntimeException("帖子不存在或未通过审核");
            }
            title = post.getTitle();
            Tribe tribe = post.getTribeId() == null ? null : tribeMapper.selectById(post.getTribeId());
            sub = tribe == null ? "帖子" : "帖子 · " + tribe.getName();
            avatar = tribe == null ? null : tribe.getAvatar();
            content = "[帖子] " + title;
        } else if ("tribe".equals(type)) {
            Tribe tribe = tribeMapper.selectById(refId);
            if (tribe == null || tribe.getStatus() == null || tribe.getStatus() != 1) {
                throw new RuntimeException("部落不存在或未通过审核");
            }
            title = tribe.getName();
            sub = "部落 · 点击查看";
            avatar = tribe.getAvatar();
            content = "[部落] " + title;
        } else {
            throw new RuntimeException("不支持的分享类型");
        }
        return doSend(fromUserId, toUserId, content, type, refId, title, sub, avatar);
    }

    private ChatMessage doSend(Long fromUserId, Long toUserId, String content, String msgType,
                               Long refId, String refTitle, String refSub, String refAvatar) {
        if (fromUserId.equals(toUserId)) throw new RuntimeException("不能给自己发消息");
        User to = userMapper.selectById(toUserId);
        if (to == null) throw new RuntimeException("用户不存在");
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new RuntimeException("消息内容不能为空");
        if (text.length() > 2000) throw new RuntimeException("单条消息不能超过 2000 字");

        if (!friendService.isFriend(fromUserId, toUserId)) {
            long sent = messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getFromUserId, fromUserId)
                    .eq(ChatMessage::getToUserId, toUserId));
            if (sent >= STRANGER_QUOTA) {
                throw new RuntimeException("你们还不是好友，只能给对方发送 1 条消息，请先添加好友");
            }
        }

        ChatMessage message = new ChatMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(text);
        message.setMsgType(msgType);
        message.setRefId(refId);
        message.setRefTitle(refTitle);
        message.setRefSub(refSub);
        message.setRefAvatar(refAvatar);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        User from = userMapper.selectById(fromUserId);
        String fromName = from == null ? "有人" : from.getUsername();
        String preview = text.length() > 50 ? text.substring(0, 50) + "..." : text;
        notificationService.create(toUserId, "MESSAGE", "来自 " + fromName + " 的私聊",
                preview, fromName, "CHAT", fromUserId);
        sseEmitterService.send(toUserId, "chat", Map.of(
                "id", message.getId(),
                "fromUserId", fromUserId,
                "fromUsername", fromName,
                "content", preview));
        return message;
    }

    /** 会话列表：按最近一条消息的时间倒序，带对方信息与未读数 */
    public List<Map<String, Object>> conversations(Long userId) {
        // created_at 只精确到秒，同一秒内的多条消息顺序不稳定，
        // 必须再加 id 做次级排序，否则会话列表可能显示上一条消息
        // 同时剔除「我这边已删除」的消息（from_deleted/to_deleted）
        List<ChatMessage> messages = messageMapper.selectList(new QueryWrapper<ChatMessage>()
                .nested(w -> w
                        .nested(x -> x.eq("from_user_id", userId).eq("from_deleted", false))
                        .or()
                        .nested(x -> x.eq("to_user_id", userId).eq("to_deleted", false)))
                .orderByDesc("created_at")
                .orderByDesc("id")
                .last("LIMIT " + CONVERSATION_SCAN));

        // 按对方用户分组：只保留每个会话的最后一条，并统计对方发来的未读数
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (ChatMessage m : messages) {
            Long otherId = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            Map<String, Object> conv = grouped.get(otherId);
            if (conv == null) {
                conv = new HashMap<>();
                conv.put("userId", otherId);
                conv.put("lastMessage", m.getContent());
                conv.put("lastTime", m.getCreatedAt());
                conv.put("unread", 0);
                grouped.put(otherId, conv);
            }
            if (!m.getIsRead() && m.getToUserId().equals(userId)) {
                conv.put("unread", ((int) conv.get("unread")) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> conv : grouped.values()) {
            User u = userMapper.selectById((Long) conv.get("userId"));
            conv.put("username", u == null ? "未知用户" : u.getUsername());
            conv.put("avatar", u == null ? null : u.getAvatar());
            conv.put("isFriend", friendService.isFriend(userId, (Long) conv.get("userId")));
            result.add(conv);
        }
        return result;
    }

    /** 与某人的聊天记录（正序），并顺带把对方发来的标记已读 */
    @Transactional
    public List<Map<String, Object>> history(Long userId, Long otherId, int limit) {
        List<ChatMessage> list = messageMapper.selectList(new QueryWrapper<ChatMessage>()
                .nested(w -> w
                        .nested(x -> x.eq("from_user_id", userId).eq("to_user_id", otherId).eq("from_deleted", false))
                        .or()
                        .nested(x -> x.eq("from_user_id", otherId).eq("to_user_id", userId).eq("to_deleted", false)))
                .orderByAsc("created_at")
                .orderByAsc("id")
                .last("LIMIT " + Math.min(Math.max(limit, 1), 500)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : list) {
            Map<String, Object> m2 = new HashMap<>();
            m2.put("id", m.getId());
            m2.put("fromUserId", m.getFromUserId());
            m2.put("toUserId", m.getToUserId());
            m2.put("content", m.getContent());
            m2.put("msgType", m.getMsgType() == null ? "text" : m.getMsgType());
            m2.put("refId", m.getRefId());
            m2.put("refTitle", m.getRefTitle());
            m2.put("refSub", m.getRefSub());
            m2.put("refAvatar", m.getRefAvatar());
            m2.put("createdAt", m.getCreatedAt());
            m2.put("mine", m.getFromUserId().equals(userId));
            result.add(m2);
        }
        markRead(userId, otherId);
        return result;
    }

    /** 把某人发给我的消息标记为已读 */
    public void markRead(Long userId, Long otherId) {
        ChatMessage update = new ChatMessage();
        update.setIsRead(true);
        messageMapper.update(update, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, otherId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, false));
    }

    public int unreadTotal(Long userId) {
        // MyBatis-Plus 的 selectCount 返回 Long，这里条数不会溢出 int
        // 已删除的会话不再计入未读，否则铃铛会显示看不见的未读数
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, false)
                .eq(ChatMessage::getToDeleted, false));
        return count == null ? 0 : Math.toIntExact(count);
    }

    /**
     * 删除会话（单向）：把两人之间我这边能看到的全部消息标记为「我已删除」。
     * 对方的聊天记录完全不受影响；对方之后再发新消息，会话会重新出现。
     */
    @Transactional
    public void deleteConversation(Long userId, Long otherId) {
        if (userId.equals(otherId)) throw new RuntimeException("不能对自己操作");
        ChatMessage mine = new ChatMessage();
        mine.setFromDeleted(true);
        messageMapper.update(mine, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, userId)
                .eq(ChatMessage::getToUserId, otherId)
                .eq(ChatMessage::getFromDeleted, false));

        ChatMessage theirs = new ChatMessage();
        theirs.setToDeleted(true);
        messageMapper.update(theirs, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, otherId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getToDeleted, false));
    }

    /** 删除单条消息（单向）：只有我是这条消息的收发双方之一时才能删 */
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) throw new RuntimeException("消息不存在");
        ChatMessage update = new ChatMessage();
        if (userId.equals(message.getFromUserId())) {
            update.setId(messageId);
            update.setFromDeleted(true);
        } else if (userId.equals(message.getToUserId())) {
            update.setId(messageId);
            update.setToDeleted(true);
        } else {
            throw new RuntimeException("无权删除该消息");
        }
        messageMapper.updateById(update);
    }

    /** 非好友时还能发几条（0 或 1），用于前端按钮提示 */
    public int strangerQuota(Long fromUserId, Long toUserId) {
        if (friendService.isFriend(fromUserId, toUserId)) return Integer.MAX_VALUE;
        // 这里刻意统计全部历史消息（含已删除的），避免删掉消息后重新获得陌生人额度
        long sent = messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, fromUserId)
                .eq(ChatMessage::getToUserId, toUserId));
        return Math.max(0, STRANGER_QUOTA - (int) sent);
    }
}
