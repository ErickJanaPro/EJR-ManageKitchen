package com.ejr.managekitchen.operations.order;

import com.ejr.managekitchen.common.exception.ConflictException;
import com.ejr.managekitchen.common.exception.NotFoundException;
import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.order.dto.CreateOrderRequest;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;
import com.ejr.managekitchen.operations.order.port.DishSnapshot;
import com.ejr.managekitchen.operations.order.port.MenuCatalogPort;
import com.ejr.managekitchen.operations.order.port.OrderLine;
import com.ejr.managekitchen.operations.order.port.StockPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Cycle de vie des commandes (F6). */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final MenuCatalogPort menuCatalog;
    private final StockPort stock;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository historyRepository,
                        MenuCatalogPort menuCatalog, StockPort stock, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.menuCatalog = menuCatalog;
        this.stock = stock;
        this.events = events;
    }

    /**
     * Étapes 2 et 3 du scénario nominal, dans UNE transaction :
     * vérification des plats → décrémentation du stock (synchrone) → enregistrement.
     * Les messages temps réel ne partent qu'après la validation de la transaction (voir OrderRealtimePublisher).
     */
    public OrderResponse create(AuthenticatedUser client, CreateOrderRequest request) {
        Order order = new Order(client.id(), request.fulfillmentType());
        for (CreateOrderRequest.Line line : request.items()) {
            DishSnapshot dish = menuCatalog.findDish(line.dishId())
                    .orElseThrow(() -> new NotFoundException("Plat introuvable : " + line.dishId()));
            if (!dish.available()) {
                throw new ConflictException("Plat indisponible : " + dish.name());
            }
            order.addItem(new OrderItem(dish.id(), dish.name(), line.quantity(), dish.price()));
        }

        List<OrderLine> lines = request.items().stream()
                .map(l -> new OrderLine(l.dishId(), l.quantity()))
                .toList();
        stock.consumeIngredientsFor(lines); // lève InsufficientStockException => rollback complet

        orderRepository.save(order);
        historyRepository.save(new OrderStatusHistory(order.getId(), null, order.getStatus(), client.id()));

        OrderResponse response = OrderResponse.from(order);
        events.publishEvent(new OrderStatusChangedEvent(response, null, client.id(), LocalDateTime.now()));
        return response;
    }

    /** Transition de statut par la cuisine (ou l'admin). */
    public OrderResponse changeStatus(Long orderId, OrderStatus target, AuthenticatedUser actor) {
        if (!(actor.hasRole(Role.CUISINIER) || actor.hasRole(Role.ADMIN))) {
            throw new AccessDeniedException("Réservé à la cuisine");
        }
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + orderId));
        OrderStatus previous = order.changeStatus(target);
        historyRepository.save(new OrderStatusHistory(order.getId(), previous, target, actor.id()));

        OrderResponse response = OrderResponse.from(order);
        events.publishEvent(new OrderStatusChangedEvent(response, previous, actor.id(), LocalDateTime.now()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(AuthenticatedUser client) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(client.id()).stream()
                .map(OrderResponse::from).toList();
    }

    /** Un client ne voit que ses commandes ; la cuisine et l'admin voient tout. */
    @Transactional(readOnly = true)
    public OrderResponse findById(Long orderId, AuthenticatedUser actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .filter(o -> !actor.hasRole(Role.CLIENT) || o.belongsTo(actor.id()))
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findForKitchen() {
        return orderRepository.findByStatusInOrderByCreatedAtAsc(OrderStatus.kitchenStatuses()).stream()
                .map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(OrderResponse::from).toList();
    }
}
