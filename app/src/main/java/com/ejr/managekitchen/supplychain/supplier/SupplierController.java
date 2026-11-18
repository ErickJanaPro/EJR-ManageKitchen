package com.ejr.managekitchen.supplychain.supplier;

import com.ejr.managekitchen.supplychain.supplier.dto.IngredientRequest;
import com.ejr.managekitchen.supplychain.supplier.dto.IngredientResponse;
import com.ejr.managekitchen.supplychain.supplier.dto.SupplierRequest;
import com.ejr.managekitchen.supplychain.supplier.dto.SupplierResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return service.findAll().stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return SupplierResponse.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/ingredients")
    public List<IngredientResponse> ingredients(@PathVariable Long id) {
        return service.ingredientsOf(id).stream().map(IngredientResponse::from).toList();
    }

    @PostMapping("/{id}/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse addIngredient(@PathVariable Long id, @Valid @RequestBody IngredientRequest request) {
        return IngredientResponse.from(service.addIngredient(id, request));
    }
}
