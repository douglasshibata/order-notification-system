package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import com.example.events.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderDlqConsumer {

    @RabbitListener(queues = RabbitMQConfig.ORDER_DLQ)
    public void handleDlqMessage(OrderCreatedEvent event) {
        System.err.println("[DLQ MONITOR] ALERT: Dead-Lettered order event received in DLQ: " + event.getOrderId());
        // In a real application, you would log to an audit DB, alert SREs, or trigger manual recovery processes.
    }
}
