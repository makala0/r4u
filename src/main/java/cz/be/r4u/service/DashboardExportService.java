package cz.be.r4u.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public void exportDashboardRowsToCsv(Path targetPath, List<DashboardService.DashboardRow> rows) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Datum a cas,Order number,Min,Status,Pocet defektu\n");

        for (DashboardService.DashboardRow row : rows) {
            builder.append(escapeCsv(formatDateTime(row.createdAt()))).append(',')
                    .append(escapeCsv(valueOrBlank(row.orderNumber()))).append(',')
                    .append(escapeCsv(valueOrBlank(row.min()))).append(',')
                    .append(escapeCsv(row.status() == null ? "" : row.status().name())).append(',')
                    .append(escapeCsv(valueOrBlank(row.defectsCount()))).append('\n');
        }

        Files.writeString(targetPath, builder.toString(), StandardCharsets.UTF_8);
    }

    public void exportDashboardRowsToXlsx(
            Path targetPath,
            List<DashboardService.DashboardRow> rows,
            List<DashboardService.DefectRow> defects
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeDashboardSheet(workbook, rows);
            writeDefectsSheet(workbook, defects);

            try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeDashboardSheet(Workbook workbook, List<DashboardService.DashboardRow> rows) {
        Sheet sheet = workbook.createSheet("Dashboard");
        CellStyle headerStyle = createHeaderStyle(workbook);
        Row header = sheet.createRow(0);
        String[] columns = {"Datum a cas", "Order number", "Min", "Status", "Pocet defektu"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (DashboardService.DashboardRow rowData : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(formatDateTime(rowData.createdAt()));
            row.createCell(1).setCellValue(valueOrBlank(rowData.orderNumber()));
            row.createCell(2).setCellValue(valueOrBlank(rowData.min()));
            row.createCell(3).setCellValue(rowData.status() == null ? "" : rowData.status().name());
            row.createCell(4).setCellValue(valueOrBlank(rowData.defectsCount()));
        }

        autoSizeColumns(sheet, columns.length);
    }

    private void writeDefectsSheet(Workbook workbook, List<DashboardService.DefectRow> defects) {
        Sheet sheet = workbook.createSheet("Defekty");
        CellStyle headerStyle = createHeaderStyle(workbook);
        Row header = sheet.createRow(0);
        String[] columns = {
                "Datum a cas", "Typ defektu", "Kamera", "Cislo bobiny", "Pozice v navinu",
                "Plocha defektu", "Velikost", "Vyrazeno", "Poloha", "Klasifikace", "Ma snimek"
        };
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (DashboardService.DefectRow defect : defects) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(formatDateTime(defect.createdAt()));
            row.createCell(1).setCellValue(defect.type() == null ? "" : defect.type().name());
            row.createCell(2).setCellValue(valueOrBlank(defect.camera()));
            row.createCell(3).setCellValue(valueOrBlank(defect.bobinaColumnNumber()));
            row.createCell(4).setCellValue(formatDecimal(defect.positionInRoll()));
            row.createCell(5).setCellValue(formatDecimal(defect.defectArea()));
            row.createCell(6).setCellValue(defect.sizeClass() == null ? "" : defect.sizeClass().name());
            row.createCell(7).setCellValue(defect.reject() ? "ANO" : "NE");
            row.createCell(8).setCellValue(valueOrBlank(defect.defectLocation()));
            row.createCell(9).setCellValue(valueOrBlank(defect.classification()));
            row.createCell(10).setCellValue(defect.hasImage() ? "ANO" : "NE");
        }

        autoSizeColumns(sheet, columns.length);
    }

    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        boolean mustQuote = safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n");
        if (!mustQuote) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatDecimal(Double value) {
        return value == null ? "" : DECIMAL_FORMAT.format(value);
    }

    private String valueOrBlank(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
