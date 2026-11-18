package com.ejr.managekitchen.supplychain.supplier.dto;

import com.ejr.managekitchen.supplychain.supplier.Supplier;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        LocalDateTime createdAt
) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getEmail(),
                s.getPhone(), s.getAddress(), s.getCreatedAt());
    }
}
