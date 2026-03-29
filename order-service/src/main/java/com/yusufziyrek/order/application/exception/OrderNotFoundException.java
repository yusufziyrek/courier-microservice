package com.yusufziyrek.order.application.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Sipariş Bulunamadı: " + orderId);
    }
}
