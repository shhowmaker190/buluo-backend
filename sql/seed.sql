-- 演示数据（可选）：3 个账号 + 2 个部落 + 4 篇帖子 + 6 条评论
--
-- 用途：让 clone 下来的人不必自己造数据就能登录、看到内容和后台。
-- 前提：已经执行过 sql/vote_db.sql 建库建表。
-- 执行：mysql -uroot -p vote_db < sql/seed.sql
--
-- 演示账号（密码为明文，仅用于演示环境，上线前请修改）：
--   demo       / Demo@123456    普通用户
--   demo2      / Demo@123456    普通用户
--   admin_demo / Admin@123456   管理员（可进后台）
--
-- 注意：脚本使用固定主键（user 54~56 / tribe 13~14 / post 83~86），
-- 只建议在空库上导入；库里若已有同 ID 数据会报主键冲突。

SET NAMES utf8mb4;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` (`id`, `username`, `password`, `email`, `avatar`, `role`, `status`, `posts_public`, `exp`, `created_at`, `user_no`) VALUES (56,'admin_demo','$2a$10$MXBrIXZCw84C8yWZ8ilhwu8iKXT7dilRRHDpqPEv5HkZ3X83Z2PDi','admin_demo@example.com',NULL,'ADMIN',1,1,0,'2026-10-28 19:01:42','995821');
INSERT INTO `user` (`id`, `username`, `password`, `email`, `avatar`, `role`, `status`, `posts_public`, `exp`, `created_at`, `user_no`) VALUES (54,'demo','$2a$10$vTyCM1IRXFRQRDmrR7SvR.McmVpOnkmzmhRQT7hWBc0huDPP7OvGe','demo@example.com',NULL,'USER',1,1,0,'2026-10-28 19:01:42','541362');
INSERT INTO `user` (`id`, `username`, `password`, `email`, `avatar`, `role`, `status`, `posts_public`, `exp`, `created_at`, `user_no`) VALUES (55,'demo2','$2a$10$2AfNK2ICLMe4uZCMthBieeDGIYH6Og1FOUgz1EkZdNy5ubLuqauey','demo2@example.com',NULL,'USER',1,1,0,'2026-10-28 19:01:42','929517');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `tribe` DISABLE KEYS */;
INSERT INTO `tribe` (`id`, `name`, `description`, `avatar`, `creator_id`, `status`, `reject_reason`, `pending_name`, `pending_description`, `pending_avatar`, `pending_status`, `created_at`) VALUES (13,'独立游戏交流','聊聊 Steam 独立游戏、像素游戏和各种冷门佳作',NULL,54,1,NULL,NULL,NULL,NULL,0,'2026-10-28 19:01:43');
INSERT INTO `tribe` (`id`, `name`, `description`, `avatar`, `creator_id`, `status`, `reject_reason`, `pending_name`, `pending_description`, `pending_avatar`, `pending_status`, `created_at`) VALUES (14,'Java 后端自习室','Spring、MySQL、Redis 的学习记录与踩坑分享',NULL,54,1,NULL,NULL,NULL,NULL,0,'2026-10-28 19:01:43');
/*!40000 ALTER TABLE `tribe` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `tribe_member` DISABLE KEYS */;
INSERT INTO `tribe_member` (`id`, `tribe_id`, `user_id`, `role`, `muted`, `joined_at`) VALUES (30,13,54,'MEMBER',0,'2026-10-28 19:02:07');
INSERT INTO `tribe_member` (`id`, `tribe_id`, `user_id`, `role`, `muted`, `joined_at`) VALUES (31,13,55,'MEMBER',0,'2026-10-28 19:02:08');
INSERT INTO `tribe_member` (`id`, `tribe_id`, `user_id`, `role`, `muted`, `joined_at`) VALUES (32,14,54,'MEMBER',0,'2026-10-28 19:02:09');
INSERT INTO `tribe_member` (`id`, `tribe_id`, `user_id`, `role`, `muted`, `joined_at`) VALUES (33,14,55,'MEMBER',0,'2026-10-28 19:02:10');
/*!40000 ALTER TABLE `tribe_member` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `post` DISABLE KEYS */;
INSERT INTO `post` (`id`, `title`, `content`, `user_id`, `tribe_id`, `created_at`, `status`, `reject_reason`) VALUES (83,'《星露谷物语》玩了 200 小时后，我的一些碎碎念','第一年种地，第二年钓鱼，第三年才发现矿洞才是本体。\n\n最上头的是节奏感：每天 6 点起床浇水，晚上 2 点倒地，现实里的时间完全被游戏节奏带走。',54,13,'2026-10-28 19:02:15',1,NULL);
INSERT INTO `post` (`id`, `title`, `content`, `user_id`, `tribe_id`, `created_at`, `status`, `reject_reason`) VALUES (84,'求推荐适合双人联机的独立游戏','和室友想找一款能一起玩的，最好是合作而不是对战的。\n\n已玩过《胡闹厨房 2》《双人成行》，都比较喜欢。',55,13,'2026-10-28 19:02:15',1,NULL);
INSERT INTO `post` (`id`, `title`, `content`, `user_id`, `tribe_id`, `created_at`, `status`, `reject_reason`) VALUES (85,'MyBatis-Plus 分页插件的一个坑，踩了两小时','现象：page 参数传了，但总数永远是 0。\n\n原因：分页插件没注册，PaginationInnerInterceptor 需要显式配置 DbType。加上之后一次就通了。\n\n记录一下，免得下次再踩。',54,14,'2026-10-28 19:02:15',1,NULL);
INSERT INTO `post` (`id`, `title`, `content`, `user_id`, `tribe_id`, `created_at`, `status`, `reject_reason`) VALUES (86,'关于 Redis 缓存要不要给列表接口加 TTL 的思考','首页列表缓存 30 秒，好处是抗住并发；坏处是发帖后自己看不到新内容。\n\n最后的做法是写操作后主动 evict，TTL 只当兜底。这样既保证一致性，也不会因为漏删缓存一直脏着。',55,14,'2026-10-28 19:02:15',1,NULL);
/*!40000 ALTER TABLE `post` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (99,'矿洞确实是本体，尤其下雨天不用浇水的时候。',55,83,NULL,'2026-10-28 19:02:37',0);
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (100,'200 小时还没开始建农场，你已经很克制了。',54,83,NULL,'2026-10-28 19:02:37',0);
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (101,'推荐《逃出生天》，剧情向的双人合作。',54,84,NULL,'2026-10-28 19:02:37',0);
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (102,'这个坑我也踩过，官方文档写得太隐蔽了。',55,85,NULL,'2026-10-28 19:02:37',0);
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (103,'写后失效 + 短 TTL，这个组合是对的。',54,86,NULL,'2026-10-28 19:02:37',0);
INSERT INTO `comment` (`id`, `content`, `user_id`, `post_id`, `parent_id`, `created_at`, `like_count`) VALUES (104,'如果并发高的话，建议再叠加一个本地缓存。',55,86,NULL,'2026-10-28 19:02:37',0);
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

