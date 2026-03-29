package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderRepository;

import java.util.UUID;

public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public CancelOrderUseCase(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public void execute(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş Bulunamadı: " + id));

        order.cancel();
        orderRepository.save(order);

        orderEventPublisher.publishOrderCancelledEvent(order);
    }
}
