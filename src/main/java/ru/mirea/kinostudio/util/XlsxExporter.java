package ru.mirea.kinostudio.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.kinostudio.model.RequestView;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Экспорт того, что сейчас показано в таблице заявок, в файл .xlsx (Apache POI).
 * В отличие от ExcelExporter из КР №1 здесь в файле имена клиента и локации, а не их id.
 */
public final class XlsxExporter {

    private XlsxExporter() {
    }

    public static void exportRequests(List<RequestView> views, Path file) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("Заявки на съёмку");

            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            String[] headers = {"ID", "Название", "Клиент", "Email клиента", "Локация", "Дата съёмки",
                    "Длительность (ч)", "Бюджет", "Статус", "Описание"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(head);
            }

            int r = 1;
            for (RequestView v : views) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(v.getId());
                row.createCell(1).setCellValue(v.getTitle());
                row.createCell(2).setCellValue(v.getClientName());
                row.createCell(3).setCellValue(v.getClient() == null ? "" : v.getClient().getEmail());
                row.createCell(4).setCellValue(v.getLocationName());
                row.createCell(5).setCellValue(Labels.date(v.getShootingDate()));
                row.createCell(6).setCellValue(v.getDurationHours());
                Cell budget = row.createCell(7);
                budget.setCellValue(v.getBudget().doubleValue());
                budget.setCellStyle(money);
                row.createCell(8).setCellValue(Labels.status(v.getStatus()));
                row.createCell(9).setCellValue(v.getDescription());
            }
            for (int i = 0; i < headers.length - 1; i++) sheet.autoSizeColumn(i);
            sheet.setColumnWidth(9, 60 * 256);
            sheet.createFreezePane(0, 1);
            wb.write(out);
        }
    }
}
