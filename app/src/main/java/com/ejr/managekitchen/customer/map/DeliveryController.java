package com.ejr.managekitchen.customer.map;

import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.customer.map.dto.CreateDeliveryRequest;
import com.ejr.managekitchen.customer.map.dto.DeliveryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/deliveries")
@Tag(name = "Livraisons & Carte", description = "F8 — géolocalisation et suivi de livraison")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enregistrer l'adresse et les coordonnées GPS d'une commande en livraison")
    public DeliveryResponse register(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody CreateDeliveryRequest request) {
        return deliveryService.registerDelivery(user, request);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Suivre la livraison d'une commande sur la carte")
    public DeliveryResponse track(@PathVariable("orderId") Long orderId,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return deliveryService.findByOrderId(orderId, user);
    }
}