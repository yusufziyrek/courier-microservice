package com.yusufziyrek.order.presentation.api;

import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.usecase.CancelOrderUseCase;
import com.yusufziyrek.order.application.usecase.ChangeOrderStatusUseCase;
import com.yusufziyrek.order.application.usecase.CreateOrderUseCase;
import com.yusufziyrek.order.application.usecase.GetOrderUseCase;
import com.yusufziyrek.order.presentation.api.dto.ChangeOrderStatusRequest;
import com.yusufziyrek.order.presentation.api.dto.CreateOrderRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderHttpMapper orderHttpMapper;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            ChangeOrderStatusUseCase changeOrderStatusUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            OrderHttpMapper orderHttpMapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.changeOrderStatusUseCase = changeOrderStatusUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.orderHttpMapper = orderHttpMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResult> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UUID userId = extractUserId(httpServletRequest);
        OrderResult created = createOrderUseCase.execute(orderHttpMapper.toCreateCommand(request, userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResult> getOrder(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        UUID requesterId = extractUserId(httpServletRequest);
        OrderResult result = getOrderUseCase.execute(id);
        ensureOwner(requesterId, result.userId());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResult> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeOrderStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UUID requesterId = extractUserId(httpServletRequest);
        OrderResult current = getOrderUseCase.execute(id);
        ensureOwner(requesterId, current.userId());

        OrderResult updated = changeOrderStatusUseCase.execute(id, request.status());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        UUID requesterId = extractUserId(httpServletRequest);
        OrderResult current = getOrderUseCase.execute(id);
        ensureOwner(requesterId, current.userId());

        cancelOrderUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(HttpServletRequest request) {
        Object rawUserId = request.getAttribute("user_id");
        if (rawUserId == null) {
            throw new SecurityException("Unauthorized: user_id missing");
        }
        return UUID.fromString(rawUserId.toString());
    }

    private void ensureOwner(UUID requesterId, UUID resourceOwnerId) {
        if (!requesterId.equals(resourceOwnerId)) {
            throw new SecurityException("Forbidden: cannot access another user's order");
        }
    }
}
