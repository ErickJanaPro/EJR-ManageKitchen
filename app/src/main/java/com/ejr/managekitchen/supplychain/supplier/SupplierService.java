package com.ejr.managekitchen.supplychain.supplier;

import com.ejr.managekitchen.supplychain.supplier.dto.IngredientRequest;
import com.ejr.managekitchen.supplychain.supplier.dto.SupplierRequest;
import com.ejr.managekitchen.supplychain.shared.DuplicateResourceException;
import com.ejr.managekitchen.supplychain.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository suppliers;
    private final IngredientRepository ingredients;

    public SupplierService(SupplierRepository suppliers, IngredientRepository ingredients) {
        this.suppliers = suppliers;
        this.ingredients = ingredients;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return suppliers.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + id));
    }

    public Supplier create(SupplierRequest r) {
        if (suppliers.existsByNameIgnoreCase(r.name())) {
            throw new DuplicateResourceException("Un fournisseur nommé '" + r.name() + "' existe déjà");
        }
        return suppliers.save(new Supplier(r.name(), r.email(), r.phone(), r.address()));
    }

    public Supplier update(Long id, SupplierRequest r) {
        Supplier s = get(id);
        s.update(r.name(), r.email(), r.phone(), r.address());
        return s;
    }

    public void delete(Long id) {
        if (!ingredients.findBySupplierId(id).isEmpty()) {
            throw new DuplicateResourceException(
                    "Suppression impossible : des ingrédients sont rattachés à ce fournisseur");
        }
        suppliers.delete(get(id));
    }

    @Transactional(readOnly = true)
    public List<Ingredient> ingredientsOf(Long supplierId) {
        get(supplierId);
        return ingredients.findBySupplierId(supplierId);
    }

    public Ingredient addIngredient(Long supplierId, IngredientRequest r) {
        Supplier s = get(supplierId);
        return ingredients.save(new Ingredient(r.name(), r.unit(), r.unitPrice(), s));
    }
}
