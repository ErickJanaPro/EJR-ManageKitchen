package com.ejr.managekitchen.supplychain.stock;

import com.ejr.managekitchen.supplychain.stock.dto.AdjustRequest;
import com.ejr.managekitchen.supplychain.stock.dto.ReceiveRequest;
import com.ejr.managekitchen.supplychain.stock.dto.StockItemResponse;
import com.ejr.managekitchen.supplychain.stock.dto.StockMovementResponse;
import com.ejr.managekitchen.supplychain.stock.dto.ThresholdRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lecture : ADMIN et CUISINIER. Modification : ADMIN uniquement. */
@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasRole('ADMIN')")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUISINIER')")
    public List<StockItemResponse> list() {
        return service.findAll().stream().map(StockItemResponse::from).toList();
    }

    /** Ingrédients dont la quantité est au niveau (ou sous le niveau) d'alerte. */
    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('ADMIN','CUISINIER')")
    public List<StockItemResponse> low() {
        return service.findLow().stream().map(StockItemResponse::from).toList();
    }

    @GetMapping("/{ingredientId}")
    @PreAuthorize("hasAnyRole('ADMIN','CUISINIER')")
    public StockItemResponse get(@PathVariable Long ingredientId) {
        return StockItemResponse.from(service.get(ingredientId));
    }

    @GetMapping("/{ingredientId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','CUISINIER')")
    public List<StockMovementResponse> movements(@PathVariable Long ingredientId) {
        return service.movementsOf(ingredientId).stream().map(StockMovementResponse::from).toList();
    }

    @PostMapping("/{ingredientId}/receive")
    @ResponseStatus(HttpStatus.CREATED)
    public StockItemResponse receive(@PathVariable Long ingredientId, @Valid @RequestBody ReceiveRequest r) {
        return StockItemResponse.from(service.receive(ingredientId, r.quantity(), r.reason()));
    }

    @PostMapping("/{ingredientId}/adjust")
    public StockItemResponse adjust(@PathVariable Long ingredientId, @Valid @RequestBody AdjustRequest r) {
        return StockItemResponse.from(service.adjust(ingredientId, r.newQuantity(), r.reason()));
    }

    @PutMapping("/{ingredientId}/threshold")
    public StockItemResponse threshold(@PathVariable Long ingredientId, @Valid @RequestBody ThresholdRequest r) {
        return StockItemResponse.from(service.updateThreshold(ingredientId, r.alertThreshold()));
    }
}
