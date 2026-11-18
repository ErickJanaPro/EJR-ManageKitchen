package com.ejr.managekitchen.supplychain.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findBySupplierId(Long supplierId);
}
