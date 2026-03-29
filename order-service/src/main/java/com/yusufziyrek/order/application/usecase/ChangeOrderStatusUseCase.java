package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.exception.OrderNotFoundException;
import com.yusufziyrek.order.application.mapper.OrderResultMapper;
import com.yusufziyrek.order.domain.InvalidOrderStateException;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderRepository;
import com.yusufziyrek.order.domain.OrderStatus;

import java.util.Locale;
import java.util.UUID;

public class ChangeOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public ChangeOrderStatusUseCase(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResult execute(UUID id, String newStatusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(newStatusStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStateException("Geçersiz sipariş durumu (status): " + newStatusStr);
        }

        switch (targetStatus) {
            case CONFIRMED -> order.confirm();
            case PREPARING -> order.prepare();
            case ON_THE_WAY -> order.ship();
            case DELIVERED -> {
                order.deliver();
                orderEventPublisher.publishOrderDeliveredEvent(order);
            }
            case CANCELLED -> {
                order.cancel();
                orderEventPublisher.publishOrderCancelledEvent(order);
            }
            default -> throw new InvalidOrderStateException("Tanımsız durum geçişi: " + targetStatus);
        }

        Order savedOrder = orderRepository.save(order);
        return OrderResultMapper.toResult(savedOrder);
    }
}
