package com.adu.importer.web;

import com.adu.importer.service.ExcelImportService;
import com.adu.importer.service.ExcelImportService.ImportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final ExcelImportService service;

    public ImportController(ExcelImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("No file uploaded.", List.of()));
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Only .xlsx files are supported.", List.of()));
        }

        try (InputStream in = file.getInputStream()) {
            ImportSummary summary = service.importWorkbook(in);
            HttpStatus status = summary.success() ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
            return ResponseEntity.status(status).body(summary);
        } catch (IOException ex) {
            log.error("Failed to read uploaded workbook", ex);
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Could not read the uploaded file: " + ex.getMessage(),
                            List.of()));
        } catch (Exception ex) {
            log.error("Import failed", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponse("Import failed: " + ex.getMessage(), List.of()));
        }
    }

    public record ErrorResponse(String message, List<String> details) {}
}
