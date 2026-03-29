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

class GetUserOrdersUseCaseTest {

    @Test
    void shouldReturnOnlyOrdersBelongingToUser() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        Order orderA1 = Order.create(userA);
        orderA1.setId(UUID.randomUUID());

        Order orderA2 = Order.create(userA);
        orderA2.setId(UUID.randomUUID());

        Order orderB = Order.create(userB);
        orderB.setId(UUID.randomUUID());

        repository.save(orderA1);
        repository.save(orderA2);
        repository.save(orderB);

        GetUserOrdersUseCase useCase = new GetUserOrdersUseCase(repository);
        List<OrderResult> userOrders = useCase.execute(userA);

        assertEquals(2, userOrders.size());
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
