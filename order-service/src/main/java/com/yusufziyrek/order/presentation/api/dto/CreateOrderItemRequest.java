package com.yusufziyrek.order.presentation.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {
}
