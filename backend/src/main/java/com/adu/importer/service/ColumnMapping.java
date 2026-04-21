package com.adu.importer.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ColumnMapping {

    public static final String COL_SKU = "avb_sku";
    public static final String COL_BRAND = "avb_brand";
    public static final String COL_LINQ_ID = "linq_id";
    public static final String COL_CLOSEOUT_ID = "closeout_id";
    public static final String COL_CLOSEOUT_TYPE = "closeout_type";
    public static final String COL_STATUS = "avb_status";

    public static final Map<String, String> HEADER_TO_DB;

    public static final Map<String, String> NORMALIZED_HEADER_TO_DB;

    static {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("SKU", COL_SKU);
        headers.put("Brand", COL_BRAND);
        headers.put("Product Linq ID", COL_LINQ_ID);
        headers.put("CLOSEOUT_ID", COL_CLOSEOUT_ID);
        headers.put("CLOSEOUT_TYPE", COL_CLOSEOUT_TYPE);
        headers.put("Active Inactive Status-Default", COL_STATUS);
        HEADER_TO_DB = Map.copyOf(headers);

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            normalized.put(normalize(e.getKey()), e.getValue());
        }
        NORMALIZED_HEADER_TO_DB = Map.copyOf(normalized);
    }

    public static Set<String> requiredNormalizedHeaders() {
        return NORMALIZED_HEADER_TO_DB.keySet();
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private ColumnMapping() {}
}
