package com.vote.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

/**
 * 基于 JDK 自带 ImageIO 的图片压缩工具：等比例缩放 + JPEG 质量压缩。
 * 不引入第三方依赖，避免额外体积；读不出来的格式（如 webp）返回 null，由调用方保存原图。
 */
@Slf4j
public final class ImageCompressor {

    /** 裁剪方式 */
    public enum CropMode {
        /** 不裁剪，完整保留原图宽高比（帖子正文、评论配图） */
        NONE,
        /** 居中裁正方形：适合图案类的部落头像 */
        SQUARE_CENTER,
        /** 偏上裁正方形：用户头像多为人物照，脸在中上部，居中裁容易切到下巴 */
        SQUARE_TOP
    }

    /** 偏上裁剪时，纵向可裁区域中保留在上方的比例 */
    private static final float TOP_RATIO = 0.35f;

    private ImageCompressor() {
    }

    /**
     * 压缩并写入目标目录。
     *
     * @param source   原始图片文件
     * @param dir      目标目录
     * @param baseName 不含扩展名的文件名
     * @param maxWidth 缩放后的最大宽度（原图更小时不放大）
     * @param quality  JPEG 压缩质量 0.1 ~ 1.0
     * @param mode     裁剪方式：NONE 保持比例 / SQUARE_CENTER 居中裁方 / SQUARE_TOP 偏上裁方
     * @return 写入成功返回目标文件路径；格式不支持或压缩失败返回 null
     */
    public static Path compress(Path source, Path dir, String baseName,
                                int maxWidth, float quality, CropMode mode) {
        BufferedImage sourceImage;
        try {
            sourceImage = ImageIO.read(source.toFile());
        } catch (IOException e) {
            log.warn("读取图片失败，保持原图: {}", source, e);
            return null;
        }
        if (sourceImage == null) {
            log.warn("无法解码的图片格式，保持原图: {}", source);
            return null;
        }

        try {
            BufferedImage image = mode == CropMode.NONE ? sourceImage : cropSquare(sourceImage, mode);
            image = resize(image, maxWidth);
            boolean keepAlpha = image.getColorModel().hasAlpha();
            Path target = dir.resolve(baseName + (keepAlpha ? ".png" : ".jpg"));
            // 先写临时文件再整体替换：直接往已存在的同名文件写不会截断，会残留旧字节导致文件虚胖
            Path tmp = Files.createTempFile(dir, baseName, ".tmp");
            boolean written;
            try {
                written = keepAlpha ? ImageIO.write(image, "png", tmp.toFile()) : writeJpeg(image, tmp, quality);
                if (!written) {
                    log.warn("图片写出失败，保持原图: {}", source);
                    return null;
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
            log.info("图片压缩完成: {} -> {} ({} KB -> {} KB)",
                    source.getFileName(), target.getFileName(),
                    sizeInKb(source), sizeInKb(target));
            return target;
        } catch (IOException e) {
            log.warn("图片压缩失败，保持原图: {}", source, e);
            return null;
        }
    }

    /** 以短边为边长裁出正方形；横向居中，纵向按裁剪模式取位 */
    private static BufferedImage cropSquare(BufferedImage image, CropMode mode) {
        int side = Math.min(image.getWidth(), image.getHeight());
        int spare = image.getHeight() - side;
        int x = Math.max(0, (image.getWidth() - side) / 2);
        int y = mode == CropMode.SQUARE_TOP
                ? Math.round(spare * TOP_RATIO)
                : spare / 2;
        // 兜底，防止极端比例下越界导致 getSubimage 抛异常
        y = Math.min(Math.max(y, 0), Math.max(0, image.getHeight() - side));
        return image.getSubimage(x, y, side, side);
    }

    private static BufferedImage resize(BufferedImage image, int maxWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxWidth) return image;

        int targetHeight = Math.max(1, Math.round((float) height * maxWidth / width));
        BufferedImage current = image;
        int currentWidth = width;
        int currentHeight = height;
        // 缩放比例超过 2 倍时先逐级折半，避免一次性降采样产生锯齿
        while (currentWidth / 2 >= maxWidth) {
            currentWidth = currentWidth / 2;
            currentHeight = Math.max(1, currentHeight / 2);
            current = draw(current, currentWidth, currentHeight);
        }
        return draw(current, maxWidth, targetHeight);
    }

    private static BufferedImage draw(BufferedImage source, int width, int height) {
        boolean alpha = source.getColorModel().hasAlpha();
        BufferedImage target = new BufferedImage(width, height,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!alpha) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static boolean writeJpeg(BufferedImage image, Path target, float quality) throws IOException {
        BufferedImage rgb = image.getColorModel().hasAlpha() ? draw(image, image.getWidth(), image.getHeight()) : image;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("当前环境不支持 JPEG 编码");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.1f, Math.min(1f, quality)));
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return true;
    }

    private static long sizeInKb(Path file) {
        try {
            return Math.max(1, file.toFile().length() / 1024);
        } catch (Exception e) {
            return 0;
        }
    }
}
