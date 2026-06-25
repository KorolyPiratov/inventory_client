package inventory_client.service;

import inventory_client.model.Issuance;
import inventory_client.model.Item;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExcelService {

    // ===== ЭКСПОРТ СКЛАДА =====
    public static void exportItems(List<Item> items, File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Склад");

            // Стиль заголовка
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Заголовки
            Row header = sheet.createRow(0);
            String[] cols = {"Название", "Категория", "Цвет", "№ Коробки",
                    "Дата поставки", "Количество", "Описание"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowNum = 1;
            for (Item item : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getName() != null ? item.getName() : "");
                row.createCell(1).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                row.createCell(2).setCellValue(item.getColorType() != null ? item.getColorType() : "");
                row.createCell(3).setCellValue(item.getBoxNumber() != null ? item.getBoxNumber() : "");
                row.createCell(4).setCellValue(item.getSupplyDate() != null ? item.getSupplyDate().toString() : "");
                row.createCell(5).setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                row.createCell(6).setCellValue(item.getDescription() != null ? item.getDescription() : "");
            }

            // Авторазмер колонок
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    // ===== ЭКСПОРТ АРХИВА =====
    public static void exportArchive(List<Issuance> issuances, File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Архив выдач");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] cols = {"Вещь", "Кому выдано", "Дата выдачи", "Бессрочно", "Дата возврата"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Issuance iss : issuances) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(iss.getItemName() != null ? iss.getItemName() : "");
                row.createCell(1).setCellValue(iss.getFullName() != null ? iss.getFullName() : "");
                row.createCell(2).setCellValue(iss.getIssuedAt() != null ? iss.getIssuedAt() : "");
                row.createCell(3).setCellValue(iss.getIsIndefinite() != null && iss.getIsIndefinite() ? "Да" : "Нет");
                row.createCell(4).setCellValue(iss.getReturnDate() != null ? iss.getReturnDate().toString() : "");
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    // ===== ИМПОРТ СКЛАДА =====
    public static List<Item> importItems(File file) throws Exception {
        List<Item> items = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);

            // Пропускаем заголовок (строка 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellString(row, 0);
                if (name.isEmpty()) continue; // пропускаем пустые строки

                Item item = new Item();
                item.setName(name);
                item.setCategory(getCellString(row, 1));
                item.setColorType(getCellString(row, 2));
                item.setBoxNumber(getCellString(row, 3));

                String dateStr = getCellString(row, 4);
                if (!dateStr.isEmpty()) {
                    try { item.setSupplyDate(LocalDate.parse(dateStr)); }
                    catch (Exception ignored) {}
                }

                String qtyStr = getCellString(row, 5);
                try { item.setQuantity((int) Double.parseDouble(qtyStr)); }
                catch (Exception e) { item.setQuantity(0); }

                item.setDescription(getCellString(row, 6));
                items.add(item);
            }
        }
        return items;
    }

    private static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}