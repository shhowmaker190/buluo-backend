package com.vote.service;

import com.vote.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppConfig appConfig;

    @Value("${spring.mail.username:noreply@tribe.com}")
    private String fromEmail;

    @Async
    public void sendCommentNotificationEmail(String toEmail, String username, String postTitle, Long postId) {
        try {
            String subject = "🔔 您的帖子有新评论！";
            String content = buildCommentNotificationContent(username, postTitle, postId);
            sendHtmlEmail(toEmail, subject, content);
            log.info("评论通知邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送评论通知邮件失败: {}", e.getMessage());
        }
    }

    @Async
    public void sendLikeNotificationEmail(String toEmail, String username, String postTitle, String commentContent, Long postId) {
        try {
            String subject = "👍 有人点赞了您的评论！";
            String content = buildLikeNotificationContent(username, postTitle, commentContent, postId);
            sendHtmlEmail(toEmail, subject, content);
            log.info("点赞通知邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送点赞通知邮件失败: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String buildCommentNotificationContent(String fromUsername, String postTitle, Long postId) {
        String postUrl = appConfig.getBaseUrl() + "/post/" + postId;
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Georgia, serif; background-color: #f5ead6; padding: 20px; }
                    .container { background: #fdf8e8; border: 2px solid #a67c52; border-radius: 8px; padding: 30px; max-width: 600px; margin: 0 auto; box-shadow: 8px 8px 16px rgba(60, 35, 15, 0.2); }
                    h1 { color: #5a3a1a; border-bottom: 2px solid #8b5a2b; padding-bottom: 10px; font-size: 24px; }
                    .content { color: #3d2314; line-height: 1.8; margin: 20px 0; }
                    .button { display: inline-block; background: linear-gradient(180deg, #b8860b 0%, #a07009 100%); color: #fffacd; padding: 12px 30px; border-radius: 25px; text-decoration: none; box-shadow: 2px 2px 6px rgba(0,0,0,0.3); }
                    .footer { color: #8b7355; font-size: 12px; margin-top: 30px; text-align: center; border-top: 1px solid #c4a87a; padding-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🔔 新评论通知</h1>
                    <div class="content">
                        <p>亲爱的用户，</p>
                        <p><strong>%s</strong> 刚刚评论了您的帖子：</p>
                        <p style="font-size: 18px; color: #5a3a1a; font-weight: bold;">《%s》</p>
                        <p>快去看看楼主的最新动态吧！</p>
                    </div>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">查看详情</a>
                    </div>
                    <div class="footer">
                        <p>此邮件由部落闲聊自动发送，请勿回复</p>
                        <p>© 2024 部落闲聊</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fromUsername, postTitle, postUrl);
    }

    private String buildLikeNotificationContent(String fromUsername, String postTitle, String commentContent, Long postId) {
        String postUrl = appConfig.getBaseUrl() + "/post/" + postId;
        String truncatedComment = commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent;
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Georgia, serif; background-color: #f5ead6; padding: 20px; }
                    .container { background: #fdf8e8; border: 2px solid #a67c52; border-radius: 8px; padding: 30px; max-width: 600px; margin: 0 auto; box-shadow: 8px 8px 16px rgba(60, 35, 15, 0.2); }
                    h1 { color: #5a3a1a; border-bottom: 2px solid #8b5a2b; padding-bottom: 10px; font-size: 24px; }
                    .content { color: #3d2314; line-height: 1.8; margin: 20px 0; }
                    .quote { background: #f5e4cc; border-left: 4px solid #8b5a2b; padding: 10px 15px; margin: 15px 0; font-style: italic; }
                    .button { display: inline-block; background: linear-gradient(180deg, #b8860b 0%, #a07009 100%); color: #fffacd; padding: 12px 30px; border-radius: 25px; text-decoration: none; box-shadow: 2px 2px 6px rgba(0,0,0,0.3); }
                    .footer { color: #8b7355; font-size: 12px; margin-top: 30px; text-align: center; border-top: 1px solid #c4a87a; padding-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>👍 新点赞通知</h1>
                    <div class="content">
                        <p>亲爱的用户，</p>
                        <p><strong>%s</strong> 点赞了您在帖子 <strong>"%s"</strong> 中的评论：</p>
                        <div class="quote">"%s"</div>
                        <p>感谢您的精彩评论！</p>
                    </div>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">查看详情</a>
                    </div>
                    <div class="footer">
                        <p>此邮件由部落闲聊自动发送，请勿回复</p>
                        <p>© 2024 部落闲聊</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fromUsername, postTitle, truncatedComment, postUrl);
    }
}
