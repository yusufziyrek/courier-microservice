package com.yusufziyrek.order.infrastructure.persistence.mapper;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderItem;
import com.yusufziyrek.order.domain.OrderStatus;
import com.yusufziyrek.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceOrderMapperTest {

    private final PersistenceOrderMapper mapper = new PersistenceOrderMapper();

    @Test
    void shouldMapDomainToJpaAndBack() {
        Order domain = Order.create(UUID.randomUUID());
        domain.setId(UUID.randomUUID());
        domain.setStatus(OrderStatus.CONFIRMED);
        domain.setTotalAmount(new BigDecimal("21.50"));
        domain.setItems(List.of(new OrderItem(UUID.randomUUID(), 2, new BigDecimal("10.75"))));

        OrderJpaEntity entity = mapper.toJpaEntity(domain);
        Order mappedBack = mapper.toDomain(entity);

        assertEquals(domain.getId(), mappedBack.getId());
        assertEquals(domain.getUserId(), mappedBack.getUserId());
        assertEquals(domain.getStatus(), mappedBack.getStatus());
        assertEquals(1, mappedBack.getItems().size());
        assertEquals(domain.getItems().getFirst().getProductId(), mappedBack.getItems().getFirst().getProductId());
    }
}
