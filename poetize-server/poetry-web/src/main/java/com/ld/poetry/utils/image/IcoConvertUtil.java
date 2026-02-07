package com.ld.poetry.utils.image;

import lombok.extern.slf4j.Slf4j;
import net.sf.image4j.codec.ico.ICOEncoder;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 网站图标转 ICO 工具
 * 支持 PNG、JPEG、GIF、BMP、WebP 等格式上传后统一转为 ICO，便于浏览器与收藏夹兼容
 */
@Slf4j
public class IcoConvertUtil {

    private static final int ICO_SIZE_32 = 32;
    private static final int ICO_SIZE_16 = 16;

    /**
     * 将上传的图片转为 ICO 格式字节数组
     * 支持 PNG、JPEG、GIF、BMP；WebP 需系统已安装 dwebp 命令
     *
     * @param file 上传的图片文件
     * @return ICO 格式字节数组，转换失败返回 null
     */
    public static byte[] convertToIco(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        try {
            BufferedImage image = readImage(file, contentType, name);
            if (image == null) {
                return null;
            }
            // 生成 16x16 和 32x32 两个尺寸，兼容不同场景
            BufferedImage size32 = resize(image, ICO_SIZE_32, ICO_SIZE_32);
            BufferedImage size16 = resize(image, ICO_SIZE_16, ICO_SIZE_16);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ICOEncoder.write(java.util.List.of(size16, size32), baos);
                byte[] icoBytes = baos.toByteArray();
                log.info("网站图标已转为 ICO，原始格式: {}, 输出大小: {} bytes", contentType, icoBytes.length);
                return icoBytes;
            }
        } catch (Exception e) {
            log.warn("网站图标转 ICO 失败: {} - {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * 读取图片为 BufferedImage，支持 WebP（通过 dwebp 命令）
     */
    private static BufferedImage readImage(MultipartFile file, String contentType, String name) throws IOException {
        if (contentType != null && contentType.toLowerCase().contains("webp")) {
            return readWebPViaDwebp(file);
        }
        try (InputStream in = file.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img != null) {
                return img;
            }
        }
        // 部分环境 WebP 的 contentType 可能不准确，再尝试 dwebp
        if (name != null && name.toLowerCase().endsWith(".webp")) {
            return readWebPViaDwebp(file);
        }
        return null;
    }

    /**
     * 通过 dwebp 命令将 WebP 转为 PNG 再读取（与项目 cwebp 方案一致，无 JNI 依赖）
     */
    private static BufferedImage readWebPViaDwebp(MultipartFile file) throws IOException {
        Path webpPath = null;
        Path pngPath = null;
        try {
            webpPath = Files.createTempFile("favicon_", ".webp");
            pngPath = Files.createTempFile("favicon_", ".png");
            Files.write(webpPath, file.getBytes());
            ProcessBuilder pb = new ProcessBuilder("dwebp", webpPath.toAbsolutePath().toString(), "-o", pngPath.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean ok = p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
            if (!ok) {
                p.destroyForcibly();
                log.warn("dwebp 执行超时，无法解码 WebP");
                return null;
            }
            if (p.exitValue() != 0 || !Files.exists(pngPath) || Files.size(pngPath) == 0) {
                log.warn("dwebp 解码失败或未生成文件，exitCode={}", p.exitValue());
                return null;
            }
            return ImageIO.read(pngPath.toFile());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("dwebp 被中断");
            return null;
        } finally {
            if (webpPath != null) {
                try {
                    Files.deleteIfExists(webpPath);
                } catch (IOException ignored) {}
            }
            if (pngPath != null) {
                try {
                    Files.deleteIfExists(pngPath);
                } catch (IOException ignored) {}
            }
        }
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
