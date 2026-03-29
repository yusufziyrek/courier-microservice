package com.yusufziyrek.order.application.usecase;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderUseCaseTest {

    @Test
    void shouldReturnOrderById() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Order order = Order.create(UUID.randomUUID());
        order.setId(UUID.randomUUID());
        repository.save(order);

        GetOrderUseCase useCase = new GetOrderUseCase(repository);
        OrderResult result = useCase.execute(order.getId());

        assertEquals(order.getId(), result.id());
        assertEquals(order.getUserId(), result.userId());
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        GetOrderUseCase useCase = new GetOrderUseCase(new InMemoryOrderRepository());
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
}
