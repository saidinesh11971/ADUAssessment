package com.adu.importer.service;

import com.adu.importer.model.CloseoutRecord;
import com.adu.importer.repo.CloseoutRepository;
import com.adu.importer.service.ExcelImportService.ImportSummary;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcelImportServiceTest {

    private static final String[] HEADERS = {
            "SKU", "Brand", "Product Linq ID",
            "CLOSEOUT_ID", "CLOSEOUT_TYPE", "Active Inactive Status-Default"
    };

    private InputStream buildWorkbook(WorkbookBuilder builder) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            builder.populate(wb);
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void writeRow(Sheet sheet, int rowIdx, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) continue;
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @Test
    void importWorkbook_persistsParsedRecords_andReturnsSummary() throws Exception {
        CloseoutRepository repo = mock(CloseoutRepository.class);
        when(repo.upsertAll(anyList())).thenReturn(2);
        ExcelImportService service = new ExcelImportService(repo);

        InputStream xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Closeouts");
            writeRow(sheet, 0, HEADERS);
            writeRow(sheet, 1, "SKU-1", "BrandA", "1", "CO-1", "TypeA", "Active");
            writeRow(sheet, 2, "SKU-2", "BrandB", "2", "CO-2", "TypeB", "Inactive");
        });

        ImportSummary summary = service.importWorkbook(xlsx);

        assertTrue(summary.success(), "errors: " + summary.errors());
        assertEquals(2, summary.importedCount());
        assertEquals(List.of("Closeouts"), summary.processedSheets());
        assertTrue(summary.skippedSheets().isEmpty());

        org.mockito.ArgumentCaptor<List<CloseoutRecord>> captor =
                org.mockito.ArgumentCaptor.captor();
        verify(repo).upsertAll(captor.capture());
        List<CloseoutRecord> persisted = captor.getValue();
        assertEquals(2, persisted.size());
        assertEquals("SKU-1", persisted.get(0).avbSku());
    }

    @Test
    void importWorkbook_recordsSkippedSheets_andStillImportsValidOnes() throws Exception {
        CloseoutRepository repo = mock(CloseoutRepository.class);
        when(repo.upsertAll(anyList())).thenReturn(1);
        ExcelImportService service = new ExcelImportService(repo);

        InputStream xlsx = buildWorkbook(wb -> {
            Sheet skip = wb.createSheet("Cover");
            skip.createRow(0).createCell(0).setCellValue("This sheet is intro text");

            Sheet good = wb.createSheet("Data");
            writeRow(good, 0, HEADERS);
            writeRow(good, 1, "SKU-1", "BrandA", "1", "CO-1", "TypeA", "Active");
        });

        ImportSummary summary = service.importWorkbook(xlsx);

        assertEquals(1, summary.importedCount());
        assertEquals(List.of("Data"), summary.processedSheets());
        assertEquals(1, summary.skippedSheets().size());
        assertTrue(summary.skippedSheets().get(0).startsWith("Cover"));
        assertTrue(summary.success());
    }

    @Test
    void importWorkbook_doesNotCallRepository_whenNoRecords() throws Exception {
        CloseoutRepository repo = mock(CloseoutRepository.class);
        ExcelImportService service = new ExcelImportService(repo);

        InputStream xlsx = buildWorkbook(wb -> {
            Sheet skip = wb.createSheet("OnlyText");
            skip.createRow(0).createCell(0).setCellValue("nothing here");
        });

        ImportSummary summary = service.importWorkbook(xlsx);

        assertEquals(0, summary.importedCount());
        assertTrue(summary.processedSheets().isEmpty());
        verify(repo, never()).upsertAll(anyList());
    }

    @Test
    void importWorkbook_aggregatesCellLevelErrors_acrossSheets() throws Exception {
        CloseoutRepository repo = mock(CloseoutRepository.class);
        when(repo.upsertAll(anyList())).thenReturn(1);
        ExcelImportService service = new ExcelImportService(repo);

        InputStream xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Mixed");
            writeRow(sheet, 0, HEADERS);
            // valid row
            writeRow(sheet, 1, "SKU-1", "BrandA", "1", "CO-1", "TypeA", "Active");
            // invalid linq id
            writeRow(sheet, 2, "SKU-2", "BrandB", "abc", "CO-2", "TypeB", "Inactive");
        });

        ImportSummary summary = service.importWorkbook(xlsx);

        assertFalse(summary.success());
        assertEquals(1, summary.errors().size());
        assertEquals("Product Linq ID", summary.errors().get(0).column());
        assertEquals(1, summary.importedCount());
    }

    @FunctionalInterface
    interface WorkbookBuilder {
        void populate(XSSFWorkbook wb) throws Exception;
    }
}
