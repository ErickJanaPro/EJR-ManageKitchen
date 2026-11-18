package com.ejr.managekitchen.operations.notification;

import com.ejr.managekitchen.common.exception.NotFoundException;
import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.notification.dto.NotificationResponse;
import com.ejr.managekitchen.operations.order.FulfillmentType;
import com.ejr.managekitchen.operations.order.OrderStatus;
import com.ejr.managekitchen.operations.order.OrderStatusChangedEvent;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;
import com.ejr.managekitchen.operations.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * F7 — Notifications.
 * <ul>
 *   <li>Nouvelle commande → une notification par cuisinier actif</li>
 *   <li>Commande PRETE → notification au client (WebSocket + e-mail)</li>
 * </ul>
 */
@Service
public class NotificationService {

    public static final String CLIENT_NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailSender emailSender;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate, EmailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.emailSender = emailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderChanged(OrderStatusChangedEvent event) {
        OrderResponse order = event.order();
        if (event.isCreation()) {
            notifyKitchen(order);
        } else if (order.status() == OrderStatus.PRETE) {
            notifyClientOrderReady(order);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(AuthenticatedUser user) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.id()).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, AuthenticatedUser user) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientId().equals(user.id()))
                .orElseThrow(() -> new NotFoundException("Notification introuvable : " + notificationId));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    private void notifyKitchen(OrderResponse order) {
        String message = "Nouvelle commande n°" + order.id() + " (" + order.items().size() + " ligne(s))";
        userRepository.findByRoleAndEnabledTrue(Role.CUISINIER).forEach(cook ->
                notificationRepository.save(
                        new Notification(cook.getId(), NotificationType.ORDER_RECEIVED, message, order.id())));
        // L'affichage instantané sur le KDS passe par /topic/kitchen/orders (OrderRealtimePublisher).
    }

    private void notifyClientOrderReady(OrderResponse order) {
        String message = order.fulfillmentType() == FulfillmentType.RETRAIT
                ? "Votre commande n°" + order.id() + " est prête : vous pouvez venir la récupérer."
                : "Votre commande n°" + order.id() + " est prête et part en livraison.";
        Notification notification = notificationRepository.save(
                new Notification(order.clientId(), NotificationType.ORDER_READY, message, order.id()));

        messagingTemplate.convertAndSendToUser(String.valueOf(order.clientId()), CLIENT_NOTIFICATIONS_QUEUE,
                NotificationResponse.from(notification));
        userRepository.findById(order.clientId())
                .ifPresent(client -> emailSender.send(client.getEmail(), "Votre commande est prête", message));
    }
}
