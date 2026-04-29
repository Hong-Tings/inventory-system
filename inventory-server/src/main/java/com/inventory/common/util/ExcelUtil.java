package com.inventory.common.util;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExcelUtil {

    /**
     * 导出 Excel 文件
     *
     * @param response HttpServletResponse
     * @param data     数据列表
     * @param fileName 文件名（不含扩展名）
     * @param clazz    Excel 行模型 Class（标注 @ExcelProperty）
     */
    public static void export(HttpServletResponse response, List<?> data,
                              String fileName, Class<?> clazz) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition",
                    "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), clazz)
                    .sheet("Sheet1")
                    .doWrite(data);
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }
}
