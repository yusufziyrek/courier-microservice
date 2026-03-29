package com.yusufziyrek.order.infrastructure.persistence.mapper;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderItem;
import com.yusufziyrek.order.domain.OrderStatus;
import com.yusufziyrek.order.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.yusufziyrek.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PersistenceOrderMapper {

    public OrderJpaEntity toJpaEntity(Order domain) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setStatus(domain.getStatus().name());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        List<OrderItemJpaEntity> itemEntities = new ArrayList<>();
        for (OrderItem item : domain.getItems()) {
            OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
            itemEntity.setId(item.getId());
            itemEntity.setProductId(item.getProductId());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setUnitPrice(item.getUnitPrice());
            itemEntities.add(itemEntity);
        }

        entity.setItems(itemEntities);
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        Order domain = new Order();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setStatus(OrderStatus.valueOf(entity.getStatus()));
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemJpaEntity itemEntity : entity.getItems()) {
            items.add(new OrderItem(
                    itemEntity.getId(),
                    itemEntity.getProductId(),
                    itemEntity.getQuantity(),
                    itemEntity.getUnitPrice()
            ));
        }
        domain.setItems(items);
        return domain;
    }
}
