package com.ejr.managekitchen.customer.map;

import com.ejr.managekitchen.common.exception.ConflictException;
import com.ejr.managekitchen.common.exception.NotFoundException;
import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.customer.map.dto.CreateDeliveryRequest;
import com.ejr.managekitchen.customer.map.dto.DeliveryResponse;
import com.ejr.managekitchen.operations.order.FulfillmentType;
import com.ejr.managekitchen.operations.order.OrderStatus;
import com.ejr.managekitchen.operations.order.OrderStatusChangedEvent;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

/**
 * F8 — Géolocalisation et suivi cartographique des livraisons.
 * Réagit à {@link OrderStatusChangedEvent} sans dépendre du code interne de {@code operations}.
 */
@Service
@Transactional
public class DeliveryService {

    public static final double RESTAURANT_LAT = 48.8566;
    public static final double RESTAURANT_LNG = 2.3522;
    public static final String RESTAURANT_NAME = "EJR Kitchen — Paris Centre";
    public static final double MAX_RADIUS_KM = 15.0;

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public DeliveryResponse registerDelivery(AuthenticatedUser client, CreateDeliveryRequest request) {
        if (deliveryRepository.existsByOrderId(request.orderId())) {
            throw new ConflictException("Un suivi de livraison existe déjà pour la commande : " + request.orderId());
        }
        double distanceKm = roundOneDecimal(haversineKm(RESTAURANT_LAT, RESTAURANT_LNG,
                request.latitude(), request.longitude()));
        if (distanceKm > MAX_RADIUS_KM) {
            throw new ConflictException(String.format(Locale.US,
                    "Adresse hors zone de livraison (%.1f km > %.0f km maximum)", distanceKm, MAX_RADIUS_KM));
        }
        int estimatedMinutes = Math.max(15, (int) Math.round(10 + distanceKm * 3.5));
        Delivery delivery = deliveryRepository.save(new Delivery(
                request.orderId(), client.id(), request.address().trim(),
                request.latitude(), request.longitude(), distanceKm, estimatedMinutes));
        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse findByOrderId(Long orderId, AuthenticatedUser actor) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .filter(d -> !actor.hasRole(Role.CLIENT) || d.belongsTo(actor.id()))
                .orElseThrow(() -> new NotFoundException("Suivi de livraison introuvable pour la commande : " + orderId));
        return toResponse(delivery);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderChanged(OrderStatusChangedEvent event) {
        OrderResponse order = event.order();
        if (order.fulfillmentType() != FulfillmentType.LIVRAISON) {
            return;
        }
        if (order.status() == OrderStatus.PRETE) {
            Delivery delivery = deliveryRepository.findByOrderId(order.id())
                    .orElseGet(() -> new Delivery(order.id(), order.clientId(),
                            "Adresse de livraison enregistrée", 48.8620, 2.3480, 0.7, 15));
            delivery.start();
            deliveryRepository.save(delivery);
        } else if (order.status() == OrderStatus.LIVREE) {
            deliveryRepository.findByOrderId(order.id()).ifPresent(delivery -> {
                delivery.complete();
                deliveryRepository.save(delivery);
            });
        }
    }

    private DeliveryResponse toResponse(Delivery d) {
        return DeliveryResponse.from(d, RESTAURANT_LAT, RESTAURANT_LNG, RESTAURANT_NAME);
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}