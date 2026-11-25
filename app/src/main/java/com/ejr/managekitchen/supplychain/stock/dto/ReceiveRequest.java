package com.ejr.managekitchen.supplychain.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReceiveRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @Size(max = 255) String reason
) {
}
