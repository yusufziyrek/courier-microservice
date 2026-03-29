package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.CreateOrderCommand;
import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.mapper.OrderResultMapper;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderItem;
import com.yusufziyrek.order.domain.OrderRepository;

// Tek sorumluluğu olan Saf Java Sınıfı! Bağımlılık (Spring vb.) yok.
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public CreateOrderUseCase(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResult execute(CreateOrderCommand command) {
        Order order = Order.create(command.userId());

        command.items().forEach(itemCmd -> {
            order.addItem(new OrderItem(itemCmd.productId(), itemCmd.quantity(), itemCmd.unitPrice()));
        });

        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderPlacedEvent(savedOrder);

        return OrderResultMapper.toResult(savedOrder);
    }
}
