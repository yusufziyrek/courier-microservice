package com.yusufziyrek.order.infrastructure.persistence;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderRepository;
import com.yusufziyrek.order.infrastructure.persistence.mapper.PersistenceOrderMapper;
import com.yusufziyrek.order.infrastructure.persistence.repository.OrderSpringDataRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryJpaAdapter implements OrderRepository {

    private final OrderSpringDataRepository orderSpringDataRepository;
    private final PersistenceOrderMapper persistenceOrderMapper;

    public OrderRepositoryJpaAdapter(
            OrderSpringDataRepository orderSpringDataRepository,
            PersistenceOrderMapper persistenceOrderMapper
    ) {
        this.orderSpringDataRepository = orderSpringDataRepository;
        this.persistenceOrderMapper = persistenceOrderMapper;
    }

    @Override
    public Order save(Order order) {
        var saved = orderSpringDataRepository.save(persistenceOrderMapper.toJpaEntity(order));
        return persistenceOrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderSpringDataRepository.findById(id).map(persistenceOrderMapper::toDomain);
    }

    @Override
    public List<Order> findAllByUserId(UUID userId) {
        return orderSpringDataRepository.findAllByUserId(userId)
                .stream()
                .map(persistenceOrderMapper::toDomain)
                .toList();
    }
}
