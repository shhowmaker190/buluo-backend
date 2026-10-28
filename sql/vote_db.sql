
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

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `vote_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `vote_db`;
DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `msg_type` varchar(16) NOT NULL DEFAULT 'text' COMMENT '娑堟伅绫诲瀷: text/post/tribe',
  `ref_id` bigint DEFAULT NULL COMMENT '鍒嗕韩瀵硅薄ID(甯栧瓙/閮ㄨ惤)',
  `ref_title` varchar(200) DEFAULT NULL COMMENT '鍒嗕韩鍗＄墖鏍囬?',
  `ref_sub` varchar(200) DEFAULT NULL COMMENT '鍒嗕韩鍗＄墖鍓?爣棰',
  `ref_avatar` varchar(255) DEFAULT NULL COMMENT '鍒嗕韩鍗＄墖澶村儚',
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `from_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '鍙戦?鏂瑰凡鍒犻櫎(浠呰嚜宸变笉鍙??)',
  `to_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '鎺ユ敹鏂瑰凡鍒犻櫎(浠呰嚜宸变笉鍙??)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conversation` (`from_user_id`,`to_user_id`,`created_at`),
  KEY `idx_to_read` (`to_user_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绉佽亰娑堟伅';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `checkin_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checkin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `checkin_date` date NOT NULL,
  `exp` int NOT NULL DEFAULT '5' COMMENT '鏈??绛惧埌鑾峰緱鐨勭粡楠岋紙鍚?繛缁??鍔憋級',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`checkin_date`),
  KEY `idx_user_date` (`user_id`,`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='姣忔棩绛惧埌璁板綍';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `like_count` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_comment_user` (`user_id`),
  KEY `fk_comment_vote` (`post_id`),
  KEY `idx_comment_post` (`post_id`),
  KEY `idx_comment_parent` (`parent_id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_comment_vote` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comment_like_ibfk_1` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_like_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comment_restriction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_restriction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `admin_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_vote_restrict` (`user_id`,`post_id`),
  KEY `fk_restrict_user` (`user_id`),
  KEY `fk_restrict_vote` (`post_id`),
  KEY `fk_restrict_admin` (`admin_id`),
  CONSTRAINT `fk_restrict_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_restrict_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_restrict_vote` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鏈?汉ID',
  `friend_id` bigint NOT NULL COMMENT '濂藉弸ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='濂藉弸鍏崇郴锛屼簰涓哄ソ鍙嬫椂鍙屽悜鍚勫瓨涓?潯';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `friend_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL COMMENT '鐢宠?浜',
  `to_user_id` bigint NOT NULL COMMENT '琚?敵璇蜂汉',
  `message` varchar(200) DEFAULT NULL COMMENT '鐢宠?鐣欒█',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0寰呭?鐞?1宸插悓鎰?2宸叉嫆缁',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_to_status` (`to_user_id`,`status`),
  KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='濂藉弸鐢宠?';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `name_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `name_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `old_name` varchar(50) NOT NULL,
  `new_name` varchar(50) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=寰呭?鏍?1=閫氳繃 2=鎷掔粷',
  `reject_reason` varchar(200) DEFAULT NULL,
  `handler_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `handled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name_req_status` (`status`,`created_at`),
  KEY `idx_name_req_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鏀瑰悕鐢宠?';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '通知接收者ID',
  `post_id` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL COMMENT '关联的评论ID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知类型：VOTE、LIKE',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '通知内容',
  `from_username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发通知的用户名',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `email_sent` tinyint(1) DEFAULT '0' COMMENT '邮件是否已发送',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'POST/CHAT/FRIEND',
  `biz_id` bigint DEFAULT NULL COMMENT '璺宠浆鐩?爣ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_vote_id` (`post_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `user_id` bigint NOT NULL,
  `tribe_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` tinyint DEFAULT '1',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vote_user` (`user_id`),
  KEY `idx_post_status_created` (`status`,`created_at`),
  KEY `idx_post_tribe` (`tribe_id`),
  CONSTRAINT `fk_vote_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL COMMENT '涓炬姤浜',
  `target_type` varchar(16) NOT NULL COMMENT 'POST / TRIBE',
  `target_id` bigint NOT NULL COMMENT '琚?妇鎶ュ?璞?D',
  `reason` varchar(32) NOT NULL COMMENT '涓炬姤鍘熷洜绫诲埆',
  `detail` varchar(500) DEFAULT NULL COMMENT '琛ュ厖璇存槑',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=寰呭?鐞?1=宸蹭笅鏋跺唴瀹?2=椹冲洖',
  `handler_id` bigint DEFAULT NULL COMMENT '澶勭悊浜',
  `handle_note` varchar(200) DEFAULT NULL COMMENT '澶勭悊璇存槑',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `handled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`status`,`created_at`),
  KEY `idx_report_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='涓炬姤璁板綍';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tribe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tribe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `creator_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=寰呭?鏍?1=宸查?杩?2=宸叉嫆缁',
  `reject_reason` varchar(255) DEFAULT NULL,
  `pending_name` varchar(50) DEFAULT NULL,
  `pending_description` varchar(200) DEFAULT NULL,
  `pending_avatar` varchar(255) DEFAULT NULL,
  `pending_status` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tribe_name` (`name`),
  KEY `idx_tribe_status` (`status`),
  KEY `idx_tribe_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tribe_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tribe_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tribe_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'MEMBER',
  `muted` tinyint NOT NULL DEFAULT '0' COMMENT '1=鍦ㄨ?閮ㄨ惤鍐呰?绂佽█',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tribe_user` (`tribe_id`,`user_id`),
  KEY `idx_member_user` (`user_id`),
  KEY `idx_member_tribe_role` (`tribe_id`,`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `status` tinyint DEFAULT '1',
  `posts_public` tinyint NOT NULL DEFAULT '1',
  `exp` int NOT NULL DEFAULT '0' COMMENT '绛惧埌鑾峰緱鐨勯?澶栫粡楠岋紝鐩存帴鍙犲姞鍒扮瓑绾ф椿璺冨?',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `user_no` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢ㄦ埛鍞?竴6浣岻D',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_user_no` (`user_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

