package com.adu.importer.model;


public record ImportIssue(
        String sheet,
        Integer row,
        String column,
        String message
) {
    public static ImportIssue sheetLevel(String sheet, String message) {
        return new ImportIssue(sheet, null, null, message);
    }

    public static ImportIssue cellLevel(String sheet, int row, String column, String message) {
        return new ImportIssue(sheet, row, column, message);
    }
}
