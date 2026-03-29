package com.yusufziyrek.order.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Domain, "Ben verilerimi bu kurallarla saklarım" der. Nasıl saklanacağı (JPA) altyapıya kalır.
public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findAllByUserId(UUID userId);
}
