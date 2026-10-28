package com.vote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vote.dto.*;
import com.vote.entity.*;
import com.vote.mapper.*;
import com.vote.service.TribeService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;
    private final TribeMapper tribeMapper;
    private final TribeService tribeService;
    private final CommentMapper commentMapper;
    private final CommentRestrictionMapper restrictionMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    @Transactional
    public Post createPost(CreatePostDTO dto, Long userId) {
        Long tribeId = dto.getTribeId();
        Tribe tribe = tribeId == null ? null : tribeMapper.selectById(tribeId);
        if (tribe == null) throw new RuntimeException("请选择有效的部落");
        if (tribe.getStatus() == null || tribe.getStatus() != 1) throw new RuntimeException("该部落未通过审核");
        if (tribeService.getMember(tribeId, userId) == null) throw new RuntimeException("请先加入该部落");
        if (tribeService.isMuted(tribeId, userId)) throw new RuntimeException("你已被该部落首领禁言");

        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setUserId(userId);
        post.setTribeId(tribeId);
        post.setCreatedAt(LocalDateTime.now());
        post.setStatus(0);
        postMapper.insert(post);

        User author = userMapper.selectById(userId);
        String authorName = author == null ? "用户" : author.getUsername();
        notificationService.notifyAdmins("有新的帖子待审核",
                "《" + post.getTitle() + "》· 来自 " + authorName,
                authorName, "ADMIN_POST", post.getId());
        cacheService.evict(HOME_LIST_KEY);
        return post;
    }

    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) return false;
        boolean ok = postMapper.deleteById(postId) > 0;
        if (ok) cacheService.evict(HOME_LIST_KEY);
        return ok;
    }

    /** 列表页每个帖子附带的热门回复条数 */
    private static final int HOT_COMMENT_SIZE = 3;

    /** 首页第一页（最新、无部落/关键词筛选）缓存 30 秒：流量最大且对 30 秒的陈旧不敏感 */
    private static final String HOME_LIST_KEY = "post:list:home:p1";
    private static final Duration HOME_LIST_TTL = Duration.ofSeconds(30);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedPage {
        private List<Post> records;
        private long total;
        private long size;
    }

    public Page<Post> getPostList(int page, int size, Long tribeId, String sort, String keyword) {
        // 分页参数兜底：page 至少为 1，size 限制在 1~50。
        // 否则 page=0 会让 offset 变成负数、size=100000 会一次性拽出整表。
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);

        boolean cacheable = safePage == 1 && tribeId == null
                && (keyword == null || keyword.isBlank()) && "latest".equals(sort);
        if (!cacheable) {
            return loadPostPage(safePage, safeSize, tribeId, sort, keyword);
        }

        CachedPage cached = cacheService.getOrLoad(HOME_LIST_KEY, CachedPage.class, HOME_LIST_TTL, () -> {
            Page<Post> loaded = loadPostPage(1, safeSize, null, "latest", null);
            return new CachedPage(loaded.getRecords(), loaded.getTotal(), loaded.getSize());
        });
        Page<Post> result = new Page<>(1, cached.getSize());
        result.setRecords(cached.getRecords());
        result.setTotal(cached.getTotal());
        return result;
    }

    private Page<Post> loadPostPage(int page, int size, Long tribeId, String sort, String keyword) {
        Page<Post> pageObj = new Page<>(page, size);
        Page<Post> result = postMapper.selectPostPage(pageObj, tribeId, sort, keyword);
        fillHotComments(result.getRecords());
        return result;
    }

    /** 一次性查出本页所有帖子的热门回复并挂到对应帖子上，避免每帖一次查询 */
    private void fillHotComments(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        List<Long> ids = posts.stream().map(Post::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, List<HotCommentDTO>> grouped = new HashMap<>();
        try {
            postMapper.selectHotComments(ids, HOT_COMMENT_SIZE)
                    .forEach(c -> grouped.computeIfAbsent(c.getPostId(), k -> new ArrayList<>()).add(c));
        } catch (Exception e) {
            // 热门回复只是列表增强信息，查询失败不能影响帖子列表本身
            log.warn("加载热门回复失败", e);
            return;
        }
        posts.forEach(p -> p.setHotComments(grouped.getOrDefault(p.getId(), Collections.emptyList())));
    }

    public PostDetailDTO getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) return null;
        return buildDetail(post, currentUserId);
    }

    public PostDetailDTO getPostDetailAdmin(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) return null;
        return buildDetail(post, null);
    }

    private PostDetailDTO buildDetail(Post post, Long currentUserId) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setUserId(post.getUserId());
        dto.setTribeId(post.getTribeId());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setStatus(post.getStatus());
        dto.setRejectReason(post.getRejectReason());
        User author = userMapper.selectById(post.getUserId());
        dto.setUsername(author != null ? author.getUsername() : "未知");
        dto.setAvatar(author != null ? author.getAvatar() : null);
        Tribe tribe = post.getTribeId() == null ? null : tribeMapper.selectById(post.getTribeId());
        dto.setTribeName(tribe != null ? tribe.getName() : "未分类");
        dto.setTribeAvatar(tribe != null ? tribe.getAvatar() : null);
        dto.setAuthorTribeRole(tribe == null ? null : tribeService.roleOf(post.getTribeId(), post.getUserId()));

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .orderByAsc(Comment::getCreatedAt));
        dto.setCommentCount(comments.size());

        // 楼主 + 所有评论者一起批量统计，保证等级口径和首页/个人主页一致，且只查两次库
        Set<Long> statUserIds = new HashSet<>(comments.stream().map(Comment::getUserId).collect(Collectors.toSet()));
        statUserIds.add(post.getUserId());
        Map<Long, Integer> postCountMap = countPublishedPostsByUserIds(statUserIds);
        Map<Long, Integer> commentCountMap = countCommentsByUserIds(statUserIds);
        Map<Long, String> tribeRoleMap = tribeService.rolesOf(post.getTribeId(), statUserIds);
        dto.setAuthorPostCount(postCountMap.getOrDefault(post.getUserId(), 0));
        dto.setAuthorCommentCount(commentCountMap.getOrDefault(post.getUserId(), 0));
        // 签到等额外经验，等级 = 发帖/评论活跃值 + exp
        Map<Long, Integer> expMap = new HashMap<>();
        userMapper.selectBatchIds(statUserIds)
                .forEach(u -> expMap.put(u.getId(), u.getExp() == null ? 0 : u.getExp()));
        dto.setAuthorExp(expMap.getOrDefault(post.getUserId(), 0));

        Map<Long, Boolean> likedMap = new HashMap<>();
        if (currentUserId != null) {
            commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getUserId, currentUserId))
                    .forEach(like -> likedMap.put(like.getCommentId(), true));
        }
        Map<Long, User> userMap = new HashMap<>();
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        Map<Long, PostDetailDTO.CommentDTO> dtoMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            PostDetailDTO.CommentDTO cd = new PostDetailDTO.CommentDTO();
            cd.setId(c.getId());
            cd.setContent(c.getContent());
            cd.setUserId(c.getUserId());
            User cu = userMap.get(c.getUserId());
            cd.setUsername(cu != null ? cu.getUsername() : "未知");
            cd.setAvatar(cu != null ? cu.getAvatar() : null);
            cd.setCreatedAt(c.getCreatedAt());
            cd.setLikeCount(c.getLikeCount() == null ? 0 : c.getLikeCount());
            cd.setLiked(likedMap.getOrDefault(c.getId(), false));
            cd.setParentId(c.getParentId());
            cd.setAuthorPostCount(postCountMap.getOrDefault(c.getUserId(), 0));
            cd.setAuthorCommentCount(commentCountMap.getOrDefault(c.getUserId(), 0));
            cd.setAuthorExp(expMap.getOrDefault(c.getUserId(), 0));
            cd.setRole(tribeRoleMap.get(c.getUserId()));
            cd.setReplies(new ArrayList<>());
            dtoMap.put(c.getId(), cd);
        }

        List<PostDetailDTO.CommentDTO> roots = new ArrayList<>();
        for (PostDetailDTO.CommentDTO cd : dtoMap.values()) {
            PostDetailDTO.CommentDTO parent = cd.getParentId() == null ? null : dtoMap.get(cd.getParentId());
            if (parent == null) {
                roots.add(cd);
            } else {
                parent.getReplies().add(cd);
            }
        }
        int floor = 1;
        for (PostDetailDTO.CommentDTO root : roots) {
            root.setFloor(floor++);
        }
        dto.setComments(roots);
        return dto;
    }

    @Transactional
    public Comment addComment(Long postId, Long userId, String content, Long parentId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) return null;
        // 部落内禁言：不能在该部落的帖子下发言
        if (post.getTribeId() != null && tribeService.isMuted(post.getTribeId(), userId)) {
            throw new RuntimeException("你已被该部落首领禁言，无法发言");
        }
        Comment parent = null;
        if (parentId != null) {
            parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getPostId().equals(postId)) return null;
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        Long targetUserId = parent != null ? parent.getUserId() : post.getUserId();
        if (!targetUserId.equals(userId)) {
            try {
                User commenter = userMapper.selectById(userId);
                notificationService.createCommentNotification(
                        targetUserId,
                        postId,
                        comment.getId(),
                        post.getTitle(),
                        content,
                        commenter != null ? commenter.getUsername() : "未知用户",
                        parent != null
                );
            } catch (Exception e) {
                log.error("发送评论通知失败: {}", e.getMessage());
            }
        }

        return comment;
    }

    @Transactional
    public boolean deleteComment(Long commentId, Long currentUserId, Long postOwnerId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) return false;
        if (!comment.getUserId().equals(currentUserId) && !postOwnerId.equals(currentUserId)) return false;
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, commentId));
        return commentMapper.deleteById(commentId) > 0;
    }

    @Transactional
    public boolean likeComment(Long commentId, Long userId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId).eq(CommentLike::getUserId, userId);
        if (commentLikeMapper.selectCount(wrapper) > 0) return false;
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) return false;

        User liker = userMapper.selectById(userId);
        String likerUsername = liker != null ? liker.getUsername() : "未知用户";

        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        commentLikeMapper.insert(like);
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        commentMapper.updateById(comment);

        if (!comment.getUserId().equals(userId)) {
            try {
                Post post = postMapper.selectById(comment.getPostId());
                String postTitle = post != null ? post.getTitle() : "未知帖子";
                notificationService.createLikeNotification(
                        comment.getUserId(),
                        comment.getPostId(),
                        postTitle,
                        comment.getContent(),
                        likerUsername
                );
            } catch (Exception e) {
                log.error("发送点赞通知失败: {}", e.getMessage());
            }
        }

        return true;
    }

    @Transactional
    public boolean unlikeComment(Long commentId, Long userId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId).eq(CommentLike::getUserId, userId);
        int deleted = commentLikeMapper.delete(wrapper);
        if (deleted > 0) {
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null) {
                comment.setLikeCount(Math.max((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1, 0));
                commentMapper.updateById(comment);
            }
            return true;
        }
        return false;
    }

    public boolean isUserRestricted(Long userId, Long postId) {
        LambdaQueryWrapper<CommentRestriction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentRestriction::getUserId, userId).eq(CommentRestriction::getPostId, postId);
        return restrictionMapper.selectCount(wrapper) > 0;
    }

    public List<Post> getMyPosts(Long userId) {
        // 必须走带 comment_count / board_name 的查询，否则列表里的评论数永远是 null
        return postMapper.selectMyPosts(userId);
    }

    /** 他人主页用：最近已发布帖子（带评论数与作者计数，供等级展示） */
    public List<Post> getPublishedPosts(Long userId, int limit) {
        return postMapper.selectPublishedPosts(userId, limit);
    }

    public long countByUserId(Long userId) {
        return postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getUserId, userId));
    }

    /** 已发布帖子数：等级口径统一用它，待审核/被拒的帖子不计入活跃值 */
    public long countPublishedByUserId(Long userId) {
        return postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId).eq(Post::getStatus, 1));
    }

    public long countPendingByUserId(Long userId) {
        return postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId).eq(Post::getStatus, 0));
    }

    /** 批量统计：已发布帖子数（status=1）按用户分组 */
    private Map<Long, Integer> countPublishedPostsByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) return new HashMap<>();
        QueryWrapper<Post> query = new QueryWrapper<Post>()
                .select("user_id", "COUNT(*) AS cnt")
                .eq("status", 1)
                .in("user_id", userIds)
                .groupBy("user_id");
        return toCountMap(postMapper.selectMaps(query));
    }

    /** 批量统计：评论数按用户分组 */
    private Map<Long, Integer> countCommentsByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) return new HashMap<>();
        QueryWrapper<Comment> query = new QueryWrapper<Comment>()
                .select("user_id", "COUNT(*) AS cnt")
                .in("user_id", userIds)
                .groupBy("user_id");
        return toCountMap(commentMapper.selectMaps(query));
    }

    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = null;
            Integer count = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
                if (entry.getValue() instanceof Number number) {
                    if (key.endsWith("user_id")) userId = number.longValue();
                    else if (key.endsWith("cnt")) count = number.intValue();
                }
            }
            if (userId != null) map.put(userId, count);
        }
        return map;
    }

    public Comment getCommentById(Long commentId) {
        return commentMapper.selectById(commentId);
    }

    public Post getPostById(Long postId) {
        return postMapper.selectById(postId);
    }

    public List<Post> getPendingPosts() {
        // 走带 join 的查询，管理端列表才能显示作者与所属部落
        return postMapper.selectPendingForAdmin();
    }

    /** 管理端：全部帖子（含待审、已拒绝），按 ID 倒序 */
    public List<Post> getAllPostsForAdmin(int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        return postMapper.selectAllForAdmin(size);
    }

    @Transactional
    public boolean approvePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !Integer.valueOf(0).equals(post.getStatus())) return false;
        post.setStatus(1);
        post.setRejectReason(null);
        boolean ok = postMapper.updateById(post) > 0;
        if (ok) {
            notificationService.create(post.getUserId(), "REVIEW", "帖子已通过审核",
                    "你的帖子《" + post.getTitle() + "》已发布", "管理员", "POST", post.getId());
            cacheService.evict(HOME_LIST_KEY);
        }
        return ok;
    }

    @Transactional
    public boolean rejectPost(Long postId, String reason) {
        Post post = postMapper.selectById(postId);
        if (post == null || !Integer.valueOf(0).equals(post.getStatus())) return false;
        post.setStatus(2);
        post.setRejectReason(reason);
        boolean ok = postMapper.updateById(post) > 0;
        if (ok) {
            notificationService.create(post.getUserId(), "REVIEW", "帖子未通过审核",
                    "你的帖子《" + post.getTitle() + "》被拒绝" +
                            (reason == null || reason.isBlank() ? "" : "，原因：" + reason),
                    "管理员", "POST", post.getId());
        }
        return ok;
    }
}
