package com.ejr.managekitchen.operations.order.port;

/** Une ligne « plat × quantité » transmise à la supply chain pour la décrémentation du stock. */
public record OrderLine(Long dishId, int quantity) {
}
