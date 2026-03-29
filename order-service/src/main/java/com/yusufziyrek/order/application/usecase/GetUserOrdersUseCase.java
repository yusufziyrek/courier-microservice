package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.mapper.OrderResultMapper;
import com.yusufziyrek.order.domain.OrderRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetUserOrdersUseCase {

    private final OrderRepository orderRepository;

    public GetUserOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResult> execute(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(OrderResultMapper::toResult)
                .collect(Collectors.toList());
    }
}
