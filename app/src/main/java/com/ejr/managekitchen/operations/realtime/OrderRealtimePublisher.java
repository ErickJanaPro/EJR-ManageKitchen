package com.ejr.managekitchen.operations.realtime;

import com.ejr.managekitchen.operations.order.OrderStatusChangedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Diffuse chaque création / changement de statut, UNIQUEMENT après la validation de la transaction :
 * la cuisine ne verra jamais une commande annulée faute de stock.
 */
@Component
public class OrderRealtimePublisher {

    public static final String KITCHEN_TOPIC = "/topic/kitchen/orders";
    public static final String CLIENT_ORDERS_QUEUE = "/queue/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public OrderRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderStatusChangedEvent event) {
        OrderRealtimeMessage message = OrderRealtimeMessage.from(event);
        messagingTemplate.convertAndSend(KITCHEN_TOPIC, message);
        messagingTemplate.convertAndSendToUser(String.valueOf(event.order().clientId()), CLIENT_ORDERS_QUEUE, message);
    }
}
