package com.yusufziyrek.order.infrastructure.messaging;

import com.yusufziyrek.order.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqOrderEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitMqOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new RabbitMqOrderEventPublisher(rabbitTemplate);
    }

    @Test
    void shouldPublishPlacedEvent() {
        Order order = Order.create(UUID.randomUUID());
        order.setId(UUID.randomUUID());

        publisher.publishOrderPlacedEvent(order);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMqTopologyConfig.ORDERS_EXCHANGE),
            eq("order.placed"),
            any(Object.class)
        );
    }

    @Test
    void shouldPublishCancelledEvent() {
        Order order = Order.create(UUID.randomUUID());
        order.setId(UUID.randomUUID());

        publisher.publishOrderCancelledEvent(order);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMqTopologyConfig.ORDERS_EXCHANGE),
            eq("order.cancelled"),
            any(Object.class)
        );
    }
}
