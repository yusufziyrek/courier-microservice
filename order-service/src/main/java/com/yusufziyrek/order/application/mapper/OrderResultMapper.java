package com.yusufziyrek.order.application.mapper;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.domain.Order;

import java.util.stream.Collectors;

// Tüm UseCase'lerin Domain nesnesini dışarı güvenle aktarabilmesi için ortak dönüştürücü
public class OrderResultMapper {
    
    public static OrderResult toResult(Order order) {
        var items = order.getItems().stream()
                .map(i -> new OrderResult.OrderItemResult(
                        i.getId(), i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());

        return new OrderResult(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
