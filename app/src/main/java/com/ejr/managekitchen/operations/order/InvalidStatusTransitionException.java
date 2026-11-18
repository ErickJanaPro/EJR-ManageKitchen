package com.ejr.managekitchen.operations.order;

import com.ejr.managekitchen.common.exception.ConflictException;

public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to, FulfillmentType type) {
        super("Transition interdite : " + from + " → " + to + " (mode " + type + ")");
    }
}
