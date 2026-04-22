package com.adu.importer.service;

import com.adu.importer.model.CloseoutRecord;
import com.adu.importer.model.ImportIssue;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetParserTest {

    private static final String[] HEADERS = {
            "SKU", "Brand", "Product Linq ID",
            "CLOSEOUT_ID", "CLOSEOUT_TYPE", "Active Inactive Status-Default"
    };

    private void writeHeaderRow(Sheet sheet, int rowIdx, String[] headers) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private void writeDataRow(Sheet sheet, int rowIdx, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) continue;
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @Test
    void parse_returnsSkipped_whenSheetHasNoHeaderRow() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Empty");
            sheet.createRow(0).createCell(0).setCellValue("Just some random text");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertTrue(result.skipped());
            assertEquals("Empty", result.sheet());
            assertNotNull(result.skipReason());
            assertTrue(result.records().isEmpty());
        }
    }

    @Test
    void parse_returnsHeaderError_whenRequiredColumnMissing() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("PartialHeaders");
            // Missing "Active Inactive Status-Default"
            String[] partial = { "SKU", "Brand", "Product Linq ID", "CLOSEOUT_ID", "CLOSEOUT_TYPE" };
            writeHeaderRow(sheet, 0, partial);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "123", "C-1", "Type1");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertFalse(result.skipped());
            assertTrue(result.records().isEmpty());
            assertEquals(1, result.issues().size());
            ImportIssue issue = result.issues().get(0);
            assertTrue(issue.message().contains("Active Inactive Status-Default"),
                    "Expected message to mention missing column, got: " + issue.message());
        }
    }

    @Test
    void parse_extractsRecords_fromValidSheet() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Closeouts");
            writeHeaderRow(sheet, 0, HEADERS);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "100", "CO-1", "TypeA", "Active");
            writeDataRow(sheet, 2, "SKU-2", "BrandB", "200", "CO-2", "TypeB", "Inactive");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertFalse(result.skipped());
            assertTrue(result.issues().isEmpty(), "Expected no issues, got: " + result.issues());
            assertEquals(2, result.records().size());

            CloseoutRecord first = result.records().get(0);
            assertEquals("SKU-1", first.avbSku());
            assertEquals("BrandA", first.avbBrand());
            assertEquals(100, first.linqId());
            assertEquals("CO-1", first.closeoutId());
            assertEquals("TypeA", first.closeoutType());
            assertEquals("Active", first.avbStatus());
        }
    }

    @Test
    void parse_skipsBlankRows_inMiddleOfData() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Gappy");
            writeHeaderRow(sheet, 0, HEADERS);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "100", "CO-1", "TypeA", "Active");
            // row 2 intentionally blank — no createRow call
            writeDataRow(sheet, 3, "SKU-2", "BrandB", "200", "CO-2", "TypeB", "Inactive");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertEquals(2, result.records().size());
            assertTrue(result.issues().isEmpty());
        }
    }

    @Test
    void parse_emitsCellLevelIssue_whenRequiredValueMissing() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("MissingCells");
            writeHeaderRow(sheet, 0, HEADERS);
            // Brand and Status missing
            writeDataRow(sheet, 1, "SKU-1", null, "100", "CO-1", "TypeA", null);

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertEquals(0, result.records().size());
            assertEquals(2, result.issues().size());
            assertTrue(result.issues().stream().allMatch(i -> i.row() != null && i.row() == 2));
            assertTrue(result.issues().stream().anyMatch(i -> "Brand".equals(i.column())));
            assertTrue(result.issues().stream().anyMatch(
                    i -> "Active Inactive Status-Default".equals(i.column())));
        }
    }

    @Test
    void parse_emitsIssue_whenLinqIdNotInteger() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BadLinq");
            writeHeaderRow(sheet, 0, HEADERS);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "not-a-number", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertEquals(0, result.records().size());
            assertEquals(1, result.issues().size());
            ImportIssue issue = result.issues().get(0);
            assertEquals("Product Linq ID", issue.column());
            assertTrue(issue.message().contains("not-a-number"));
        }
    }

    @Test
    void parse_stripsCommas_inLinqIdValues() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("CommaLinq");
            writeHeaderRow(sheet, 0, HEADERS);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "1,234,567", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertTrue(result.issues().isEmpty());
            assertEquals(1, result.records().size());
            assertEquals(1234567, result.records().get(0).linqId());
        }
    }

    @Test
    void parse_resolvesHeader_caseInsensitively() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("WeirdCase");
            String[] mixedCase = {
                    "sku", "  brand ", "product linq id",
                    "closeout_id", "closeout_type", "active inactive status-default"
            };
            writeHeaderRow(sheet, 0, mixedCase);
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "1", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertTrue(result.issues().isEmpty(), "issues: " + result.issues());
            assertEquals(1, result.records().size());
        }
    }

    @Test
    void parse_findsHeaderRow_belowLeadingTitleRows() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Titled");
            sheet.createRow(0).createCell(0).setCellValue("Quarterly Closeout Report");
            sheet.createRow(1).createCell(0).setCellValue("Generated 2026-04-21");
            writeHeaderRow(sheet, 2, HEADERS);
            writeDataRow(sheet, 3, "SKU-1", "BrandA", "100", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertFalse(result.skipped());
            assertTrue(result.issues().isEmpty());
            assertEquals(1, result.records().size());
        }
    }

    @Test
    void parse_propagatesValueFromMergedHeaderRegion() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Merged");
            // Header in row 0; Brand spans columns 1-2 as a merged region.
            String[] headers = {
                    "SKU", "Brand", "", "Product Linq ID",
                    "CLOSEOUT_ID", "CLOSEOUT_TYPE", "Active Inactive Status-Default"
            };
            writeHeaderRow(sheet, 0, headers);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));
            writeDataRow(sheet, 1, "SKU-1", "BrandA", "x", "100", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertTrue(result.issues().isEmpty(), "issues: " + result.issues());
            assertEquals(1, result.records().size());
            assertEquals("BrandA", result.records().get(0).avbBrand());
        }
    }

    @Test
    void parse_reportsIssueRowsUsingExcelOneBasedNumbering() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("RowNumbers");
            writeHeaderRow(sheet, 0, HEADERS);
            writeDataRow(sheet, 1, null, "BrandA", "100", "CO-1", "TypeA", "Active");

            SheetParser.Result result = new SheetParser().parse(sheet);

            assertEquals(1, result.issues().size());
            assertEquals(Integer.valueOf(2), result.issues().get(0).row(),
                    "POI row index 1 should be reported as Excel row 2");
        }
    }
}
