package com.adu.importer.service;

import com.adu.importer.model.CloseoutRecord;
import com.adu.importer.model.ImportIssue;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SheetParser {

    private static final int PARTIAL_HEADER_THRESHOLD = 2;

    private final DataFormatter formatter = new DataFormatter();

    public Result parse(Sheet sheet) {
        String sheetName = sheet.getSheetName();
        Map<String, String> mergedValues = buildMergedRegionMap(sheet);

        HeaderScan scan = findHeaderRow(sheet, mergedValues);
        if (scan == null) {
            return Result.skipped(sheetName,
                    "No header row found — sheet skipped.");
        }

        if (!scan.missing.isEmpty()) {
            return Result.headerError(sheetName,
                    "Header row at row " + (scan.rowIndex + 1)
                            + " is missing required columns: " + String.join(", ", scan.missing));
        }

        return parseDataRows(sheet, scan, mergedValues);
    }


    private Map<String, String> buildMergedRegionMap(Sheet sheet) {
        Map<String, String> map = new HashMap<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            Row topRow = sheet.getRow(region.getFirstRow());
            if (topRow == null) continue;
            Cell topCell = topRow.getCell(region.getFirstColumn());
            if (topCell == null) continue;
            String value = formatter.formatCellValue(topCell);
            if (value == null || value.isBlank()) continue;
            for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                    map.put(r + ":" + c, value);
                }
            }
        }
        return map;
    }

    private String cellText(Row row, int colIdx, Map<String, String> merged) {
        String merge = merged.get(row.getRowNum() + ":" + colIdx);
        if (merge != null) return merge;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return formatter.formatCellValue(cell);
    }


    private HeaderScan findHeaderRow(Sheet sheet, Map<String, String> merged) {
        HeaderScan best = null;
        int lastRow = sheet.getLastRowNum();
        for (int r = sheet.getFirstRowNum(); r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String, Integer> colIndexByDb = new LinkedHashMap<>();
            int maxCol = row.getLastCellNum();
            for (int c = 0; c < maxCol; c++) {
                String raw = cellText(row, c, merged);
                if (raw == null || raw.isBlank()) continue;
                String normalized = ColumnMapping.normalize(raw);
                String dbCol = ColumnMapping.NORMALIZED_HEADER_TO_DB.get(normalized);
                if (dbCol != null && !colIndexByDb.containsKey(dbCol)) {
                    colIndexByDb.put(dbCol, c);
                }
            }

            int matches = colIndexByDb.size();
            if (matches == 0) continue;

            List<String> missing = new ArrayList<>();
            for (String header : ColumnMapping.HEADER_TO_DB.keySet()) {
                String db = ColumnMapping.HEADER_TO_DB.get(header);
                if (!colIndexByDb.containsKey(db)) missing.add(header);
            }

            HeaderScan candidate = new HeaderScan(r, colIndexByDb, missing);

            if (missing.isEmpty()) {
                return candidate; // perfect header row wins immediately
            }
            if (matches >= PARTIAL_HEADER_THRESHOLD
                    && (best == null || matches > best.matches())) {
                best = candidate;
            }
        }
        return best; // may be null (no candidate) or a partial (missing != empty)
    }


    private Result parseDataRows(Sheet sheet, HeaderScan scan, Map<String, String> merged) {
        String sheetName = sheet.getSheetName();
        List<CloseoutRecord> records = new ArrayList<>();
        List<ImportIssue> issues = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();

        for (int r = scan.rowIndex + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            if (isRowEmpty(row, scan.colIndexByDb, merged)) continue;

            RowParse parse = parseRow(sheetName, r, row, scan.colIndexByDb, merged);
            if (parse.issues.isEmpty()) {
                records.add(parse.record);
            } else {
                issues.addAll(parse.issues);
            }
        }

        return Result.data(sheetName, records, issues);
    }

    private boolean isRowEmpty(Row row, Map<String, Integer> cols, Map<String, String> merged) {
        for (int idx : cols.values()) {
            String v = cellText(row, idx, merged);
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }

    private RowParse parseRow(String sheetName, int rowIdx, Row row,
                              Map<String, Integer> cols, Map<String, String> merged) {
        List<ImportIssue> issues = new ArrayList<>();
        int excelRow = rowIdx + 1;

        String sku = readRequired(sheetName, excelRow, row, cols, ColumnMapping.COL_SKU, "SKU", merged, issues);
        String brand = readRequired(sheetName, excelRow, row, cols, ColumnMapping.COL_BRAND, "Brand", merged, issues);
        String closeoutId = readRequired(sheetName, excelRow, row, cols, ColumnMapping.COL_CLOSEOUT_ID, "CLOSEOUT_ID", merged, issues);
        String closeoutType = readRequired(sheetName, excelRow, row, cols, ColumnMapping.COL_CLOSEOUT_TYPE, "CLOSEOUT_TYPE", merged, issues);
        String status = readRequired(sheetName, excelRow, row, cols, ColumnMapping.COL_STATUS, "Active Inactive Status-Default", merged, issues);

        Integer linqId = null;
        String rawLinq = readRaw(row, cols.get(ColumnMapping.COL_LINQ_ID), merged);
        if (rawLinq == null || rawLinq.isBlank()) {
            issues.add(ImportIssue.cellLevel(sheetName, excelRow, "Product Linq ID",
                    "Missing required value."));
        } else {
            String cleaned = rawLinq.replace(",", "").trim();
            try {
                linqId = Integer.parseInt(cleaned);
            } catch (NumberFormatException ex) {
                issues.add(ImportIssue.cellLevel(sheetName, excelRow, "Product Linq ID",
                        "Value '" + rawLinq + "' is not a valid integer."));
            }
        }

        if (!issues.isEmpty()) return new RowParse(null, issues);
        return new RowParse(
                new CloseoutRecord(sku, closeoutId, linqId, status, closeoutType, brand),
                Collections.emptyList());
    }

    private String readRequired(String sheetName, int excelRow, Row row,
                                Map<String, Integer> cols, String dbCol, String headerName,
                                Map<String, String> merged, List<ImportIssue> issues) {
        Integer idx = cols.get(dbCol);
        String value = readRaw(row, idx, merged);
        if (value == null || value.isBlank()) {
            issues.add(ImportIssue.cellLevel(sheetName, excelRow, headerName,
                    "Missing required value."));
            return null;
        }
        return value.trim();
    }

    private String readRaw(Row row, Integer colIdx, Map<String, String> merged) {
        if (colIdx == null) return null;
        return cellText(row, colIdx, merged);
    }



    public record Result(
            String sheet,
            List<CloseoutRecord> records,
            List<ImportIssue> issues,
            String skipReason,
            boolean skipped
    ) {
        static Result data(String sheet, List<CloseoutRecord> records, List<ImportIssue> issues) {
            return new Result(sheet, records, issues, null, false);
        }
        static Result skipped(String sheet, String reason) {
            return new Result(sheet, List.of(), List.of(), reason, true);
        }
        static Result headerError(String sheet, String message) {
            return new Result(sheet, List.of(), List.of(ImportIssue.sheetLevel(sheet, message)), null, false);
        }
    }

    private record HeaderScan(int rowIndex, Map<String, Integer> colIndexByDb, List<String> missing) {
        int matches() { return colIndexByDb.size(); }
    }

    private record RowParse(CloseoutRecord record, List<ImportIssue> issues) {}
}
