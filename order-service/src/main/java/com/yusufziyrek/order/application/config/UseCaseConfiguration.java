package com.yusufziyrek.order.application.config;

import com.yusufziyrek.order.application.usecase.CancelOrderUseCase;
import com.yusufziyrek.order.application.usecase.ChangeOrderStatusUseCase;
import com.yusufziyrek.order.application.usecase.CreateOrderUseCase;
import com.yusufziyrek.order.application.usecase.GetOrderUseCase;
import com.yusufziyrek.order.application.usecase.GetUserOrdersUseCase;
import com.yusufziyrek.order.domain.OrderEventPublisher;
import com.yusufziyrek.order.domain.OrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        return new CreateOrderUseCase(orderRepository, orderEventPublisher);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderUseCase(orderRepository);
    }

    @Bean
    public ChangeOrderStatusUseCase changeOrderStatusUseCase(
            OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        return new ChangeOrderStatusUseCase(orderRepository, orderEventPublisher);
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        return new CancelOrderUseCase(orderRepository, orderEventPublisher);
    }

    @Bean
    public GetUserOrdersUseCase getUserOrdersUseCase(OrderRepository orderRepository) {
        return new GetUserOrdersUseCase(orderRepository);
    }
}
