package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import com.example.events.OrderCreatedEvent;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderEventConsumer {

    private static final int MAX_RETRIES = 3;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public OrderEventConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE, ackMode = "MANUAL")
    public void handleOrderCreatedEvent(OrderCreatedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            System.out.println("[NOTIFICATION SERVICE] Consuming order event: " + event.getOrderId());

            // Simulate occasional processing failure (e.g. if customerId starts with 'fail-')
            if (event.getCustomerId() != null && event.getCustomerId().startsWith("fail-")) {
                throw new RuntimeException("Simulated notification gateway timeout error");
            }

            System.out.println("[NOTIFICATION SERVICE] Notification successfully sent for order: " + event.getOrderId());

            // Acknowledge receipt
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            System.err.println("[NOTIFICATION SERVICE] Failed to process event: " + event.getOrderId() + " - Error: " + e.getMessage());

            // Get current retry count
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) {
                retryCount = 0;
            }

            if (retryCount < MAX_RETRIES) {
                retryCount++;
                System.out.println("[NOTIFICATION SERVICE] Retrying order: " + event.getOrderId() + " (Attempt " + retryCount + " of " + MAX_RETRIES + ")");

                // Acknowledge the current failed message to prevent duplicates on broker
                channel.basicAck(deliveryTag, false);

                // Re-publish the message to the same queue with incremented header
                final Integer finalRetryCount = retryCount;
                rabbitTemplate.convertAndSend("", RabbitMQConfig.ORDER_QUEUE, event, postProcessor -> {
                    postProcessor.getMessageProperties().getHeaders().put("x-retry-count", finalRetryCount);
                    return postProcessor;
                });
            } else {
                System.err.println("[NOTIFICATION SERVICE] Max retries (" + MAX_RETRIES + ") exceeded for order: " + event.getOrderId() + ". Routing to DLQ...");

                // Reject without requeue -> routes directly to Dead-Letter Queue (DLQ)
                channel.basicReject(deliveryTag, false);
            }
        }
    }
}
