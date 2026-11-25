package com.ejr.managekitchen.customer.menu.dto;

import java.math.BigDecimal;

public record MenuDTO(
        Long id,
        String nom,
        String description,
        BigDecimal prix,
        String imageUrl,
        boolean disponible
) {
}