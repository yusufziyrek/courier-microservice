package com.yusufziyrek.order.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {

    @Test
    void shouldMoveFromPendingToDeliveredWithValidTransitions() {
        Order order = Order.create(UUID.randomUUID());

        order.confirm();
        order.prepare();
        order.ship();
        order.deliver();

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void shouldRejectInvalidTransitionFromPendingToDelivered() {
        Order order = Order.create(UUID.randomUUID());

        assertThrows(InvalidOrderStateException.class, order::deliver);
    }

    @Test
    void shouldAllowCancelFromConfirmed() {
        Order order = Order.create(UUID.randomUUID());
        order.confirm();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
