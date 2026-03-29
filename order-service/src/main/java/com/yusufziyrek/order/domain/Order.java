package com.yusufziyrek.order.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {

    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    // DDD Fabrika (Factory) Metodu
    public static Order create(UUID userId) {
        Order order = new Order();
        order.userId = userId;
        order.status = OrderStatus.PENDING;
        order.totalAmount = BigDecimal.ZERO;
        order.createdAt = OffsetDateTime.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        this.totalAmount = this.totalAmount.add(
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        this.updatedAt = OffsetDateTime.now();
    }

    // --- STATE MACHINE ---

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Sipariş yalnızca PENDING durumundayken onaylanabilir. Geçerli durum: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void prepare() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Sipariş yalnızca CONFIRMED durumundayken yemeğe hazırlanabilir. Geçerli durum: " + this.status);
        }
        this.status = OrderStatus.PREPARING;
        this.updatedAt = OffsetDateTime.now();
    }

    public void ship() {
        if (this.status != OrderStatus.PREPARING) {
            throw new InvalidOrderStateException(
                    "Sipariş yalnızca PREPARING durumundayken kuryeye verilebilir. Geçerli durum: " + this.status);
        }
        this.status = OrderStatus.ON_THE_WAY;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deliver() {
        if (this.status != OrderStatus.ON_THE_WAY) {
            throw new InvalidOrderStateException(
                    "Sipariş yalnızca Kuryedeyken teslim edilebilir. Geçerli durum: " + this.status);
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Sipariş iptali yalnızca PENDING veya CONFIRMED durumlarında mümkündür. Geçerli durum: "
                            + this.status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters & (Gereken) Setters
    // Not: ID vb. alanlara setter ekliyoruz ki Infrastructure DB'den çekerken ID'yi
    // doldurabilsin.
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
