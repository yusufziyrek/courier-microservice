package com.yusufziyrek.order.application.service;

import com.yusufziyrek.order.application.dto.CreateOrderCommand;
import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.domain.Order;
import com.yusufziyrek.order.domain.OrderItem;
import com.yusufziyrek.order.domain.OrderStatus;
import com.yusufziyrek.order.domain.event.OrderEventPublisher;
import com.yusufziyrek.order.domain.exception.InvalidOrderStateException;
import com.yusufziyrek.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResult createOrder(CreateOrderCommand command) {
        // Domain Fabrika Metodu İle Başla
        Order order = Order.create(command.userId());
        
        command.items().forEach(itemCmd -> {
            OrderItem item = new OrderItem(itemCmd.productId(), itemCmd.quantity(), itemCmd.unitPrice());
            order.addItem(item);
        });

        // Veritabanına kaydet
        Order savedOrder = orderRepository.save(order);
        
        // Sipariş Oluşturuldu Olayı Fırlat (Infrastructure katmanı bunu db başarılı olursa algılayacak)
        orderEventPublisher.publishOrderPlacedEvent(savedOrder);
        
        // Mimaride Domain Nesnesini dışarı sızdırma, Result (DTO) dön!
        return mapToResult(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResult getOrder(UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sipariş Bulunamadı: " + id)); // Özelleştirilmiş bir Not Found DTO da eklenebilir.
        return mapToResult(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResult> getUserOrders(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(this::mapToResult)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResult changeOrderStatus(UUID id, String newStatusStr) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sipariş Bulunamadı: " + id));
            
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStateException("Geçersiz sipariş durumu (status): " + newStatusStr);
        }

        switch (targetStatus) {
            case CONFIRMED -> order.confirm();
            case PREPARING -> order.prepare();
            case ON_THE_WAY -> order.ship();
            case DELIVERED -> {
                order.deliver();
                orderEventPublisher.publishOrderDeliveredEvent(order);
            }
            case CANCELLED -> {
                order.cancel();
                orderEventPublisher.publishOrderCancelledEvent(order);
            }
            default -> throw new InvalidOrderStateException("Tanımsız durum geçişi: " + targetStatus);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResult(savedOrder);
    }

    // Saf Java ile Local Entity To DTO (İleride karmaşıklaşırsa bu da dışarıya MapStruct gibi çıkartılabilir)
    private OrderResult mapToResult(Order order) {
        List<OrderResult.OrderItemResult> items = order.getItems().stream()
                .map(i -> new OrderResult.OrderItemResult(
                        i.getId(), i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());

        return new OrderResult(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
