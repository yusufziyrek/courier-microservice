package com.yusufziyrek.order.domain.event;

import com.yusufziyrek.order.domain.Order;

// Domain katmanının "bu olayları dünyaya duyur" dediği sözleşme noktası.
public interface OrderEventPublisher {
    void publishOrderPlacedEvent(Order order);
    void publishOrderDeliveredEvent(Order order);
    void publishOrderCancelledEvent(Order order);
}
