package com.ejr.managekitchen.supplychain.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Quantité réelle constatée après inventaire. */
public record AdjustRequest(
        @NotNull @DecimalMin("0.0") BigDecimal newQuantity,
        @Size(max = 255) String reason
) {
}
