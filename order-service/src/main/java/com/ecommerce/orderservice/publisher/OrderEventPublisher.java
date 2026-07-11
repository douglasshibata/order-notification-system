package com.ecommerce.orderservice.publisher;

import com.ecommerce.orderservice.config.RabbitMQConfig;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.example.events.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderEventPublisher(RabbitTemplate rabbitTemplate, OrderRepository orderRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setCustomerId(request.getCustomerId());
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
            savedOrder.getId().toString(),
            savedOrder.getProductId(),
            savedOrder.getQuantity(),
            savedOrder.getCustomerId()
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_ROUTING_KEY,
            event
        );

        return savedOrder;
    }
}
