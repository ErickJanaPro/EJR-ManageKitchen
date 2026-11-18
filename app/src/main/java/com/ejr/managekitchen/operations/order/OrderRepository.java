package com.ejr.managekitchen.operations.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Order> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @EntityGraph(attributePaths = "items")
    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("""
            select coalesce(sum(o.total), 0) from CustomerOrder o
            where o.status in :statuses and o.createdAt >= :from and o.createdAt < :to
            """)
    BigDecimal sumTotal(@Param("statuses") Collection<OrderStatus> statuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);
}
