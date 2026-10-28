package com.vote.controller;

import com.vote.common.Result;
import com.vote.util.ImageCompressor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Value("${app.upload-path:uploads}")
    private String uploadPath;

    @Value("${app.upload.max-avatar-size:10485760}")
    private long maxAvatarSize;

    @Value("${app.upload.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${app.upload.avatar-max-width:512}")
    private int avatarMaxWidth;

    @Value("${app.upload.image-max-width:1280}")
    private int imageMaxWidth;

    @Value("${app.upload.quality:0.8}")
    private float quality;

    private static final Set<String> ALLOWED_EXT =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /** 上传根目录（启动时解析为绝对路径，避免把文件写进 Tomcat 临时目录） */
    private Path rootDir;

    @PostConstruct
    public void init() {
        rootDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
            log.info("上传文件根目录: {}", rootDir);
        } catch (IOException e) {
            log.error("创建上传根目录失败: {}", rootDir, e);
        }
    }

    @PostMapping("/avatar")
    public Result<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.error(400, "文件不能为空");
        if (file.getSize() > maxAvatarSize) {
            return Result.error(400, "头像不能超过 " + (maxAvatarSize / 1024 / 1024) + "MB");
        }
        try {
            // 用户头像多为人物照，偏上裁剪，避免切到脸
            return Result.success("上传成功",
                    saveFile(file, "avatars", avatarMaxWidth, ImageCompressor.CropMode.SQUARE_TOP));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 部落头像：图案类居多，居中裁剪，列表/成员栏用固定尺寸 + cover 显示 */
    @PostMapping("/tribe-avatar")
    public Result<?> uploadTribeAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.error(400, "文件不能为空");
        if (file.getSize() > maxAvatarSize) {
            return Result.error(400, "头像不能超过 " + (maxAvatarSize / 1024 / 1024) + "MB");
        }
        try {
            return Result.success("上传成功",
                    saveFile(file, "tribes", avatarMaxWidth, ImageCompressor.CropMode.SQUARE_CENTER));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.error(400, "文件不能为空");
        if (file.getSize() > maxImageSize) {
            return Result.error(400, "图片不能超过 " + (maxImageSize / 1024 / 1024) + "MB");
        }
        try {
            // 帖子正文与评论配图：等比缩放，不裁剪
            return Result.success("上传成功",
                    saveFile(file, "images", imageMaxWidth, ImageCompressor.CropMode.NONE));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    private Map<String, String> saveFile(MultipartFile file, String subDir, int maxWidth,
                                        ImageCompressor.CropMode mode) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只允许上传图片文件");
        }
        String ext = resolveExt(file.getOriginalFilename());
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("不支持的图片格式，仅支持 jpg/png/gif/webp/bmp");
        }

        String baseName = UUID.randomUUID().toString().replace("-", "");
        try {
            Path dir = rootDir.resolve(subDir).normalize();
            Files.createDirectories(dir);

            // 先落盘原图再压缩；压缩失败或格式不支持时保留原图
            Path origin = dir.resolve(baseName + ext);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, origin, StandardCopyOption.REPLACE_EXISTING);
            }

            Path saved = origin;
            // gif 压缩会丢掉动画帧，动图保留原样
            if (!".gif".equals(ext)) {
                Path compressed = ImageCompressor.compress(origin, dir, baseName, maxWidth, quality, mode);
                if (compressed != null && Files.exists(compressed)) {
                    // 压缩输出是 baseName + ".jpg"/".png"，当原图扩展名恰好相同时
                    // compressed 就是 origin，此时绝不能删，否则刚写好的文件会被自己删掉
                    if (!samePath(compressed, origin)) {
                        Files.deleteIfExists(origin);
                    }
                    saved = compressed;
                }
            }
            log.info("文件已保存: {}", saved);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + subDir + "/" + saved.getFileName().toString());
            return result;
        } catch (IOException e) {
            log.error("文件保存失败, 目录: {}", rootDir.resolve(subDir), e);
            throw new RuntimeException("文件保存失败");
        }
    }

    private static boolean samePath(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    /** 从原始文件名中提取并校验扩展名，非法或无扩展名时返回空串 */
    private String resolveExt(String original) {
        if (original == null) return "";
        int idx = original.lastIndexOf('.');
        if (idx < 0 || idx == original.length() - 1) return "";
        String ext = original.substring(idx).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        return ALLOWED_EXT.contains(ext) ? ext : "";
    }
}
