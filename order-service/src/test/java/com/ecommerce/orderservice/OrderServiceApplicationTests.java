package com.ecommerce.orderservice;

import com.ecommerce.orderservice.config.RabbitMQConfig;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.publisher.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.example.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:order_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class OrderServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
    }

    @Test
    public void contextLoads() {
        assertNotNull(orderRepository);
    }

    @Test
    public void testCreateOrderFlow() {
        CreateOrderRequest request = new CreateOrderRequest("prod-abc", 5, "cust-789");
        String url = "http://localhost:" + port + "/orders";

        Order response = restTemplate.postForObject(url, request, Order.class);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("prod-abc", response.getProductId());
        assertEquals(5, response.getQuantity());
        assertEquals("cust-789", response.getCustomerId());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        // Verify it was saved in Database
        Optional<Order> savedOrderOpt = orderRepository.findById(response.getId());
        assertTrue(savedOrderOpt.isPresent());
        Order savedOrder = savedOrderOpt.get();
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        // Verify RabbitMQ Event Publisher was invoked
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_ROUTING_KEY),
                eventCaptor.capture()
        );

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(savedOrder.getId().toString(), publishedEvent.getOrderId());
        assertEquals("prod-abc", publishedEvent.getProductId());
        assertEquals(5, publishedEvent.getQuantity());
        assertEquals("cust-789", publishedEvent.getCustomerId());
    }
}
