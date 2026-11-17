package com.restaurant.supplychain.web.dto;

import com.restaurant.supplychain.domain.Supplier;

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
