package com.ejr.managekitchen.supplychain.stock;

public enum MovementType {
    /** Entrée de marchandise (livraison fournisseur). */
    RECEPTION,
    /** Sortie liée à une commande (décrémentation automatique). */
    CONSOMMATION,
    /** Correction manuelle après inventaire. */
    AJUSTEMENT
}
