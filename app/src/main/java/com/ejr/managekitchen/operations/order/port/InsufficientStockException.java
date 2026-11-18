package com.ejr.managekitchen.operations.order.port;

import com.ejr.managekitchen.common.exception.ConflictException;

/** Levée par l'implémentation de {@link StockPort} : renvoie HTTP 409 et annule la commande. */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
