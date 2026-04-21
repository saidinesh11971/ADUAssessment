package com.adu.importer.model;

public record CloseoutRecord(
        String avbSku,
        String closeoutId,
        int linqId,
        String avbStatus,
        String closeoutType,
        String avbBrand
) {}
