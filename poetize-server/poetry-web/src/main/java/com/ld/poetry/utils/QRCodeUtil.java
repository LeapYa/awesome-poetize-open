package com.ld.poetry.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具类
 * 支持生成带Logo的二维码
 */
@Slf4j
public class QRCodeUtil {

    /**
     * 默认二维码宽度
     */
    private static final int DEFAULT_WIDTH = 300;

    /**
     * 默认二维码高度
     */
    private static final int DEFAULT_HEIGHT = 300;

    /**
     * Logo默认宽度
     */
    private static final int DEFAULT_LOGO_WIDTH = 60;

    /**
     * Logo默认高度
     */
    private static final int DEFAULT_LOGO_HEIGHT = 60;

    /**
     * 生成二维码（不带Logo）
     *
     * @param content 二维码内容
     * @param width   二维码宽度
     * @param height  二维码高度
     * @return 二维码图片字节数组
     */
    public static byte[] generateQRCode(String content, int width, int height) throws WriterException, IOException {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }

        // 设置二维码参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // 设置编码格式
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 设置容错等级（L < M < Q < H）
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 设置边距
        hints.put(EncodeHintType.MARGIN, 1);

        // 生成二维码
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        // 转换为BufferedImage - 使用纯黑色前景和透明背景
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix,
                new MatrixToImageConfig(0xFF000000, 0x00FFFFFF));

        // 转换为字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 生成带Logo的二维码
     *
     * @param content 二维码内容
     * @param width   二维码宽度
     * @param height  二维码高度
     * @param logoUrl Logo图片URL
     * @return 二维码图片字节数组
     */
    public static byte[] generateQRCodeWithLogo(String content, int width, int height, String logoUrl) 
            throws WriterException, IOException {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }

        // 生成基础二维码
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        // 使用纯黑色前景和透明背景
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix,
                new MatrixToImageConfig(0xFF000000, 0x00FFFFFF));

        // 添加Logo
        if (StringUtils.hasText(logoUrl)) {
            try {
                log.info("准备添加Logo到二维码，Logo URL: {}", logoUrl);
                qrImage = addLogoToQRCode(qrImage, logoUrl, DEFAULT_LOGO_WIDTH, DEFAULT_LOGO_HEIGHT);
                log.info("Logo添加成功");
            } catch (Exception e) {
                log.error("添加Logo失败，将返回不带Logo的二维码: {}", e.getMessage(), e);
            }
        } else {
            log.info("logoUrl为空，生成不带Logo的二维码");
        }

        // 转换为字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 向二维码中添加Logo
     *
     * @param qrImage    二维码图片
     * @param logoUrl    Logo图片URL
     * @param logoWidth  Logo宽度
     * @param logoHeight Logo高度
     * @return 添加Logo后的二维码图片
     */
    private static BufferedImage addLogoToQRCode(BufferedImage qrImage, String logoUrl, 
                                                  int logoWidth, int logoHeight) throws IOException {
        // 读取Logo图片
        BufferedImage logo = readLogoImage(logoUrl);
        if (logo == null) {
            return qrImage;
        }

        // 获取二维码图片的宽高
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();

        // 创建Graphics2D对象
        Graphics2D g2d = qrImage.createGraphics();

        // 计算Logo的位置（居中）
        int x = (qrWidth - logoWidth) / 2;
        int y = (qrHeight - logoHeight) / 2;

        // 绘制白色背景（Logo周围留白）
        int margin = 5;
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(x - margin, y - margin, 
                         logoWidth + 2 * margin, logoHeight + 2 * margin, 15, 15);

        // 绘制Logo
        g2d.drawImage(logo, x, y, logoWidth, logoHeight, null);

        // 绘制Logo边框
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.WHITE);
        g2d.draw(new RoundRectangle2D.Float(x - margin, y - margin, 
                                            logoWidth + 2 * margin, logoHeight + 2 * margin, 15, 15));

        g2d.dispose();
        logo.flush();

        return qrImage;
    }

    /**
     * 读取Logo图片
     *
     * @param logoUrl Logo图片URL
     * @return Logo图片
     */
    private static BufferedImage readLogoImage(String logoUrl) {
        try {
            if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
                // 从URL读取，支持HTTP重定向
                log.info("从URL读取Logo图片: {}", logoUrl);
                
                // 如果是HTTP，先尝试HTTPS（更安全且避免重定向问题）
                String actualUrl = logoUrl;
                if (logoUrl.startsWith("http://")) {
                    String httpsUrl = logoUrl.replace("http://", "https://");
                    try {
                        BufferedImage httpsImage = readImageWithRedirect(httpsUrl);
                        if (httpsImage != null) {
                            log.info("HTTPS图片读取成功，尺寸: {}x{}", httpsImage.getWidth(), httpsImage.getHeight());
                            return httpsImage;
                        }
                    } catch (Exception e) {
                    }
                }
                
                // 使用原始URL或HTTP协议读取
                BufferedImage image = readImageWithRedirect(actualUrl);
                if (image != null) {
                    log.info("Logo图片读取成功，尺寸: {}x{}", image.getWidth(), image.getHeight());
                } else {
                    log.error("Logo图片读取失败，ImageIO.read返回null");
                }
                return image;
            } else {
                // 从本地文件读取（暂不支持）
                log.warn("暂不支持本地文件路径的Logo: {}", logoUrl);
                return null;
            }
        } catch (Exception e) {
            log.error("读取Logo图片失败: {}, 异常: {}", logoUrl, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 读取图片并处理HTTP重定向
     */
    private static BufferedImage readImageWithRedirect(String imageUrl) throws IOException {
        java.net.HttpURLConnection connection = null;
        try {
            URL url = URI.create(imageUrl).toURL();
            connection = (java.net.HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true); // 自动跟随重定向
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (POETIZE QRCode Generator)");
            
            // 连接并获取响应码
            int responseCode = connection.getResponseCode();
            
            // 处理重定向（301, 302, 303, 307, 308）
            if (responseCode >= 300 && responseCode < 400) {
                String newUrl = connection.getHeaderField("Location");
                connection.disconnect();
                return readImageWithRedirect(newUrl); // 递归处理重定向
            }
            
            if (responseCode == 200) {
                try (InputStream inputStream = connection.getInputStream()) {
                    return ImageIO.read(inputStream);
                }
            } else {
                log.warn("HTTP响应码异常: {}, URL: {}", responseCode, imageUrl);
                return null;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 生成带Logo的二维码（使用默认尺寸）
     *
     * @param content 二维码内容
     * @param logoUrl Logo图片URL
     * @return 二维码图片字节数组
     */
    public static byte[] generateQRCodeWithLogo(String content, String logoUrl) 
            throws WriterException, IOException {
        return generateQRCodeWithLogo(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, logoUrl);
    }
}

