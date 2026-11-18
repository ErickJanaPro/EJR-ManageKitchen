package com.ejr.managekitchen.operations.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Commande client. Nom d'entité JPQL « CustomerOrder » car ORDER est un mot réservé (SQL et JPQL).
 * Le champ {@code version} active le verrouillage optimiste : si deux cuisiniers cliquent en même temps,
 * le second reçoit une erreur 409 au lieu d'écraser le premier.
 */
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private FulfillmentType fulfillmentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // JPA
    }

    public Order(Long clientId, FulfillmentType fulfillmentType) {
        this.clientId = clientId;
        this.fulfillmentType = fulfillmentType;
        this.status = OrderStatus.NOUVELLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
        total = total.add(item.lineTotal());
    }

    /**
     * Applique une transition de la machine à états.
     * @return le statut précédent
     * @throws InvalidStatusTransitionException si la transition n'est pas autorisée
     */
    public OrderStatus changeStatus(OrderStatus target) {
        if (!status.canTransitionTo(target, fulfillmentType)) {
            throw new InvalidStatusTransitionException(status, target, fulfillmentType);
        }
        OrderStatus previous = status;
        status = target;
        updatedAt = LocalDateTime.now();
        return previous;
    }

    public boolean belongsTo(Long userId) {
        return clientId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public OrderStatus getStatus() { return status; }
    public FulfillmentType getFulfillmentType() { return fulfillmentType; }
    public BigDecimal getTotal() { return total; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
