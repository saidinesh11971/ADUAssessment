package com.adu.importer.service;

import com.adu.importer.model.CloseoutRecord;
import com.adu.importer.model.ImportIssue;
import com.adu.importer.repo.CloseoutRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final CloseoutRepository repository;

    public ExcelImportService(CloseoutRepository repository) {
        this.repository = repository;
    }

    public ImportSummary importWorkbook(InputStream xlsx) throws IOException {
        List<CloseoutRecord> allRecords = new ArrayList<>();
        List<ImportIssue> errors = new ArrayList<>();
        List<String> skippedSheets = new ArrayList<>();
        List<String> processedSheets = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(xlsx)) {
            SheetParser parser = new SheetParser();
            int numSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                SheetParser.Result result = parser.parse(sheet);

                if (result.skipped()) {
                    skippedSheets.add(result.sheet() + " — " + result.skipReason());
                    continue;
                }

                if (!result.issues().isEmpty()) {
                    errors.addAll(result.issues());
                }
                if (!result.records().isEmpty()) {
                    allRecords.addAll(result.records());
                    processedSheets.add(result.sheet());
                }
            }
        }

        int imported = 0;
        if (!allRecords.isEmpty()) {
            imported = repository.upsertAll(allRecords);
            log.info("Upserted {} records across {} sheets", imported, processedSheets.size());
        }

        return new ImportSummary(imported, processedSheets, skippedSheets, errors);
    }

    public record ImportSummary(
            int importedCount,
            List<String> processedSheets,
            List<String> skippedSheets,
            List<ImportIssue> errors
    ) {
        public boolean success() {
            return errors.isEmpty();
        }
    }
}
