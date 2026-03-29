package com.yusufziyrek.order.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yusufziyrek.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Application servisinin dış Controller'lara okuması için verdiği "Saf Java Sonucu"
// Controller kesinlikle "Order.java" nesnesini görmez, sadece bunu görür.
public record OrderResult(
    UUID id,
    @JsonProperty("user_id") UUID userId,
    OrderStatus status,
    @JsonProperty("total_amount") BigDecimal totalAmount,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    @JsonProperty("updated_at") OffsetDateTime updatedAt,
    List<OrderItemResult> items
) {
    public record OrderItemResult(
        UUID id,
        @JsonProperty("product_id") UUID productId,
        Integer quantity,
        @JsonProperty("unit_price") BigDecimal unitPrice
    ) {}
}
