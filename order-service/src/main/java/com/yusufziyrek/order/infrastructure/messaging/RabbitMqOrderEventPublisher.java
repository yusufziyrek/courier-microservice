package com.yusufziyrek.order.infrastructure.messaging;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.infrastructure.messaging.event.OrderCancelledEvent;
import com.yusufziyrek.order.infrastructure.messaging.event.OrderDeliveredEvent;
import com.yusufziyrek.order.infrastructure.messaging.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqOrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqOrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishOrderPlacedEvent(Order order) {
        safePublish(
            "order.placed",
            new OrderPlacedEvent(order.getId(), order.getUserId(), order.getTotalAmount(), order.getUpdatedAt())
        );
    }

    @Override
    public void publishOrderDeliveredEvent(Order order) {
        safePublish(
            "order.delivered",
            new OrderDeliveredEvent(order.getId(), order.getUserId(), order.getUpdatedAt())
        );
    }

    @Override
    public void publishOrderCancelledEvent(Order order) {
        safePublish(
                "order.cancelled",
                new OrderCancelledEvent(order.getId(), order.getUserId(), order.getUpdatedAt())
        );
    }

    private void safePublish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqTopologyConfig.ORDERS_EXCHANGE, routingKey, payload);
        } catch (Exception ex) {
            // Portfolio simplicity: message bus errors should not break core order flow.
            log.warn("rabbit publish failed (routingKey={}): {}", routingKey, ex.getMessage());
        }
    }
}
