package com.ecommerce.notificationservice;

import com.ecommerce.notificationservice.config.RabbitMQConfig;
import com.ecommerce.notificationservice.consumer.OrderEventConsumer;
import com.example.events.OrderCreatedEvent;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceApplicationTests {

    @Autowired
    private OrderEventConsumer orderEventConsumer;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testSuccessfulNotificationConsumption() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent("o-1", "p-1", 1, "c-1");
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(100L);
        when(message.getMessageProperties()).thenReturn(properties);

        Channel channel = mock(Channel.class);

        orderEventConsumer.handleOrderCreatedEvent(event, message, channel);

        // Should call basicAck
        verify(channel, times(1)).basicAck(eq(100L), eq(false));
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    public void testFailedNotificationConsumptionTriggersRetryRepublish() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent("o-failed", "p-failed", 2, "fail-customer");
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(200L);
        // Initially no x-retry-count header
        properties.setHeaders(new HashMap<>());
        when(message.getMessageProperties()).thenReturn(properties);

        Channel channel = mock(Channel.class);

        orderEventConsumer.handleOrderCreatedEvent(event, message, channel);

        // Since it failed (customerId starts with 'fail-') and current retries < MAX_RETRIES (3),
        // it should call basicAck (to delete the current duplicate on broker) and then republish to RabbitMQ queue with retry count incremented to 1.
        verify(channel, times(1)).basicAck(eq(200L), eq(false));

        // Verify rabbitTemplate republish back to the same queue
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(""),
                eq(RabbitMQConfig.ORDER_QUEUE),
                eq(event),
                any(org.springframework.amqp.core.MessagePostProcessor.class)
        );
    }

    @Test
    public void testFailedNotificationExceedsMaxRetriesDlqRouting() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent("o-dead", "p-dead", 3, "fail-customer");
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(300L);
        // Set retry count header to 3 (which equals MAX_RETRIES)
        properties.setHeader("x-retry-count", 3);
        when(message.getMessageProperties()).thenReturn(properties);

        Channel channel = mock(Channel.class);

        orderEventConsumer.handleOrderCreatedEvent(event, message, channel);

        // Should reject without requeue -> routes directly to Dead-Letter Queue (DLQ)
        verify(channel, times(1)).basicReject(eq(300L), eq(false));
        verify(channel, never()).basicAck(eq(300L), eq(false));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }
}
