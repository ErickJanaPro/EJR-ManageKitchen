package com.ejr.managekitchen.supplychain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Query("select s from StockItem s join fetch s.ingredient order by s.ingredient.name")
    List<StockItem> findAllWithIngredient();

    @Query("select s from StockItem s join fetch s.ingredient where s.ingredient.id = :ingredientId")
    Optional<StockItem> findByIngredientId(Long ingredientId);
}
