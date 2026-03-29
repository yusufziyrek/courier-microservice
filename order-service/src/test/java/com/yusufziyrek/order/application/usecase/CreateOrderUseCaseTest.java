package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.CreateOrderCommand;
import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateOrderUseCaseTest {

    @Test
    void shouldCreateOrderAndPublishPlacedEvent() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        SpyOrderEventPublisher eventPublisher = new SpyOrderEventPublisher();
        CreateOrderUseCase useCase = new CreateOrderUseCase(orderRepository, eventPublisher);

        UUID userId = UUID.randomUUID();
        List<CreateOrderCommand.OrderItemCommand> items = List.of(
                new CreateOrderCommand.OrderItemCommand(UUID.randomUUID(), 2, new BigDecimal("15.50")),
                new CreateOrderCommand.OrderItemCommand(UUID.randomUUID(), 1, new BigDecimal("9.90"))
        );

        OrderResult result = useCase.execute(new CreateOrderCommand(userId, items));

        assertNotNull(result.id());
        assertEquals(userId, result.userId());
        assertEquals(2, result.items().size());
        assertEquals(1, eventPublisher.placedEventCount);
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public Order save(Order order) {
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
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
        private int placedEventCount;

        @Override
        public void publishOrderPlacedEvent(Order order) {
            placedEventCount++;
        }

        @Override
        public void publishOrderDeliveredEvent(Order order) {
        }

        @Override
        public void publishOrderCancelledEvent(Order order) {
        }
    }
}
