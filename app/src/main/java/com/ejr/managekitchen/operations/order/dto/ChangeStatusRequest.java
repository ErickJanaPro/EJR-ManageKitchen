package com.ejr.managekitchen.operations.order.dto;

import com.ejr.managekitchen.operations.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {
}
