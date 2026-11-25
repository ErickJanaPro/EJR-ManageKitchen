package com.ejr.managekitchen.supplychain.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ThresholdRequest(
        @NotNull @DecimalMin("0.0") BigDecimal alertThreshold
) {
}
