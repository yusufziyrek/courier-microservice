package com.yusufziyrek.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;

    // Yeni kayıt için
    public OrderItem(UUID productId, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Veritabanından (Infrastructure) geri yüklemek (reconstruct) için
    public OrderItem(UUID id, UUID productId, Integer quantity, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
