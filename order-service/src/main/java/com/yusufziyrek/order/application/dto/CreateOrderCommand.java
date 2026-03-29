package com.yusufziyrek.order.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Sistem dışından Application katmanına (OrderService'e) giren "emir" nesnesi.
public record CreateOrderCommand(
    UUID userId,
    List<OrderItemCommand> items
) {
    public record OrderItemCommand(
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
