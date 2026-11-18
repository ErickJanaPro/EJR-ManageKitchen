package com.ejr.managekitchen.operations.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lectures mises à disposition des autres domaines (à injecter directement dans vos services).
 * <ul>
 *   <li>Dev 1 (F4 Finances) : {@link #revenueBetween}</li>
 *   <li>Dev 3 (F9 / mode dégradé de l'IA « Nos suggestions du chef ») : {@link #bestSellers}</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderQueryService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /** Chiffre d'affaires des commandes remises (LIVREE ou RECUPEREE) sur [from, to[. */
    public BigDecimal revenueBetween(LocalDateTime from, LocalDateTime to) {
        return orderRepository.sumTotal(OrderStatus.completedStatuses(), from, to);
    }

    /** Plats les plus commandés, du plus vendu au moins vendu. */
    public List<DishSalesView> bestSellers(int limit) {
        return orderItemRepository.findBestSellers(PageRequest.of(0, limit));
    }
}
