package com.ejr.managekitchen.operations.order;

import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.operations.order.dto.ChangeStatusRequest;
import com.ejr.managekitchen.operations.order.dto.CreateOrderRequest;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Commandes", description = "F6 — commandes et machine à états")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Passer une commande (client) — décrémente le stock et notifie la cuisine")
    public OrderResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(user, request);
    }

    @GetMapping("/me")
    @Operation(summary = "Mes commandes (client)")
    public List<OrderResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.findMine(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande (un client ne voit que les siennes)")
    public OrderResponse findById(@PathVariable("id") Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.findById(id, user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUISINIER', 'ADMIN')")
    @Operation(summary = "Changer le statut (cuisine) — 409 si la transition est interdite")
    public OrderResponse changeStatus(@PathVariable("id") Long id,
                                      @Valid @RequestBody ChangeStatusRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.changeStatus(id, request.status(), user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toutes les commandes, filtrables par statut (admin)")
    public List<OrderResponse> findAll(@RequestParam(name = "status", required = false) OrderStatus status) {
        return orderService.findAll(status);
    }
}
