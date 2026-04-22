package com.adu.importer.web;

import com.adu.importer.model.ImportIssue;
import com.adu.importer.service.ExcelImportService;
import com.adu.importer.service.ExcelImportService.ImportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportControllerTest {

    private ExcelImportService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ExcelImportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ImportController(service)).build();
    }

    @Test
    void importFile_returns400_whenFileMissing() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/import").file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No file uploaded."));
    }

    @Test
    void importFile_returns400_whenExtensionNotXlsx() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "data.csv", "text/csv", "a,b,c".getBytes());

        mockMvc.perform(multipart("/api/import").file(csv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only .xlsx files are supported."));
    }

    @Test
    void importFile_returns200_andSummary_onSuccess() throws Exception {
        ImportSummary summary = new ImportSummary(
                3, List.of("Sheet1"), List.of(), List.of());
        when(service.importWorkbook(any())).thenReturn(summary);

        MockMultipartFile xlsx = new MockMultipartFile(
                "file", "good.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/import").file(xlsx))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(3))
                .andExpect(jsonPath("$.processedSheets[0]").value("Sheet1"));
    }

    @Test
    void importFile_returns207_whenSummaryHasErrors() throws Exception {
        ImportSummary summary = new ImportSummary(
                1, List.of("Sheet1"), List.of(),
                List.of(ImportIssue.cellLevel("Sheet1", 2, "SKU", "Missing required value.")));
        when(service.importWorkbook(any())).thenReturn(summary);

        MockMultipartFile xlsx = new MockMultipartFile(
                "file", "partial.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/import").file(xlsx))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.errors[0].column").value("SKU"));
    }

    @Test
    void importFile_returns400_whenServiceThrowsIOException() throws Exception {
        when(service.importWorkbook(any())).thenThrow(new IOException("corrupt zip"));

        MockMultipartFile xlsx = new MockMultipartFile(
                "file", "broken.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/import").file(xlsx))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        org.hamcrest.Matchers.containsString("corrupt zip")));
    }

    @Test
    void importFile_returns500_onUnexpectedException() throws Exception {
        when(service.importWorkbook(any())).thenThrow(new RuntimeException("boom"));

        MockMultipartFile xlsx = new MockMultipartFile(
                "file", "any.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/import").file(xlsx))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message",
                        org.hamcrest.Matchers.containsString("boom")));
    }
}
