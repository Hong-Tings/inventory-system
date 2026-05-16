package com.inventory.product.controller;

import cn.hutool.core.util.StrUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name = "商品条码")
@RestController
@RequestMapping("/api/v1/product/barcode")
@RequiredArgsConstructor
public class BarcodeController {

    private final ProductMapper productMapper;

    private static final int BARCODE_WIDTH = 600;
    private static final int BARCODE_HEIGHT = 140;
    private static final int TEXT_HEIGHT = 65;

    @Operation(summary = "批量生成商品条码（ZIP下载）")
    @PostMapping("/download")
    public void downloadBarcodes(@RequestBody List<Long> ids, HttpServletResponse response) {
        List<Product> products = productMapper.selectBatchIds(ids);
        if (products.isEmpty()) return;

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("商品条码.zip", StandardCharsets.UTF_8));

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            MultiFormatWriter writer = new MultiFormatWriter();
            for (Product p : products) {
                String code = p.getCode();
                String name = p.getName();
                String spec = p.getSpec();
                if (StrUtil.isBlank(code)) continue;

                // 文件名：编码_名称_规格.png（去除非法字符）
                String fileNameCode = code.replaceAll("[/\\\\:*?\"<>|]", "");
                String fileNameName = StrUtil.blankToDefault(name, "").replaceAll("[/\\\\:*?\"<>|]", "");
                String fileNameSpec = StrUtil.blankToDefault(spec, "").replaceAll("[/\\\\:*?\"<>|]", "");
                if (fileNameName.length() > 10) fileNameName = fileNameName.substring(0, 10);
                if (fileNameSpec.length() > 8) fileNameSpec = fileNameSpec.substring(0, 8);
                String fileName = fileNameCode
                        + (fileNameName.isEmpty() ? "" : "_" + fileNameName)
                        + (fileNameSpec.isEmpty() ? "" : "_" + fileNameSpec)
                        + ".png";
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(generateBarcodeImage(code, name, spec));
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("生成条码失败", e);
        }
    }

    @Operation(summary = "单个商品条码（直接显示）")
    @GetMapping("/{id}")
    public void getBarcode(@PathVariable Long id, HttpServletResponse response) {
        Product product = productMapper.selectById(id);
        if (product == null || StrUtil.isBlank(product.getCode())) return;

        response.setContentType("image/png");
        try (OutputStream os = response.getOutputStream()) {
            os.write(generateBarcodeImage(product.getCode(), product.getName(), product.getSpec()));
        } catch (Exception e) {
            throw new RuntimeException("生成条码失败", e);
        }
    }

    private byte[] generateBarcodeImage(String code, String name, String spec) throws Exception {
        int totalHeight = BARCODE_HEIGHT + TEXT_HEIGHT;

        BitMatrix matrix = new MultiFormatWriter().encode(code, BarcodeFormat.CODE_128, BARCODE_WIDTH, BARCODE_HEIGHT);
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(matrix);

        BufferedImage result = new BufferedImage(BARCODE_WIDTH, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        // 白色背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, BARCODE_WIDTH, totalHeight);

        // 绘制条码
        g.drawImage(barcodeImage, 0, 0, null);

        // 绘制底部文字
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 第一行：编码（大字）
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int textX = (BARCODE_WIDTH - fm.stringWidth(code)) / 2;
        g.drawString(code, textX, BARCODE_HEIGHT + 26);

        // 第二行：名称 + 规格（中字）
        String info = StrUtil.blankToDefault(name, "");
        if (StrUtil.isNotBlank(spec)) {
            info += "  " + spec;
        }
        if (StrUtil.isNotBlank(info)) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            fm = g.getFontMetrics();
            // 截断过长文字
            if (fm.stringWidth(info) > BARCODE_WIDTH - 20) {
                while (fm.stringWidth(info + "…") > BARCODE_WIDTH - 20 && info.length() > 1) {
                    info = info.substring(0, info.length() - 1);
                }
                info += "…";
            }
            textX = (BARCODE_WIDTH - fm.stringWidth(info)) / 2;
            g.drawString(info, textX, BARCODE_HEIGHT + 54);
        }

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(result, "PNG", baos);
        return baos.toByteArray();
    }
}
