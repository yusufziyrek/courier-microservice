package com.yusufziyrek.order.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqTopologyConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDER_PLACED_QUEUE = "orders.placed";
    public static final String ORDER_CANCELLED_QUEUE = "orders.cancelled";
    public static final String ORDER_DELIVERED_QUEUE = "orders.delivered";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return new Queue(ORDER_PLACED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue orderDeliveredQueue() {
        return new Queue(ORDER_DELIVERED_QUEUE, true);
    }

    @Bean
    public Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(ordersExchange).with("order.placed");
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(ordersExchange).with("order.cancelled");
    }

    @Bean
    public Binding orderDeliveredBinding(Queue orderDeliveredQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderDeliveredQueue).to(ordersExchange).with("order.delivered");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
