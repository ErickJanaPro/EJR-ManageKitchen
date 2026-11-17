package com.restaurant.supplychain.repository;

import com.restaurant.supplychain.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findBySupplierId(Long supplierId);
}
