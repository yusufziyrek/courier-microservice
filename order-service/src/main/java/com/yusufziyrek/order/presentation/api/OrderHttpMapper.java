package com.yusufziyrek.order.presentation.api;

import com.yusufziyrek.order.application.dto.CreateOrderCommand;
import com.yusufziyrek.order.presentation.api.dto.CreateOrderRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderHttpMapper {

    public CreateOrderCommand toCreateCommand(CreateOrderRequest request, UUID userId) {
        var items = request.items().stream()
                .map(item -> new CreateOrderCommand.OrderItemCommand(item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        return new CreateOrderCommand(userId, items);
    }
}
