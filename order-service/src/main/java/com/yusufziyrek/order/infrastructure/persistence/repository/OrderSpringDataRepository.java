package com.yusufziyrek.order.infrastructure.persistence.repository;

import com.yusufziyrek.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderSpringDataRepository extends JpaRepository<OrderJpaEntity, UUID> {
    List<OrderJpaEntity> findAllByUserId(UUID userId);
}
