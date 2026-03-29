package com.yusufziyrek.order.infrastructure.messaging.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDeliveredEvent(
        UUID orderId,
        UUID userId,
        OffsetDateTime occurredAt
) {
}
