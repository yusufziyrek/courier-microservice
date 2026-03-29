package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.mapper.OrderResultMapper;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderRepository;

import java.util.UUID;

public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResult execute(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş Bulunamadı: " + id));
        return OrderResultMapper.toResult(order);
    }
}
