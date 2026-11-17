package com.restaurant.supplychain.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IngredientRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice
) {
}
