package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.publisher.OrderEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderEventPublisher orderEventPublisher;

    @Autowired
    public OrderController(OrderEventPublisher orderEventPublisher) {
        this.orderEventPublisher = orderEventPublisher;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderEventPublisher.createOrder(request);
        return ResponseEntity.ok(order);
    }
}
