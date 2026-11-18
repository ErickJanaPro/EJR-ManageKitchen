package com.ejr.managekitchen.supplychain.shared;

/** Conflit métier (doublon, suppression impossible...) -> HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
