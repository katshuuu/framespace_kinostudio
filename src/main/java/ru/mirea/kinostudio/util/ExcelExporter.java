package ru.mirea.kinostudio.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.ShootingRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Экспорт данных системы в Excel (.xlsx).
 */
public final class ExcelExporter {

    private ExcelExporter() {
    }

    public static void exportRequests(List<ShootingRequest> requests, List<Client> clients,
                                       List<Location> locations, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {

            // Лист "Заявки"
            Sheet requestSheet = workbook.createSheet("Заявки на съёмку");
            String[] reqHeaders = {"ID", "Клиент ID", "Локация ID", "Название", "Описание",
                    "Дата съёмки", "Длительность (ч)", "Бюджет", "Статус", "Создана"};
            writeHeader(workbook, requestSheet, reqHeaders);

            int rowIdx = 1;
            for (ShootingRequest r : requests) {
                Row row = requestSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getClientId());
                row.createCell(2).setCellValue(r.getLocationId());
                row.createCell(3).setCellValue(r.getTitle());
                row.createCell(4).setCellValue(r.getDescription());
                row.createCell(5).setCellValue(r.getShootingDate().toString());
                row.createCell(6).setCellValue(r.getDurationHours());
                row.createCell(7).setCellValue(r.getBudget().doubleValue());
                row.createCell(8).setCellValue(r.getStatus().name());
                row.createCell(9).setCellValue(r.getCreatedAt().toString());
            }
            autoSizeColumns(requestSheet, reqHeaders.length);

            // Лист "Клиенты"
            Sheet clientSheet = workbook.createSheet("Клиенты");
            String[] clientHeaders = {"ID", "ФИО", "Email", "Телефон", "Дата регистрации"};
            writeHeader(workbook, clientSheet, clientHeaders);

            int cRowIdx = 1;
            for (Client c : clients) {
                Row row = clientSheet.createRow(cRowIdx++);
                row.createCell(0).setCellValue(c.getId());
                row.createCell(1).setCellValue(c.getFullName());
                row.createCell(2).setCellValue(c.getEmail());
                row.createCell(3).setCellValue(c.getPhone());
                row.createCell(4).setCellValue(c.getCreatedAt().toString());
            }
            autoSizeColumns(clientSheet, clientHeaders.length);

            // Лист "Локации"
            Sheet locationSheet = workbook.createSheet("Локации");
            String[] locHeaders = {"ID", "Название", "Адрес", "Вместимость", "Тип", "Дата добавления"};
            writeHeader(workbook, locationSheet, locHeaders);

            int lRowIdx = 1;
            for (Location l : locations) {
                Row row = locationSheet.createRow(lRowIdx++);
                row.createCell(0).setCellValue(l.getId());
                row.createCell(1).setCellValue(l.getName());
                row.createCell(2).setCellValue(l.getAddress());
                row.createCell(3).setCellValue(l.getCapacity());
                row.createCell(4).setCellValue(l.getType().name());
                row.createCell(5).setCellValue(l.getCreatedAt().toString());
            }
            autoSizeColumns(locationSheet, locHeaders.length);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private static void writeHeader(Workbook workbook, Sheet sheet, String[] headers) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
