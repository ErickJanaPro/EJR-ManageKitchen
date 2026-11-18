package com.ejr.managekitchen.common.exception;

/** Règle métier violée (transition interdite, stock insuffisant, doublon...) : renvoie HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
