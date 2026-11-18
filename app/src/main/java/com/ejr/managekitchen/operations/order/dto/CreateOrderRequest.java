package com.ejr.managekitchen.operations.order.dto;

import com.ejr.managekitchen.operations.order.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Panier validé par le client. */
public record CreateOrderRequest(
        @NotNull FulfillmentType fulfillmentType,
        @NotEmpty @Valid List<Line> items) {

    public record Line(@NotNull Long dishId, @Min(1) @Max(50) int quantity) {
    }
}
