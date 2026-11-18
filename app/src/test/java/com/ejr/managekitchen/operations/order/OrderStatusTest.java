package com.ejr.managekitchen.operations.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests unitaires de la machine à états (sans Spring). */
class OrderStatusTest {

    @Test
    void parcoursCompletRetrait() {
        Order order = new Order(1L, FulfillmentType.RETRAIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NOUVELLE);

        assertThat(order.changeStatus(OrderStatus.EN_PREPARATION)).isEqualTo(OrderStatus.NOUVELLE);
        assertThat(order.changeStatus(OrderStatus.PRETE)).isEqualTo(OrderStatus.EN_PREPARATION);
        order.changeStatus(OrderStatus.RECUPEREE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECUPEREE);
        assertThat(order.getStatus().isFinal()).isTrue();
    }

    @Test
    void parcoursCompletLivraison() {
        Order order = new Order(1L, FulfillmentType.LIVRAISON);
        order.changeStatus(OrderStatus.EN_PREPARATION);
        order.changeStatus(OrderStatus.PRETE);
        order.changeStatus(OrderStatus.LIVREE);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.LIVREE);
    }

    @Test
    void sauterUneEtapeEstInterdit() {
        Order order = new Order(1L, FulfillmentType.LIVRAISON);
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.PRETE))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NOUVELLE);
    }

    @Test
    void leStatutFinalDependDuModeDeRemise() {
        assertThat(OrderStatus.PRETE.canTransitionTo(OrderStatus.LIVREE, FulfillmentType.LIVRAISON)).isTrue();
        assertThat(OrderStatus.PRETE.canTransitionTo(OrderStatus.RECUPEREE, FulfillmentType.LIVRAISON)).isFalse();
        assertThat(OrderStatus.PRETE.canTransitionTo(OrderStatus.RECUPEREE, FulfillmentType.RETRAIT)).isTrue();
        assertThat(OrderStatus.PRETE.canTransitionTo(OrderStatus.LIVREE, FulfillmentType.RETRAIT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void aucunRetourEnArriereNiSortieDunStatutFinal(OrderStatus target) {
        for (FulfillmentType type : FulfillmentType.values()) {
            assertThat(OrderStatus.LIVREE.canTransitionTo(target, type)).isFalse();
            assertThat(OrderStatus.RECUPEREE.canTransitionTo(target, type)).isFalse();
            assertThat(target.canTransitionTo(OrderStatus.NOUVELLE, type)).isFalse();
        }
    }

    @Test
    void leTotalEstCalculeAPartirDesLignes() {
        Order order = new Order(1L, FulfillmentType.RETRAIT);
        order.addItem(new OrderItem(1L, "Burger", 2, new BigDecimal("12.50")));
        order.addItem(new OrderItem(3L, "Tiramisu", 1, new BigDecimal("6.50")));
        assertThat(order.getTotal()).isEqualByComparingTo("31.50");
    }
}
