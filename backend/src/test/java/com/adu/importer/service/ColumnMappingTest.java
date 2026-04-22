package com.adu.importer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnMappingTest {

    @Test
    void normalize_lowercasesTrimsAndCollapsesWhitespace() {
        assertAll(
                () -> assertEquals("sku", ColumnMapping.normalize("  SKU  ")),
                () -> assertEquals("active inactive status-default",
                        ColumnMapping.normalize("Active   Inactive\tStatus-Default")),
                () -> assertEquals("product linq id",
                        ColumnMapping.normalize("Product  Linq  ID"))
        );
    }

    @Test
    void normalize_returnsEmptyStringForNull() {
        assertEquals("", ColumnMapping.normalize(null));
    }

    @Test
    void headerToDb_containsAllExpectedHeaders() {
        assertAll(
                () -> assertEquals(ColumnMapping.COL_SKU, ColumnMapping.HEADER_TO_DB.get("SKU")),
                () -> assertEquals(ColumnMapping.COL_BRAND, ColumnMapping.HEADER_TO_DB.get("Brand")),
                () -> assertEquals(ColumnMapping.COL_LINQ_ID, ColumnMapping.HEADER_TO_DB.get("Product Linq ID")),
                () -> assertEquals(ColumnMapping.COL_CLOSEOUT_ID, ColumnMapping.HEADER_TO_DB.get("CLOSEOUT_ID")),
                () -> assertEquals(ColumnMapping.COL_CLOSEOUT_TYPE, ColumnMapping.HEADER_TO_DB.get("CLOSEOUT_TYPE")),
                () -> assertEquals(ColumnMapping.COL_STATUS,
                        ColumnMapping.HEADER_TO_DB.get("Active Inactive Status-Default"))
        );
    }

    @Test
    void normalizedHeaderToDb_resolvesCaseAndSpacingVariants() {
        String normalized = ColumnMapping.normalize("  product   linq id ");
        assertEquals(ColumnMapping.COL_LINQ_ID,
                ColumnMapping.NORMALIZED_HEADER_TO_DB.get(normalized));
    }

    @Test
    void requiredNormalizedHeaders_containsAllSixColumns() {
        assertEquals(6, ColumnMapping.requiredNormalizedHeaders().size());
        assertTrue(ColumnMapping.requiredNormalizedHeaders().contains("sku"));
    }
}
