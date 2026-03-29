package com.yusufziyrek.order.application.dto;

import com.yusufziyrek.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Application servisinin dış Controller'lara okuması için verdiği "Saf Java Sonucu"
// Controller kesinlikle "Order.java" nesnesini görmez, sadece bunu görür.
public record OrderResult(
    UUID id,
    UUID userId,
    OrderStatus status,
    BigDecimal totalAmount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<OrderItemResult> items
) {
    public record OrderItemResult(
        UUID id,
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
