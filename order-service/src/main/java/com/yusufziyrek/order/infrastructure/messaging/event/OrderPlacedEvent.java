package com.yusufziyrek.order.infrastructure.messaging.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        OffsetDateTime occurredAt
) {
}
