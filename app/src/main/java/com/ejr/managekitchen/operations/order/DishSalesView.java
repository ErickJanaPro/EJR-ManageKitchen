package com.ejr.managekitchen.operations.order;

/** Plat et quantité totale vendue (plats populaires, mode dégradé de l'IA). */
public interface DishSalesView {

    Long getDishId();

    String getDishName();

    Long getTotalQuantity();
}
