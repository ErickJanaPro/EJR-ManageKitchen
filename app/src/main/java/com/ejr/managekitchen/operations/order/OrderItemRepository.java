package com.ejr.managekitchen.operations.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select i.dishId as dishId, i.dishName as dishName, sum(i.quantity) as totalQuantity
            from OrderItem i
            group by i.dishId, i.dishName
            order by sum(i.quantity) desc
            """)
    List<DishSalesView> findBestSellers(Pageable pageable);
}
