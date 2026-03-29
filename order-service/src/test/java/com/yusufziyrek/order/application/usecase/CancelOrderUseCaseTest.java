package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderRepository;
import com.yusufziyrek.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderUseCaseTest {

    @Test
    void shouldCancelOrderAndPublishEvent() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        SpyOrderEventPublisher publisher = new SpyOrderEventPublisher();

        Order order = Order.create(UUID.randomUUID());
        order.setId(UUID.randomUUID());
        repository.save(order);

        CancelOrderUseCase useCase = new CancelOrderUseCase(repository, publisher);
        useCase.execute(order.getId());

        Order saved = repository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, saved.getStatus());
        assertEquals(1, publisher.cancelledEventCount);
    }

    @Test
    void shouldThrowWhenOrderMissing() {
        CancelOrderUseCase useCase = new CancelOrderUseCase(new InMemoryOrderRepository(), new SpyOrderEventPublisher());
        assertThrows(RuntimeException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public Order save(Order order) {
            orders.removeIf(existing -> existing.getId().equals(order.getId()));
            orders.add(order);
            return order;
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return orders.stream().filter(order -> order.getId().equals(id)).findFirst();
        }

        @Override
        public List<Order> findAllByUserId(UUID userId) {
            return orders.stream().filter(order -> order.getUserId().equals(userId)).toList();
        }
    }

    private static class SpyOrderEventPublisher implements OrderEventPublisher {
        private int cancelledEventCount;

        @Override
        public void publishOrderPlacedEvent(Order order) {
        }

        @Override
        public void publishOrderDeliveredEvent(Order order) {
        }

        @Override
        public void publishOrderCancelledEvent(Order order) {
            cancelledEventCount++;
        }
    }
}
