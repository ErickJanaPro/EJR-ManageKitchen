package com.ejr.managekitchen.operations.order.kitchen;

import com.ejr.managekitchen.operations.order.OrderService;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de l'écran de cuisine (KDS). Accès CUISINIER / ADMIN (voir SecurityConfig).
 * L'état initial est chargé ici, puis le KDS reçoit les mises à jour sur /topic/kitchen/orders.
 */
@RestController
@RequestMapping("/api/kitchen")
@Tag(name = "Cuisine (KDS)")
public class KitchenController {

    private final OrderService orderService;

    public KitchenController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    @Operation(summary = "Commandes à afficher en cuisine (NOUVELLE, EN_PREPARATION, PRETE)")
    public List<OrderResponse> ordersToPrepare() {
        return orderService.findForKitchen();
    }
}
